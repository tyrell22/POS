import React, { useState, useEffect } from 'react';
import { ShoppingCart, Plus, Minus, Trash2, Send, X, Settings, Home, Utensils, Coffee, Wine, Cake } from 'lucide-react';
import { menuAPI, orderAPI, adminAPI } from '../services/api';
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

    // Load data on component mount
    useEffect(() => {
        loadMenuItems();
        loadActiveOrders();
    }, []);

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

    const loadOrderForTable = async (tableNumber) => {
        try {
            const response = await orderAPI.getOrCreateForTable(tableNumber);
            const order = response.data;
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
            setError('Грешка при вчитување на нарачката');
            console.error('Error loading order:', err);
        }
    };

    // Get current order for selected table
    const getCurrentOrder = () => {
        if (!selectedTable) return { items: [], total: 0 };
        return orders[selectedTable] || { items: [], total: 0 };
    };

    // Add item to order
    const addItemToOrder = async (menuItem, quantity, notes) => {
        try {
            const currentOrder = getCurrentOrder();
            const orderId = currentOrder.id;

            if (!orderId) {
                setError('Грешка: Нарачката не е пронајдена');
                return;
            }

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
        } catch (err) {
            setError('Грешка при додавање на производот');
            console.error('Error adding item:', err);
        }
    };

    // Remove item from order
    const removeItemFromOrder = async (itemId) => {
        try {
            const currentOrder = getCurrentOrder();
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
        } catch (err) {
            setError('Грешка при отстранување на производот');
            console.error('Error removing item:', err);
        }
    };

    // Update item quantity
    const updateItemQuantity = async (itemId, newQuantity) => {
        try {
            const currentOrder = getCurrentOrder();
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
        } catch (err) {
            setError('Грешка при ажурирање на количината');
            console.error('Error updating quantity:', err);
        }
    };

    // Send order to kitchen/bar
    const sendOrder = async () => {
        try {
            const currentOrder = getCurrentOrder();
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
            setError('Грешка при испраќање на нарачката');
            console.error('Error sending order:', err);
        }
    };

    // Close order
    const closeOrder = async (tableNumber) => {
        try {
            const order = orders[tableNumber];
            await orderAPI.close(order.id);

            // Remove order from local state
            setOrders(prev => {
                const newOrders = { ...prev };
                delete newOrders[tableNumber];
                return newOrders;
            });

            alert('Нарачката е затворена и сметката е отпечатена!');
        } catch (err) {
            setError('Грешка при затворање на нарачката');
            console.error('Error closing order:', err);
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
                await loadAllMenuItems(); // Load all items for admin
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
        if (window.confirm('Дали сте сигурни дека сакате да го избришете производот?')) { // eslint-disable-line no-restricted-globals
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

    // Tables View
    const TablesView = () => (
        <div className="p-6">
            <div className="flex justify-between items-center mb-6">
                <h1 className="text-3xl font-bold text-gray-800">Маси</h1>
                <button
                    onClick={() => setShowAdminLogin(true)}
                    className="flex items-center gap-2 px-4 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors"
                >
                    <Settings size={20} />
                    Админ
                </button>
            </div>

            {error && (
                <div className="mb-4 p-4 bg-red-100 border border-red-400 text-red-700 rounded">
                    {error}
                    <button onClick={() => setError('')} className="ml-2 font-bold">×</button>
                </div>
            )}

            <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4 mb-8">
                {Array.from({ length: 12 }, (_, i) => i + 1).map(tableNumber => {
                    const order = orders[tableNumber];
                    const hasOrder = order && order.items && order.items.length > 0;
                    const orderStatus = order?.status || 'ДОСТАПНА';

                    let bgColor = 'bg-green-100 border-green-500 hover:bg-green-200';
                    let statusText = 'Достапна';
                    let statusColor = 'text-green-700';

                    if (hasOrder) {
                        if (orderStatus === 'ОТВОРЕНА') {
                            bgColor = 'bg-red-100 border-red-500 hover:bg-red-200';
                            statusText = 'Зафатена';
                            statusColor = 'text-red-700';
                        } else if (orderStatus === 'ИСПРАТЕНА') {
                            bgColor = 'bg-orange-100 border-orange-500 hover:bg-orange-200';
                            statusText = 'Испратена';
                            statusColor = 'text-orange-700';
                        }
                    }

                    return (
                        <div
                            key={tableNumber}
                            onClick={async () => {
                                setSelectedTable(tableNumber);
                                await loadOrderForTable(tableNumber);
                                setCurrentView('order');
                            }}
                            className={`${bgColor} border-2 rounded-lg p-6 cursor-pointer transition-all duration-200 hover:scale-105`}
                        >
                            <div className="text-center">
                                <div className="text-2xl font-bold text-gray-800 mb-2">
                                    Маса {tableNumber}
                                </div>
                                <div className={`${statusColor} font-medium mb-2`}>
                                    {statusText}
                                </div>
                                {hasOrder && (
                                    <div className="text-sm text-gray-600">
                                        {order.total.toFixed(2)} ден
                                    </div>
                                )}
                                {hasOrder && orderStatus === 'ИСПРАТЕНА' && (
                                    <button
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            closeOrder(tableNumber);
                                        }}
                                        className="mt-2 px-3 py-1 bg-green-500 text-white text-sm rounded hover:bg-green-600 transition-colors"
                                    >
                                        Затвори
                                    </button>
                                )}
                            </div>
                        </div>
                    );
                })}
            </div>

            {/* Active Orders Summary */}
            <div className="bg-white rounded-lg shadow-md p-6">
                <h2 className="text-xl font-bold mb-4">Активни Нарачки</h2>
                {Object.keys(orders).length === 0 ? (
                    <p className="text-gray-500 text-center py-4">Нема активни нарачки</p>
                ) : (
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                        {Object.entries(orders).map(([tableNumber, order]) => (
                            <div key={tableNumber} className="border rounded-lg p-4">
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

                {/* Order Summary */}
                <div className="w-96 bg-white shadow-lg p-6 overflow-y-auto">
                    <div className="flex items-center gap-2 mb-6">
                        <ShoppingCart size={24} />
                        <h2 className="text-xl font-bold">Нарачка</h2>
                    </div>

                    {(!currentOrder.items || currentOrder.items.length === 0) ? (
                        <div className="text-center py-8 text-gray-500">
                            <ShoppingCart size={48} className="mx-auto mb-4 opacity-50" />
                            <p>Додајте производи во нарачката</p>
                        </div>
                    ) : (
                        <>
                            <div className="space-y-4 mb-6">
                                {currentOrder.items.map((item, index) => (
                                    <div key={item.id || index} className="border rounded-lg p-3">
                                        <div className="flex justify-between items-start mb-2">
                                            <div className="flex-1">
                                                <h4 className="font-medium">{item.menuItem ? item.menuItem.name : item.name}</h4>
                                                {item.notes && (
                                                    <p className="text-xs text-gray-500 italic">{item.notes}</p>
                                                )}
                                            </div>
                                            <button
                                                onClick={() => removeItemFromOrder(item.id)}
                                                className="text-red-500 hover:text-red-700 ml-2"
                                            >
                                                <Trash2 size={16} />
                                            </button>
                                        </div>
                                        <div className="flex justify-between items-center">
                                            <div className="flex items-center gap-2">
                                                <button
                                                    onClick={() => updateItemQuantity(item.id, item.quantity - 1)}
                                                    className="w-8 h-8 flex items-center justify-center bg-gray-200 rounded hover:bg-gray-300"
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
                                            </div>
                                            <span className="font-bold">{item.totalPrice ? item.totalPrice.toFixed(2) : '0.00'} ден</span>
                                        </div>
                                    </div>
                                ))}
                            </div>

                            <div className="border-t pt-4 mb-6">
                                <div className="flex justify-between items-center text-xl font-bold">
                                    <span>ВКУПНО:</span>
                                    <span className="text-blue-600">{currentOrder.total ? currentOrder.total.toFixed(2) : '0.00'} ден</span>
                                </div>
                            </div>

                            <button
                                onClick={sendOrder}
                                className="w-full flex items-center justify-center gap-2 bg-green-500 text-white py-3 px-4 rounded-lg hover:bg-green-600 transition-colors font-medium"
                            >
                                <Send size={20} />
                                Испрати во Кујна/Бар
                            </button>
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
            {currentView === 'tables' && <TablesView />}
            {currentView === 'order' && <OrderView />}
            {currentView === 'admin' && isAdminAuthenticated && <AdminView />}
            {currentView === 'floorplan' && isAdminAuthenticated && (
                <FloorPlanManager onBack={() => setCurrentView('admin')} />
            )}
        </div>
    );
};

export default RestaurantPOS;