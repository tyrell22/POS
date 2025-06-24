import React, { useState, useEffect } from 'react';
import { X, Users, Grid, Square, MapPin, Home, Eye, Settings, Palette } from 'lucide-react';

// Area type configurations
const areaTypes = {
    'DINING': { name: 'Трпезарија', icon: Users, color: '#3B82F6' },
    'BAR': { name: 'Бар', icon: Grid, color: '#8B5CF6' },
    'TERRACE': { name: 'Тераса', icon: Square, color: '#10B981' },
    'VIP': { name: 'VIP', icon: MapPin, color: '#F59E0B' },
    'PRIVATE_ROOM': { name: 'Приватна соба', icon: Home, color: '#EF4444' },
    'OUTDOOR': { name: 'Надворешна', icon: Eye, color: '#059669' },
    'SMOKING': { name: 'Пушачка', icon: Settings, color: '#6B7280' },
    'NON_SMOKING': { name: 'Непушачка', icon: Palette, color: '#14B8A6' }
};

const AreaEditor = ({
    isOpen,
    onClose,
    selectedArea,
    onSave,
    floorPlanId
}) => {
    const [name, setName] = useState('');
    const [description, setDescription] = useState('');
    const [type, setType] = useState('DINING');
    const [color, setColor] = useState('#3B82F6');
    const [loading, setLoading] = useState(false);

    // Initialize form when modal opens or selectedArea changes
    useEffect(() => {
        if (isOpen) {
            if (selectedArea) {
                setName(selectedArea.name || '');
                setDescription(selectedArea.description || '');
                setType(selectedArea.type || 'DINING');
                setColor(selectedArea.color || '#3B82F6');
            } else {
                setName('');
                setDescription('');
                setType('DINING');
                setColor('#3B82F6');
            }
        }
    }, [isOpen, selectedArea]);

    const handleTypeChange = (newType) => {
        setType(newType);
        if (!selectedArea) {
            // Only update color for new areas
            setColor(areaTypes[newType].color);
        }
    };

    const handleSave = async () => {
        if (!name.trim()) {
            alert('Внесете име на областа');
            return;
        }

        setLoading(true);
        try {
            const areaData = {
                floorPlanId: floorPlanId,
                name: name.trim(),
                description: description.trim() || null,
                type: type,
                color: color
            };

            console.log('Sending area data:', areaData); // Debug log

            await onSave(areaData, selectedArea);

            // Reset form
            setName('');
            setDescription('');
            setType('DINING');
            setColor('#3B82F6');
        } catch (error) {
            console.error('Error saving area:', error);
        } finally {
            setLoading(false);
        }
    };

    const handleClose = () => {
        // Reset form when closing
        setName('');
        setDescription('');
        setType('DINING');
        setColor('#3B82F6');
        onClose();
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white rounded-lg p-6 w-96 max-w-90vw">
                <div className="flex justify-between items-center mb-4">
                    <h3 className="text-lg font-bold">
                        {selectedArea ? 'Уреди Област' : 'Додај Нова Област'}
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
                    <div>
                        <label className="block text-sm font-medium mb-1">Име:</label>
                        <input
                            type="text"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            className="w-full p-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                            placeholder="Внесете име на област..."
                            disabled={loading}
                            autoComplete="off"
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium mb-1">Опис:</label>
                        <textarea
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            className="w-full p-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                            rows="2"
                            placeholder="Опис на областа..."
                            disabled={loading}
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium mb-1">Тип:</label>
                        <select
                            value={type}
                            onChange={(e) => handleTypeChange(e.target.value)}
                            className="w-full p-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                            disabled={loading}
                        >
                            {Object.entries(areaTypes).map(([key, areaType]) => (
                                <option key={key} value={key}>{areaType.name}</option>
                            ))}
                        </select>
                    </div>

                    <div>
                        <label className="block text-sm font-medium mb-1">Боја:</label>
                        <div className="flex items-center gap-2">
                            <input
                                type="color"
                                value={color}
                                onChange={(e) => setColor(e.target.value)}
                                className="w-12 h-8 border rounded cursor-pointer"
                                disabled={loading}
                            />
                            <span className="text-sm text-gray-600">{color}</span>
                        </div>
                    </div>

                    <div className="flex gap-2">
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
                            {loading ? 'Се зачувува...' : (selectedArea ? 'Зачувај' : 'Создај')}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default AreaEditor;