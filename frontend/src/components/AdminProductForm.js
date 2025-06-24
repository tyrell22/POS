import React, { useState } from 'react';
import { Grid, Trash2, Printer } from 'lucide-react';

const AdminProductForm = ({
    categories,
    menuItems,
    onAddProduct,
    onToggleAvailability,
    onDeleteProduct,
    onGoToFloorPlan,
    onGoToPrinterSetup
}) => {
    const [productName, setProductName] = useState('');
    const [productPrice, setProductPrice] = useState('');
    const [productCategory, setProductCategory] = useState('ХРАНА');
    const [printDestination, setPrintDestination] = useState('КУЈНА');

    const handleAddProduct = async () => {
        if (!productName || !productPrice) {
            alert('Ве молиме внесете име и цена!');
            return;
        }

        try {
            await onAddProduct({
                name: productName,
                price: parseFloat(productPrice),
                category: productCategory,
                printDestination: printDestination
            });

            // Reset form
            setProductName('');
            setProductPrice('');
            setProductCategory('ХРАНА');
            setPrintDestination('КУЈНА');

            alert('Производот е додаден успешно!');
        } catch (err) {
            console.error('Error adding product:', err);
        }
    };

    return (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Add New Product */}
            <div className="bg-white rounded-lg shadow-md p-6">
                <h2 className="text-xl font-bold mb-4">Додај Нов Производ</h2>
                <div className="space-y-4">
                    <input
                        type="text"
                        placeholder="Име на производ"
                        value={productName}
                        onChange={(e) => setProductName(e.target.value)}
                        className="w-full p-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                        autoComplete="off"
                    />
                    <input
                        type="number"
                        placeholder="Цена (денари)"
                        value={productPrice}
                        onChange={(e) => setProductPrice(e.target.value)}
                        className="w-full p-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                        autoComplete="off"
                    />
                    <select
                        value={productCategory}
                        onChange={(e) => setProductCategory(e.target.value)}
                        className="w-full p-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                        {Object.entries(categories).map(([key, category]) => (
                            <option key={key} value={key}>{category.name}</option>
                        ))}
                    </select>
                    <select
                        value={printDestination}
                        onChange={(e) => setPrintDestination(e.target.value)}
                        className="w-full p-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
                    >
                        <option value="КУЈНА">Кујна</option>
                        <option value="БАР">Бар</option>
                    </select>
                    <button
                        onClick={handleAddProduct}
                        className="w-full bg-green-500 text-white py-3 px-4 rounded-lg hover:bg-green-600 transition-colors font-medium"
                    >
                        Додај Производ
                    </button>

                    {/* Management Buttons */}
                    <div className="space-y-2">
                        {/* Floor Plan Management Button */}
                        <button
                            onClick={onGoToFloorPlan}
                            className="w-full bg-purple-500 text-white py-3 px-4 rounded-lg hover:bg-purple-600 transition-colors font-medium"
                        >
                            <Grid size={20} className="inline mr-2" />
                            Управувај со План
                        </button>

                        {/* NEW: Printer Setup Button */}
                        <button
                            onClick={onGoToPrinterSetup}
                            className="w-full bg-orange-500 text-white py-3 px-4 rounded-lg hover:bg-orange-600 transition-colors font-medium"
                        >
                            <Printer size={20} className="inline mr-2" />
                            Подеси Принтери
                        </button>
                    </div>
                </div>
            </div>

            {/* Existing Products */}
            <div className="lg:col-span-2">
                <h2 className="text-xl font-bold mb-4">Постоечки Производи ({menuItems.length})</h2>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {menuItems.map(item => {
                        const category = categories[item.category];
                        const Icon = category.icon;

                        return (
                            <div key={item.id} className={`bg-white rounded-lg shadow-md p-4 ${!item.available ? 'opacity-50' : ''}`}>
                                <div className="flex justify-between items-start mb-2">
                                    <div className="flex items-center gap-2">
                                        <Icon size={16} className="text-gray-600" />
                                        <h3 className="font-bold">{item.name}</h3>
                                    </div>
                                    <div className="flex gap-1">
                                        <button
                                            onClick={() => onToggleAvailability(item.id)}
                                            className={`px-2 py-1 text-xs rounded ${item.available ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700'
                                                }`}
                                        >
                                            {item.available ? 'Достапен' : 'Недостапен'}
                                        </button>
                                        <button
                                            onClick={() => onDeleteProduct(item.id)}
                                            className="px-2 py-1 text-xs bg-red-100 text-red-700 rounded hover:bg-red-200"
                                        >
                                            <Trash2 size={12} />
                                        </button>
                                    </div>
                                </div>
                                <div className="flex justify-between items-center">
                                    <span className="text-sm text-gray-600">{category.name} • {item.printDestination}</span>
                                    <span className="font-bold text-blue-600">{item.price} ден</span>
                                </div>
                            </div>
                        );
                    })}
                </div>
            </div>
        </div>
    );
};

export default AdminProductForm;