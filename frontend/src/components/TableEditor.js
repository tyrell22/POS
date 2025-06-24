import React, { useState, useEffect } from 'react';
import { X, Square, Circle, Move } from 'lucide-react';

const TableEditor = ({
    isOpen,
    onClose,
    selectedTable,
    onSave,
    nextTableNumber,
    tableShapes
}) => {
    const [tableNumber, setTableNumber] = useState('');
    const [capacity, setCapacity] = useState(4);
    const [shape, setShape] = useState('RECTANGLE');
    const [width, setWidth] = useState(80);
    const [height, setHeight] = useState(80);
    const [positionX, setPositionX] = useState(0);
    const [positionY, setPositionY] = useState(0);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (isOpen) {
            if (selectedTable) {
                setTableNumber(selectedTable.tableNumber?.toString() || '');
                setCapacity(selectedTable.capacity || 4);
                setShape(selectedTable.shape || 'RECTANGLE');
                setWidth(selectedTable.width || 80);
                setHeight(selectedTable.height || 80);
                setPositionX(selectedTable.positionX || 0);
                setPositionY(selectedTable.positionY || 0);
            } else {
                // New table
                setTableNumber(nextTableNumber?.toString() || '1');
                setCapacity(4);
                setShape('RECTANGLE');
                setWidth(80);
                setHeight(80);
                setPositionX(0);
                setPositionY(0);
            }
        }
    }, [isOpen, selectedTable, nextTableNumber]);

    const handleShapeChange = (newShape) => {
        setShape(newShape);
        if (newShape === 'CIRCLE' || newShape === 'SQUARE') {
            // Make it square for circles and squares
            const size = Math.max(width, height);
            setWidth(size);
            setHeight(size);
        }
    };

    const handleSave = async () => {
        if (!tableNumber || !tableNumber.toString().trim()) {
            alert('Внесете број на маса');
            return;
        }

        if (!capacity || capacity < 1) {
            alert('Внесете валиден капацитет');
            return;
        }

        const tableData = {
            tableNumber: parseInt(tableNumber),
            capacity: parseInt(capacity),
            shape: shape,
            width: parseInt(width),
            height: parseInt(height),
            positionX: parseInt(positionX),
            positionY: parseInt(positionY),
            status: selectedTable?.status || 'AVAILABLE'
        };

        console.log('Saving table data:', tableData); // Debug log

        setLoading(true);
        try {
            await onSave(tableData, selectedTable);
        } catch (error) {
            console.error('Error saving table:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleClose = () => {
        setTableNumber('');
        setCapacity(4);
        setShape('RECTANGLE');
        setWidth(80);
        setHeight(80);
        setPositionX(0);
        setPositionY(0);
        onClose();
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white rounded-lg p-6 w-96 max-w-90vw">
                <div className="flex justify-between items-center mb-4">
                    <h3 className="text-lg font-bold">
                        {selectedTable ? 'Уреди Маса' : 'Додај Нова Маса'}
                    </h3>
                    <button
                        onClick={handleClose}
                        className="text-gray-500 hover:text-gray-700"
                        disabled={loading}
                    >
                        <X size={20} />
                    </button>
                </div>

                <div className="space-y-4">
                    {/* Table Number */}
                    <div>
                        <label className="block text-sm font-medium mb-1">Број на маса:</label>
                        <input
                            type="number"
                            value={tableNumber}
                            onChange={(e) => setTableNumber(e.target.value)}
                            className="w-full p-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                            placeholder="Внесете број на маса..."
                            disabled={loading}
                            min="1"
                        />
                    </div>

                    {/* Capacity */}
                    <div>
                        <label className="block text-sm font-medium mb-1">Капацитет (места):</label>
                        <input
                            type="number"
                            value={capacity}
                            onChange={(e) => setCapacity(parseInt(e.target.value) || 1)}
                            className="w-full p-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                            placeholder="Број на места..."
                            disabled={loading}
                            min="1"
                            max="20"
                        />
                    </div>

                    {/* Shape */}
                    <div>
                        <label className="block text-sm font-medium mb-1">Облик:</label>
                        <select
                            value={shape}
                            onChange={(e) => handleShapeChange(e.target.value)}
                            className="w-full p-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                            disabled={loading}
                        >
                            {Object.entries(tableShapes).map(([key, shapeConfig]) => (
                                <option key={key} value={key}>{shapeConfig.name}</option>
                            ))}
                        </select>
                    </div>

                    {/* Size */}
                    <div className="grid grid-cols-2 gap-3">
                        <div>
                            <label className="block text-sm font-medium mb-1">
                                {shape === 'CIRCLE' ? 'Дијаметар:' : 'Ширина:'}
                            </label>
                            <input
                                type="number"
                                value={width}
                                onChange={(e) => {
                                    const newWidth = parseInt(e.target.value) || 40;
                                    setWidth(newWidth);
                                    if (shape === 'CIRCLE' || shape === 'SQUARE') {
                                        setHeight(newWidth);
                                    }
                                }}
                                className="w-full p-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                                disabled={loading}
                                min="40"
                                max="200"
                            />
                        </div>
                        {shape === 'RECTANGLE' && (
                            <div>
                                <label className="block text-sm font-medium mb-1">Висина:</label>
                                <input
                                    type="number"
                                    value={height}
                                    onChange={(e) => setHeight(parseInt(e.target.value) || 40)}
                                    className="w-full p-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    disabled={loading}
                                    min="40"
                                    max="200"
                                />
                            </div>
                        )}
                    </div>

                    {/* Position */}
                    <div className="grid grid-cols-2 gap-3">
                        <div>
                            <label className="block text-sm font-medium mb-1">Позиција X:</label>
                            <input
                                type="number"
                                value={positionX}
                                onChange={(e) => setPositionX(parseInt(e.target.value) || 0)}
                                className="w-full p-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                                disabled={loading}
                                min="0"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium mb-1">Позиција Y:</label>
                            <input
                                type="number"
                                value={positionY}
                                onChange={(e) => setPositionY(parseInt(e.target.value) || 0)}
                                className="w-full p-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                                disabled={loading}
                                min="0"
                            />
                        </div>
                    </div>

                    {/* Preview */}
                    <div>
                        <label className="block text-sm font-medium mb-2">Преглед:</label>
                        <div className="border rounded p-4 bg-gray-50 flex justify-center">
                            <div
                                className="border-2 border-gray-600 bg-green-500 flex items-center justify-center font-bold text-white"
                                style={{
                                    width: `${Math.min(width, 100)}px`,
                                    height: `${Math.min(height, 100)}px`,
                                    borderRadius: shape === 'CIRCLE' ? '50%' :
                                        shape === 'SQUARE' ? '8px' : '12px'
                                }}
                            >
                                <span className="text-sm">{tableNumber || 'N'}</span>
                            </div>
                        </div>
                        <p className="text-xs text-gray-500 mt-1 text-center">
                            {capacity} места • {shape === 'CIRCLE' ? `⌀${width}px` : `${width}×${height}px`}
                        </p>
                    </div>

                    {/* Actions */}
                    <div className="flex gap-2 pt-4">
                        <button
                            onClick={handleClose}
                            className="flex-1 px-4 py-2 bg-gray-300 rounded hover:bg-gray-400 transition-colors"
                            disabled={loading}
                        >
                            Откажи
                        </button>
                        <button
                            onClick={handleSave}
                            className="flex-1 px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600 transition-colors disabled:opacity-50"
                            disabled={loading}
                        >
                            {loading ? 'Се зачувува...' : (selectedTable ? 'Зачувај' : 'Создај')}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default TableEditor;