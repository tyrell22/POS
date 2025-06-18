import React, { useState, useEffect } from 'react';
import { ShoppingCart, Plus, Minus, Trash2, Send, X, Settings, Home, Utensils, Coffee, Wine, Cake, Grid } from 'lucide-react';
import { menuAPI, orderAPI, adminAPI, floorPlanAPI } from '../services/api';
import FloorPlanManager from './FloorPlanManager';
import AdminProductForm from './AdminProductForm';

// Categories configuration
const categories = {
    'ХРАНА': { name: 'Храна', icon: Utensils, color: 'bg-green-500' },
    'ПИЈАЛОЦИ': { name: 'Пијалоци', icon: Coffee, color: 'bg-blue-500' },
    'АЛКОХОЛ': { name: 'Алкохол', icon: Wine, color: 'bg-purple-500' },
    'ДЕСЕРТИ': { name: 'Десерти', icon: Cake, color: 'bg-pink-500' }
};

const RestaurantPOS = () => {
    const [currentView, setCurrentView] = useState('tables'); // 'tables', 'order', 'admin', 'floorplan'
    const [selectedTable, setSelectedTable] = useState(null);
    const [selectedArea, setSelectedArea] = useState(null);
    const [floorPlan, setFloorPlan] = useState(null);
    const [areas, setAreas] = useState([]);
    const [orders, setOrders] = useState({});
    const [selectedCategory, setSelectedCategory] = useState('all');
    const [showAddItemModal, setShowAddItemModal] = useState(false);
    const [selectedMenuItem, setSelectedMenuItem] = useState(null);
    const [adminCode, setAdminCode] = useState('');
    const [isAdminAuthenticated, setIsAdminAuthenticated] = useState(false);
    const [showAdminLogin, setShowAdminLogin] = useState(false);
    const [menuItems, setMenuItems] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    // Load data on component mount AND when returning from admin
    useEffect(() => {
        loadMenuItems();
        loadActiveOrders();
        loadFloorPlan();
    }, []);

    // Reload floor plan when view changes back to tables
    useEffect(() => {
        if (currentView === 'tables') {
            console.log('View changed to tables, reloading floor plan...');
            loadFloorPlan();
        }
    }, [currentView]);

    const loadFloorPlan = async () => {
        try {
            setLoading(true);
            console.log('Loading floor plan...');

            const response = await floorPlanAPI.getActive();
            console.log('Loaded floor plan:', response.data);

            setFloorPlan(response.data);
            setAreas(response.data.areas || []);
            if (response.data.areas && response.data.areas.length > 0) {
                setSelectedArea(response.data.areas[0]);
            }
        } catch (err) {
            console.log('No active floor plan found:', err);
            // Try to create default floor plan
            try {
                const defaultResponse = await floorPlanAPI.createDefault();
                console.log('Created default floor plan:', defaultResponse.data);
                setFloorPlan(defaultResponse.data);
                setAreas(defaultResponse.data.areas || []);
                if (defaultResponse.data.areas && defaultResponse.data.areas.length > 0) {
                    setSelectedArea(defaultResponse.data.areas[0]);
                }
            } catch (createErr) {
                console.error('Error creating default floor plan:', createErr);
            }
        } finally {
            setLoading(false);
        }
    };

    // API calls
    const loadMenuItems = async () => {
        try {
            setLoading(true);
            const response = await menuAPI.getAvailable();
            setMenuItems(response.data);
        } catch (err) {
            setError('Грешка при вчитување на менито');
            console.error('Error loading menu items:', err);
        } finally {
            setLoading(false);
        }
    };

    const loadActiveOrders = async () => {
        try {
            const response = await orderAPI.getAll();
            const ordersMap = {};
            response.data.forEach(order => {
                ordersMap[order.tableNumber] = {
                    ...order,
                    items: order.items || [],
                    total: order.totalAmount || 0
                };
            });
            setOrders(ordersMap);
        } catch (err) {
            console.error('Error loading orders:', err);
        }
    };

    // Enhanced order loading for table
    const loadOrderForTable = async (tableNumber) => {
        try {
            console.log('Loading order for table:', tableNumber);
            const response = await orderAPI.getOrCreateForTable(tableNumber);
            const order = response.data;

            console.log('Loaded order:', order);

            // Update local orders state
            setOrders(prev => ({
                ...prev,
                [tableNumber]: {
                    ...order,
                    items: order.items || [],
                    total: order.totalAmount || 0
                }
            }));

            return order;
        } catch (err) {
            console.error('Error loading order for table:', tableNumber, err);

            // Check if it's a sent order error
            if (err.response?.status === 400 && err.response?.data?.message?.includes('испратена')) {
                setError('Оваа нарачка е веќе испратена. Започнува нова нарачка за оваа маса.');
                // The backend should have created a new order, so try loading again
                try {
                    const retryResponse = await orderAPI.getOrCreateForTable(tableNumber);
                    const newOrder = retryResponse.data;
                    setOrders(prev => ({
                        ...prev,
                        [tableNumber]: {
                            ...newOrder,
                            items: newOrder.items || [],
                            total: newOrder.totalAmount || 0
                        }
                    }));
                    return newOrder;
                } catch (retryErr) {
                    setError('Грешка при креирање на нова нарачка');
                    console.error('Error creating new order:', retryErr);
                }
            } else {
                setError('Грешка при вчитување на нарачката');
            }
        }
    };

    // Get current order for selected table
    const getCurrentOrder = () => {
        if (!selectedTable) return { items: [], total: 0 };
        return orders[selectedTable] || { items: [], total: 0 };
    };

    // Helper function to check if an item was sent
    const isItemSent = (item, order = null) => {
        const currentOrder = order || getCurrentOrder();
        // If order was sent, consider all items as sent
        // You could enhance this with a sentQuantity field on items
        return currentOrder.status === 'ИСПРАТЕНА';
    };

    // Helper function to check if item can be removed
    const canRemoveItem = (item) => {
        const currentOrder = getCurrentOrder();
        return currentOrder.status === 'ОТВОРЕНА' && !isItemSent(item, currentOrder);
    };

    // Helper function to check if item quantity can be reduced
    const canReduceQuantity = (item) => {
        const currentOrder = getCurrentOrder();
        return currentOrder.status === 'ОТВОРЕНА' && !isItemSent(item, currentOrder);
    };

    // Enhanced add item with better error handling
    const addItemToOrder = async (menuItem, quantity, notes) => {
        try {
            const currentOrder = getCurrentOrder();
            const orderId = currentOrder.id;

            if (!orderId) {
                setError('Грешка: Нарачката не е пронајдена');
                return;
            }

            console.log('Adding item to order:', orderId, 'Item:', menuItem.id, 'Quantity:', quantity);

            const response = await orderAPI.addItem(orderId, {
                menuItemId: menuItem.id,
                quantity: parseInt(quantity),
                notes: notes || ''
            });

            const updatedOrder = response.data;
            setOrders(prev => ({
                ...prev,
                [selectedTable]: {
                    ...updatedOrder,
                    items: updatedOrder.items || [],
                    total: updatedOrder.totalAmount || 0
                }
            }));

            setShowAddItemModal(false);
            setSelectedMenuItem(null);

            console.log('Successfully added item to order');

        } catch (err) {
            console.error('Error adding item:', err);

            // Handle specific error cases
            if (err.response?.data?.message) {
                const errorMessage = err.response.data.message;
                setError(errorMessage);

                // If it's a sent order error, refresh the page or reload orders
                if (errorMessage.includes('испратена нарачка')) {
                    setTimeout(() => {
                        loadOrderForTable(selectedTable);
                    }, 2000);
                }
            } else {
                setError('Грешка при додавање на производот');
            }
        }
    };

    // Enhanced remove item with better error handling
    const removeItemFromOrder = async (itemId) => {
        try {
            const currentOrder = getCurrentOrder();

            console.log('Removing item:', itemId, 'from order:', currentOrder.id);

            const response = await orderAPI.removeItem(currentOrder.id, itemId);

            const updatedOrder = response.data;
            setOrders(prev => ({
                ...prev,
                [selectedTable]: {
                    ...updatedOrder,
                    items: updatedOrder.items || [],
                    total: updatedOrder.totalAmount || 0
                }
            }));

            console.log('Successfully removed item from order');

        } catch (err) {
            console.error('Error removing item:', err);

            if (err.response?.data?.message) {
                const errorMessage = err.response.data.message;
                setError(errorMessage);

                // Show specific error for sent items
                if (errorMessage.includes('испратена') || errorMessage.includes('испратен')) {
                    alert('⚠️ Не можете да отстраните производ од веќе испратена нарачка!\n\nВеќе испратените производи не можат да се менуваат.');
                }
            } else {
                setError('Грешка при отстранување на производот');
            }
        }
    };

    // Enhanced update quantity with better error handling
    const updateItemQuantity = async (itemId, newQuantity) => {
        try {
            const currentOrder = getCurrentOrder();

            console.log('Updating item quantity:', itemId, 'to:', newQuantity, 'in order:', currentOrder.id);

            const response = await orderAPI.updateQuantity(currentOrder.id, itemId, newQuantity);

            const updatedOrder = response.data;
            setOrders(prev => ({
                ...prev,
                [selectedTable]: {
                    ...updatedOrder,
                    items: updatedOrder.items || [],
                    total: updatedOrder.totalAmount || 0
                }
            }));

            console.log('Successfully updated item quantity');

        } catch (err) {
            console.error('Error updating quantity:', err);

            if (err.response?.data?.message) {
                const errorMessage = err.response.data.message;
                setError(errorMessage);

                // Show specific error messages
                if (errorMessage.includes('намалите') || errorMessage.includes('испратен')) {
                    alert('⚠️ Не можете да ја намалите количината на веќе испратен производ!\n\nМожете само да додадете повеќе од истиот производ.');
                }
            } else {
                setError('Грешка при ажурирање на количината');
            }
        }
    };

    // Enhanced send order with better validation
    const sendOrder = async () => {
        try {
            const currentOrder = getCurrentOrder();

            // Validate order before sending
            if (!currentOrder.id) {
                setError('Нема нарачка за испраќање');
                return;
            }

            if (!currentOrder.items || currentOrder.items.length === 0) {
                setError('Не можете да испратите празна нарачка');
                return;
            }

            if (currentOrder.status === 'ИСПРАТЕНА') {
                setError('Оваа нарачка е веќе испратена');
                return;
            }

            console.log('Sending order:', currentOrder.id);

            await orderAPI.send(currentOrder.id);

            // Update order status locally
            setOrders(prev => ({
                ...prev,
                [selectedTable]: {
                    ...prev[selectedTable],
                    status: 'ИСПРАТЕНА'
                }
            }));

            alert('Нарачката е испратена во кујна/бар!');
            setCurrentView('tables');

        } catch (err) {
            console.error('Error sending order:', err);

            if (err.response?.data?.message) {
                setError(err.response.data.message);
            } else {
                setError('Грешка при испраќање на нарачката');
            }
        }
    };

    // Enhanced close order
    const closeOrder = async (tableNumber) => {
        try {
            const order = orders[tableNumber];

            if (!order) {
                setError('Нема нарачка за затворање');
                return;
            }

            if (order.status !== 'ИСПРАТЕНА') {
                setError('Можете да затворате само испратени нарачки');
                return;
            }

            console.log('Closing order:', order.id, 'for table:', tableNumber);

            await orderAPI.close(order.id);

            // Remove order from local state
            setOrders(prev => {
                const newOrders = { ...prev };
                delete newOrders[tableNumber];
                return newOrders;
            });

            alert('Нарачката е затворена и сметката е отпечатена!');

        } catch (err) {
            console.error('Error closing order:', err);

            if (err.response?.data?.message) {
                setError(err.response.data.message);
            } else {
                setError('Грешка при затворање на нарачката');
            }
        }
    };

    // Admin functions
    const handleAdminLogin = async () => {
        try {
            const response = await adminAPI.login(adminCode);
            if (response.data.success) {
                setIsAdminAuthenticated(true);
                setShowAdminLogin(false);
                setCurrentView('admin');
                setAdminCode('');
                await loadAllMenuItems();
            }
        } catch (err) {
            alert('Неточен админ код!');
            console.error('Admin login error:', err);
        }
    };

    const loadAllMenuItems = async () => {
        try {
            const response = await menuAPI.getAll();
            setMenuItems(response.data);
        } catch (err) {
            console.error('Error loading all menu items:', err);
        }
    };

    const addNewProduct = async (productData) => {
        try {
            await menuAPI.create(productData);
            await loadAllMenuItems();
        } catch (err) {
            setError('Грешка при додавање на производот');
            console.error('Error adding product:', err);
            throw err;
        }
    };

    const deleteProduct = async (productId) => {
        if (window.confirm('Дали сте сигурни дека сакате да го избришете производот?')) {
            try {
                await menuAPI.delete(productId);
                await loadAllMenuItems();
                alert('Производот е избришан!');
            } catch (err) {
                setError('Грешка при бришење на производот');
                console.error('Error deleting product:', err);
            }
        }
    };

    const toggleProductAvailability = async (productId) => {
        try {
            await menuAPI.toggleAvailability(productId);
            await loadAllMenuItems();
        } catch (err) {
            setError('Грешка при ажурирање на достапноста');
            console.error('Error toggling availability:', err);
        }
    };

    // Filter menu items by category
    const filteredMenuItems = selectedCategory === 'all'
        ? menuItems.filter(item => item.available)
        : menuItems.filter(item => item.category === selectedCategory && item.available);

    // Get table status with order info
    const getTableStatus = (tableNumber) => {
        const order = orders[tableNumber];
        if (!order || !order.items || order.items.length === 0) {
            return { status: 'AVAILABLE', bgColor: 'bg-green-100 border-green-500 hover:bg-green-200', statusText: 'Достапна', statusColor: 'text-green-700' };
        }

        if (order.status === 'ОТВОРЕНА') {
            return { status: 'OCCUPIED', bgColor: 'bg-red-100 border-red-500 hover:bg-red-200', statusText: 'Зафатена', statusColor: 'text-red-700' };
        } else if (order.status === 'ИСПРАТЕНА') {
            return { status: 'SENT', bgColor: 'bg-orange-100 border-orange-500 hover:bg-orange-200', statusText: 'Испратена', statusColor: 'text-orange-700' };
        }

        return { status: 'AVAILABLE', bgColor: 'bg-green-100 border-green-500 hover:bg-green-200', statusText: 'Достапна', statusColor: 'text-green-700' };
    };

    // Visual Tables View
    const VisualTablesView = () => (
        <div className="p-6">
            <div className="flex justify-between items-center mb-6">
                <h1 className="text-3xl font-bold text-gray-800">Маси - {selectedArea?.name || 'Сите области'}</h1>
                <div className="flex gap-2">
                    <button
                        onClick={() => setShowAdminLogin(true)}
                        className="flex items-center gap-2 px-4 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors"
                    >
                        <Settings size={20} />
                        Админ
                    </button>
                </div>
            </div>

            {/* Area Selector */}
            {areas.length > 0 && (
                <div className="mb-6">
                    <label className="block text-sm font-medium mb-2">Избери област:</label>
                    <select
                        value={selectedArea?.id || ''}
                        onChange={(e) => {
                            const areaId = parseInt(e.target.value);
                            const area = areas.find(a => a.id === areaId);
                            setSelectedArea(area);
                        }}
                        className="px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                        <option value="">Сите области</option>
                        {areas.map(area => (
                            <option key={area.id} value={area.id}>{area.name}</option>
                        ))}
                    </select>
                </div>
            )}

            {error && (
                <div className="mb-4 p-4 bg-red-100 border border-red-400 text-red-700 rounded">
                    {error}
                    <button onClick={() => setError('')} className="ml-2 font-bold">×</button>
                </div>
            )}

            {/* Visual Floor Plan */}
            {floorPlan && (
                <div className="bg-gray-50 border-2 border-gray-300 rounded-lg p-4 mb-6 relative" style={{ minHeight: '500px' }}>
                    {areas
                        .filter(area => !selectedArea || area.id === selectedArea.id)
                        .map(area => (
                            <div key={`area-${area.id}`}>
                                {/* Area Container */}
                                <div
                                    className="absolute border-2 rounded-lg flex flex-col items-center justify-center"
                                    style={{
                                        left: `${area.positionX}px`,
                                        top: `${area.positionY}px`,
                                        width: `${area.width}px`,
                                        height: `${area.height}px`,
                                        backgroundColor: `${area.color}20`,
                                        borderColor: area.color,
                                        zIndex: 1
                                    }}
                                >
                                    <span className="text-sm font-medium text-center px-2" style={{ color: area.color }}>
                                        {area.name}
                                    </span>
                                </div>

                                {/* Tables in Area */}
                                {area.tables && area.tables.map(table => {
                                    const tableStatus = getTableStatus(table.tableNumber);
                                    const order = orders[table.tableNumber];

                                    return (
                                        <div
                                            key={`table-${table.id}`}
                                            className="absolute border-2 border-gray-600 rounded flex flex-col items-center justify-center font-bold text-white cursor-pointer hover:shadow-lg transition-all"
                                            style={{
                                                left: `${area.positionX + (table.positionX || 0)}px`,
                                                top: `${area.positionY + (table.positionY || 0)}px`,
                                                width: `${table.width || 80}px`,
                                                height: `${table.height || 80}px`,
                                                backgroundColor: tableStatus.status === 'OCCUPIED' ? '#ef4444' :
                                                    tableStatus.status === 'SENT' ? '#f59e0b' : '#10b981',
                                                borderRadius: table.shape === 'CIRCLE' ? '50%' :
                                                    table.shape === 'SQUARE' ? '8px' : '12px',
                                                zIndex: 10
                                            }}
                                            onClick={async () => {
                                                setSelectedTable(table.tableNumber);
                                                await loadOrderForTable(table.tableNumber);
                                                setCurrentView('order');
                                            }}
                                        >
                                            <span className="text-lg font-bold">{table.tableNumber}</span>
                                            <span className="text-xs">{table.capacity} места</span>
                                            {order && order.total > 0 && (
                                                <span className="text-xs">{order.total.toFixed(0)} ден</span>
                                            )}

                                            {tableStatus.status === 'SENT' && (
                                                <button
                                                    onClick={(e) => {
                                                        e.stopPropagation();
                                                        closeOrder(table.tableNumber);
                                                    }}
                                                    className="absolute -top-2 -right-2 w-6 h-6 bg-green-500 text-white rounded-full flex items-center justify-center hover:bg-green-600 text-xs"
                                                >
                                                    ✓
                                                </button>
                                            )}
                                        </div>
                                    );
                                })}
                            </div>
                        ))}

                    {/* Empty state */}
                    {(!selectedArea || !selectedArea.tables || selectedArea.tables.length === 0) && (
                        <div className="absolute inset-0 flex items-center justify-center text-gray-500">
                            <div className="text-center">
                                <Grid size={48} className="mx-auto mb-4 opacity-50" />
                                <p className="text-lg">Нема маси во оваа област</p>
                                <p className="text-sm">Користете го админ панелот за да додадете маси</p>
                            </div>
                        </div>
                    )}
                </div>
            )}

            {/* Active Orders Summary */}
            <div className="bg-white rounded-lg shadow-md p-6">
                <h2 className="text-xl font-bold mb-4">Активни Нарачки</h2>
                {Object.keys(orders).length === 0 ? (
                    <p className="text-gray-500 text-center py-4">Нема активни нарачки</p>
                ) : (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                        {Object.entries(orders).map(([tableNumber, order]) => (
                            <div
                                key={tableNumber}
                                className="border rounded-lg p-4 cursor-pointer hover:bg-gray-50"
                                onClick={async () => {
                                    setSelectedTable(parseInt(tableNumber));
                                    await loadOrderForTable(parseInt(tableNumber));
                                    setCurrentView('order');
                                }}
                            >
                                <div className="flex justify-between items-center mb-2">
                                    <span className="font-bold">Маса {tableNumber}</span>
                                    <span className={`px-2 py-1 rounded text-sm ${order.status === 'ОТВОРЕНА' ? 'bg-red-100 text-red-700' : 'bg-orange-100 text-orange-700'
                                        }`}>
                                        {order.status}
                                    </span>
                                </div>
                                <div className="text-sm text-gray-600">
                                    {order.items ? order.items.length : 0} производи
                                </div>
                                <div className="font-bold text-blue-600">
                                    {order.total ? order.total.toFixed(2) : '0.00'} ден
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );

    // Order View
    const OrderView = () => {
        const currentOrder = getCurrentOrder();

        return (
            <div className="flex h-screen">
                {/* Menu Items */}
                <div className="flex-1 p-6 overflow-y-auto">
                    <div className="flex justify-between items-center mb-6">
                        <h1 className="text-2xl font-bold">Нарачка - Маса {selectedTable}</h1>
                        <button
                            onClick={() => setCurrentView('tables')}
                            className="flex items-center gap-2 px-4 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors"
                        >
                            <Home size={20} />
                            Назад
                        </button>
                    </div>

                    {error && (
                        <div className="mb-4 p-4 bg-red-100 border border-red-400 text-red-700 rounded">
                            {error}
                            <button onClick={() => setError('')} className="ml-2 font-bold">×</button>
                        </div>
                    )}

                    {/* Order Status Warning */}
                    {currentOrder.status === 'ИСПРАТЕНА' && (
                        <div className="mb-4 p-4 bg-orange-100 border border-orange-400 text-orange-700 rounded">
                            <strong>⚠️ Оваа нарачка е веќе испратена!</strong>
                            <p className="text-sm mt-1">Новите производи ќе бидат додадени во нова нарачка.</p>
                        </div>
                    )}

                    {/* Category Filter */}
                    <div className="flex gap-2 mb-6 flex-wrap">
                        <button
                            onClick={() => setSelectedCategory('all')}
                            className={`px-4 py-2 rounded-lg transition-colors ${selectedCategory === 'all' ? 'bg-blue-500 text-white' : 'bg-gray-200 hover:bg-gray-300'
                                }`}
                        >
                            Сите
                        </button>
                        {Object.entries(categories).map(([key, category]) => {
                            const Icon = category.icon;
                            return (
                                <button
                                    key={key}
                                    onClick={() => setSelectedCategory(key)}
                                    className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-colors ${selectedCategory === key ? `${category.color} text-white` : 'bg-gray-200 hover:bg-gray-300'
                                        }`}
                                >
                                    <Icon size={16} />
                                    {category.name}
                                </button>
                            );
                        })}
                    </div>

                    {/* Menu Items Grid */}
                    {loading ? (
                        <div className="text-center py-8">Се вчитува...</div>
                    ) : (
                        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                            {filteredMenuItems.map(item => {
                                const category = categories[item.category];
                                const Icon = category.icon;

                                return (
                                    <div
                                        key={item.id}
                                        onClick={() => {
                                            setSelectedMenuItem(item);
                                            setShowAddItemModal(true);
                                        }}
                                        className="bg-white rounded-lg shadow-md p-4 cursor-pointer hover:shadow-lg transition-shadow border-l-4"
                                        style={{
                                            borderLeftColor: category.color.replace('bg-', '').replace('-500', '') === 'green' ? '#10b981' :
                                                category.color.replace('bg-', '').replace('-500', '') === 'blue' ? '#3b82f6' :
                                                    category.color.replace('bg-', '').replace('-500', '') === 'purple' ? '#8b5cf6' : '#ec4899'
                                        }}
                                    >
                                        <div className="flex items-center gap-2 mb-2">
                                            <Icon size={16} className="text-gray-600" />
                                            <span className="text-xs text-gray-500">{item.printDestination}</span>
                                        </div>
                                        <h3 className="font-bold text-gray-800 mb-2">{item.name}</h3>
                                        <div className="flex justify-between items-center">
                                            <span className="text-sm text-gray-600">{category.name}</span>
                                            <span className="font-bold text-blue-600">{item.price} ден</span>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </div>

                {/* Enhanced Order Summary */}
                <div className="w-96 bg-white shadow-lg p-6 overflow-y-auto">
                    <div className="flex items-center gap-2 mb-6">
                        <ShoppingCart size={24} />
                        <h2 className="text-xl font-bold">Нарачка</h2>
                        {currentOrder.status && (
                            <span className={`px-2 py-1 rounded text-sm ${currentOrder.status === 'ОТВОРЕНА' ? 'bg-green-100 text-green-700' : 'bg-orange-100 text-orange-700'
                                }`}>
                                {currentOrder.status}
                            </span>
                        )}
                    </div>

                    {(!currentOrder.items || currentOrder.items.length === 0) ? (
                        <div className="text-center py-8 text-gray-500">
                            <ShoppingCart size={48} className="mx-auto mb-4 opacity-50" />
                            <p>Додајте производи во нарачката</p>
                        </div>
                    ) : (
                        <>
                            <div className="space-y-4 mb-6">
                                {currentOrder.items.map((item, index) => {
                                    const itemSent = isItemSent(item, currentOrder);

                                    return (
                                        <div
                                            key={item.id || index}
                                            className={`border rounded-lg p-3 ${itemSent ? 'bg-orange-50 border-orange-200' : ''}`}
                                        >
                                            <div className="flex justify-between items-start mb-2">
                                                <div className="flex-1">
                                                    <div className="flex items-center gap-2">
                                                        <h4 className="font-medium">
                                                            {item.menuItem ? item.menuItem.name : item.name}
                                                        </h4>
                                                        {itemSent && (
                                                            <span className="text-xs bg-orange-100 text-orange-700 px-2 py-1 rounded">
                                                                Испратено
                                                            </span>
                                                        )}
                                                    </div>
                                                    {item.notes && (
                                                        <p className="text-xs text-gray-500 italic">{item.notes}</p>
                                                    )}
                                                </div>
                                                {canRemoveItem(item) && (
                                                    <button
                                                        onClick={() => removeItemFromOrder(item.id)}
                                                        className="text-red-500 hover:text-red-700 ml-2"
                                                        title="Отстрани производ"
                                                    >
                                                        <Trash2 size={16} />
                                                    </button>
                                                )}
                                            </div>

                                            <div className="flex justify-between items-center">
                                                {/* Quantity Controls */}
                                                <div className="flex items-center gap-2">
                                                    {canReduceQuantity(item) ? (
                                                        // Editable quantity for open orders
                                                        <>
                                                            <button
                                                                onClick={() => updateItemQuantity(item.id, item.quantity - 1)}
                                                                className="w-8 h-8 flex items-center justify-center bg-gray-200 rounded hover:bg-gray-300"
                                                                disabled={item.quantity <= 1}
                                                            >
                                                                <Minus size={16} />
                                                            </button>
                                                            <span className="w-8 text-center font-medium">{item.quantity}</span>
                                                            <button
                                                                onClick={() => updateItemQuantity(item.id, item.quantity + 1)}
                                                                className="w-8 h-8 flex items-center justify-center bg-gray-200 rounded hover:bg-gray-300"
                                                            >
                                                                <Plus size={16} />
                                                            </button>
                                                        </>
                                                    ) : (
                                                        // Read-only quantity for sent items
                                                        <div className="flex items-center gap-2">
                                                            {itemSent && currentOrder.status === 'ОТВОРЕНА' ? (
                                                                // Allow only increasing quantity for sent items in reopened orders
                                                                <>
                                                                    <span className="px-2 py-1 bg-gray-100 rounded text-sm">
                                                                        {item.quantity}x (испратено)
                                                                    </span>
                                                                    <button
                                                                        onClick={() => updateItemQuantity(item.id, item.quantity + 1)}
                                                                        className="w-8 h-8 flex items-center justify-center bg-green-200 rounded hover:bg-green-300"
                                                                        title="Додај повеќе (ќе биде нова ставка)"
                                                                    >
                                                                        <Plus size={16} />
                                                                    </button>
                                                                </>
                                                            ) : (
                                                                <span className="px-2 py-1 bg-gray-100 rounded text-sm">
                                                                    {item.quantity}x
                                                                </span>
                                                            )}
                                                        </div>
                                                    )}
                                                </div>

                                                <span className="font-bold">
                                                    {item.totalPrice ? item.totalPrice.toFixed(2) : '0.00'} ден
                                                </span>
                                            </div>

                                            {/* Additional info for sent items */}
                                            {itemSent && (
                                                <div className="mt-2 text-xs text-orange-600 bg-orange-50 p-2 rounded">
                                                    ⚠️ Овој производ е веќе испратен и не може да се намали или отстрани
                                                </div>
                                            )}
                                        </div>
                                    );
                                })}
                            </div>

                            {/* Total and Action Buttons */}
                            <div className="border-t pt-4 mb-6">
                                <div className="flex justify-between items-center text-xl font-bold">
                                    <span>ВКУПНО:</span>
                                    <span className="text-blue-600">
                                        {currentOrder.total ? currentOrder.total.toFixed(2) : '0.00'} ден
                                    </span>
                                </div>
                            </div>

                            {/* Action Buttons */}
                            {currentOrder.status === 'ОТВОРЕНА' && (
                                <button
                                    onClick={sendOrder}
                                    className="w-full flex items-center justify-center gap-2 bg-green-500 text-white py-3 px-4 rounded-lg hover:bg-green-600 transition-colors font-medium"
                                >
                                    <Send size={20} />
                                    Испрати во Кујна/Бар
                                </button>
                            )}

                            {currentOrder.status === 'ИСПРАТЕНА' && (
                                <div className="space-y-2">
                                    <div className="w-full flex items-center justify-center gap-2 bg-orange-100 text-orange-700 py-3 px-4 rounded-lg font-medium">
                                        ✅ Нарачката е испратена
                                    </div>
                                    <button
                                        onClick={() => closeOrder(selectedTable)}
                                        className="w-full flex items-center justify-center gap-2 bg-blue-500 text-white py-3 px-4 rounded-lg hover:bg-blue-600 transition-colors font-medium"
                                    >
                                        💳 Затвори и наплати
                                    </button>
                                </div>
                            )}
                        </>
                    )}
                </div>
            </div>
        );
    };

    // Admin View
    const AdminView = () => (
        <div className="p-6">
            <div className="flex justify-between items-center mb-6">
                <h1 className="text-3xl font-bold text-gray-800">Админ Панел</h1>
                <button
                    onClick={() => {
                        setCurrentView('tables');
                        setIsAdminAuthenticated(false);
                    }}
                    className="flex items-center gap-2 px-4 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors"
                >
                    <Home size={20} />
                    Главна страна
                </button>
            </div>

            {error && (
                <div className="mb-4 p-4 bg-red-100 border border-red-400 text-red-700 rounded">
                    {error}
                    <button onClick={() => setError('')} className="ml-2 font-bold">×</button>
                </div>
            )}

            <AdminProductForm
                categories={categories}
                menuItems={menuItems}
                onAddProduct={addNewProduct}
                onToggleAvailability={toggleProductAvailability}
                onDeleteProduct={deleteProduct}
                onGoToFloorPlan={() => setCurrentView('floorplan')}
            />
        </div>
    );

    return (
        <div className="min-h-screen bg-gray-100">
            {/* Add Item Modal */}
            {showAddItemModal && selectedMenuItem && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-lg p-6 w-96 max-w-90vw">
                        <div className="flex justify-between items-center mb-4">
                            <h3 className="text-lg font-bold">Додај Производ</h3>
                            <button
                                onClick={() => setShowAddItemModal(false)}
                                className="text-gray-500 hover:text-gray-700"
                            >
                                <X size={20} />
                            </button>
                        </div>
                        <div className="mb-4">
                            <h4 className="font-medium">{selectedMenuItem.name}</h4>
                            <p className="text-blue-600 font-bold">{selectedMenuItem.price} ден</p>
                        </div>
                        <form onSubmit={(e) => {
                            e.preventDefault();
                            const quantity = parseInt(e.target.quantity.value);
                            const notes = e.target.notes.value;
                            addItemToOrder(selectedMenuItem, quantity, notes);
                        }}>
                            <div className="space-y-4">
                                <div>
                                    <label className="block text-sm font-medium mb-1">Количина:</label>
                                    <input
                                        type="number"
                                        name="quantity"
                                        defaultValue="1"
                                        min="1"
                                        className="w-full p-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium mb-1">Забелешки:</label>
                                    <textarea
                                        name="notes"
                                        rows="2"
                                        placeholder="Дополнителни забелешки..."
                                        className="w-full p-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    />
                                </div>
                                <div className="flex gap-2">
                                    <button
                                        type="button"
                                        onClick={() => setShowAddItemModal(false)}
                                        className="flex-1 px-4 py-2 bg-gray-300 rounded hover:bg-gray-400 transition-colors"
                                    >
                                        Откажи
                                    </button>
                                    <button
                                        type="submit"
                                        className="flex-1 px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 transition-colors"
                                    >
                                        Додај
                                    </button>
                                </div>
                            </div>
                        </form>
                    </div>
                </div>
            )}

            {/* Admin Login Modal */}
            {showAdminLogin && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
                    <div className="bg-white rounded-lg p-6 w-80">
                        <div className="flex justify-between items-center mb-4">
                            <h3 className="text-lg font-bold">Админ Пристап</h3>
                            <button
                                onClick={() => setShowAdminLogin(false)}
                                className="text-gray-500 hover:text-gray-700"
                            >
                                <X size={20} />
                            </button>
                        </div>
                        <div className="space-y-4">
                            <div>
                                <label className="block text-sm font-medium mb-1">Админ Код:</label>
                                <input
                                    type="password"
                                    value={adminCode}
                                    onChange={(e) => setAdminCode(e.target.value)}
                                    placeholder="Внесете код..."
                                    className="w-full p-3 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    onKeyPress={(e) => e.key === 'Enter' && handleAdminLogin()}
                                />
                            </div>
                            <div className="flex gap-2">
                                <button
                                    onClick={() => setShowAdminLogin(false)}
                                    className="flex-1 px-4 py-2 bg-gray-300 rounded hover:bg-gray-400 transition-colors"
                                >
                                    Откажи
                                </button>
                                <button
                                    onClick={handleAdminLogin}
                                    className="flex-1 px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 transition-colors"
                                >
                                    Пристапи
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            )}

            {/* Main Content */}
            {currentView === 'tables' && <VisualTablesView />}
            {currentView === 'order' && <OrderView />}
            {currentView === 'admin' && isAdminAuthenticated && <AdminView />}
            {currentView === 'floorplan' && isAdminAuthenticated && (
                <FloorPlanManager onBack={() => setCurrentView('admin')} />
            )}
        </div>
    );
};

export default RestaurantPOS;