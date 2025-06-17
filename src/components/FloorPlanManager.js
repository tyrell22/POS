import React, { useState, useEffect } from 'react';
import {
    Grid, Plus, Edit3, Trash2, Move,
    MapPin, Eye, Settings, Home, Palette, Users, Square
} from 'lucide-react';
import { floorPlanAPI, areaAPI } from '../services/api';
import AreaEditor from './AreaEditor';

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

const FloorPlanManager = ({ onBack }) => {
    const [floorPlan, setFloorPlan] = useState(null);
    const [areas, setAreas] = useState([]);
    const [editMode, setEditMode] = useState(false);
    const [selectedArea, setSelectedArea] = useState(null);
    const [showAreaEditor, setShowAreaEditor] = useState(false);
    const [draggedArea, setDraggedArea] = useState(null);
    const [dragOffset, setDragOffset] = useState({ x: 0, y: 0 });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        loadActiveFloorPlan();
    }, []);

    const loadActiveFloorPlan = async () => {
        try {
            setLoading(true);
            // First try to get active floor plan
            const response = await floorPlanAPI.getActive();
            setFloorPlan(response.data);
            setAreas(response.data.areas || []);
        } catch (err) {
            console.error('Error loading active floor plan:', err);
            // No active floor plan, create default
            try {
                await createDefaultFloorPlan();
            } catch (createErr) {
                console.error('Error creating default floor plan:', createErr);
                setError('Грешка при вчитување на планот');
            }
        } finally {
            setLoading(false);
        }
    };

    const createDefaultFloorPlan = async () => {
        try {
            const response = await floorPlanAPI.createDefault();
            setFloorPlan(response.data);
            setAreas(response.data.areas || []);
        } catch (err) {
            console.error('Error creating default floor plan:', err);
            setError('Грешка при креирање на основен план');
            throw err;
        }
    };

    const handleSaveArea = async (areaData, existingArea) => {
        console.log('handleSaveArea called with:', { areaData, existingArea }); // Debug log

        try {
            if (existingArea) {
                // Update existing area
                console.log('Updating area with ID:', existingArea.id, 'Data:', areaData);
                const response = await areaAPI.update(existingArea.id, areaData);
                const updatedArea = response.data;
                setAreas(prev => prev.map(area =>
                    area.id === existingArea.id ? updatedArea : area
                ));
                setShowAreaEditor(false);
                setSelectedArea(null);
                setError('');
            } else {
                // Create new area
                console.log('Creating new area with data:', areaData);
                const response = await areaAPI.create(areaData);
                const area = response.data;
                setAreas(prev => [...prev, area]);
                setShowAreaEditor(false);
                setError('');
            }
        } catch (err) {
            console.error('Error saving area:', err);
            console.error('Error response:', err.response?.data);
            console.error('Error status:', err.response?.status);

            const errorMessage = err.response?.data?.error || err.response?.data?.message || 'Грешка при зачувување на област';
            setError(errorMessage);
            throw new Error('Failed to save area');
        }
    };

    const updateAreaPosition = async (areaId, newPosition) => {
        try {
            const response = await areaAPI.updatePosition(areaId, newPosition);
            const updatedArea = response.data;
            setAreas(prev => prev.map(area =>
                area.id === areaId ? updatedArea : area
            ));
        } catch (err) {
            console.error('Error updating area position:', err);
        }
    };

    const deleteArea = async (areaId) => {
        if (!window.confirm('Дали сте сигурни дека сакате да ја избришете оваа област?')) {
            return;
        }

        try {
            await areaAPI.delete(areaId);
            setAreas(prev => prev.filter(area => area.id !== areaId));
            setSelectedArea(null);
        } catch (err) {
            console.error('Error deleting area:', err);
            const errorMessage = err.response?.data?.message || err.response?.data?.error || 'Грешка при бришење на област';
            setError(errorMessage);
        }
    };

    const handleMouseDown = (e, area) => {
        if (!editMode) return;

        e.preventDefault();
        const rect = e.currentTarget.getBoundingClientRect();

        setDraggedArea(area);
        setDragOffset({
            x: e.clientX - rect.left,
            y: e.clientY - rect.top
        });
    };

    const handleMouseMove = (e) => {
        if (!draggedArea || !editMode) return;

        e.preventDefault();
        const containerRect = e.currentTarget.getBoundingClientRect();

        const newX = Math.max(0, e.clientX - containerRect.left - dragOffset.x);
        const newY = Math.max(0, e.clientY - containerRect.top - dragOffset.y);

        setAreas(prev => prev.map(area =>
            area.id === draggedArea.id
                ? { ...area, positionX: Math.round(newX), positionY: Math.round(newY) }
                : area
        ));
    };

    const handleMouseUp = () => {
        if (draggedArea && editMode) {
            const area = areas.find(a => a.id === draggedArea.id);
            if (area) {
                updateAreaPosition(draggedArea.id, {
                    x: area.positionX,
                    y: area.positionY,
                    width: area.width,
                    height: area.height
                });
            }
        }
        setDraggedArea(null);
        setDragOffset({ x: 0, y: 0 });
    };

    const handleOpenAreaEditor = (area = null) => {
        setSelectedArea(area);
        setShowAreaEditor(true);
    };

    const handleCloseAreaEditor = () => {
        setShowAreaEditor(false);
        setSelectedArea(null);
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center h-64">
                <div className="text-center">
                    <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500 mx-auto mb-4"></div>
                    <p>Се вчитува планот...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="p-6">
            {/* Header */}
            <div className="flex justify-between items-center mb-6">
                <div>
                    <h1 className="text-3xl font-bold text-gray-800">План на Ресторан</h1>
                    {floorPlan && (
                        <p className="text-gray-600">{floorPlan.name}</p>
                    )}
                </div>
                <div className="flex gap-2">
                    <button
                        onClick={() => setEditMode(!editMode)}
                        className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-colors ${editMode ? 'bg-green-500 text-white' : 'bg-gray-200'
                            }`}
                    >
                        <Edit3 size={20} />
                        {editMode ? 'Заврши уредување' : 'Уреди план'}
                    </button>
                    <button
                        onClick={() => handleOpenAreaEditor()}
                        className="flex items-center gap-2 px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors"
                    >
                        <Plus size={20} />
                        Додај област
                    </button>
                    <button
                        onClick={onBack}
                        className="flex items-center gap-2 px-4 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors"
                    >
                        <Home size={20} />
                        Назад
                    </button>
                </div>
            </div>

            {/* Error Message */}
            {error && (
                <div className="mb-4 p-4 bg-red-100 border border-red-400 text-red-700 rounded">
                    {error}
                    <button onClick={() => setError('')} className="ml-2 font-bold">×</button>
                </div>
            )}

            {/* Edit Mode Instructions */}
            {editMode && (
                <div className="mb-4 p-4 bg-yellow-100 border border-yellow-400 text-yellow-700 rounded">
                    <strong>Режим на уредување:</strong> Повлечете ги областите за да ги преместите. Кликнете на област за да ја уредите.
                </div>
            )}

            {/* Floor Plan Canvas */}
            <div
                className="relative bg-gray-50 border-2 border-gray-300 rounded-lg overflow-hidden"
                style={{ width: '100%', height: '600px', minHeight: '600px' }}
                onMouseMove={handleMouseMove}
                onMouseUp={handleMouseUp}
                onMouseLeave={handleMouseUp}
            >
                {areas.map(area => {
                    const areaType = areaTypes[area.type] || areaTypes.DINING;
                    const Icon = areaType.icon;

                    return (
                        <div
                            key={area.id}
                            className={`absolute border-2 rounded-lg flex flex-col items-center justify-center cursor-pointer transition-all ${editMode ? 'hover:shadow-lg' : ''
                                } ${selectedArea?.id === area.id ? 'ring-4 ring-blue-400' : ''}`}
                            style={{
                                left: `${area.positionX}px`,
                                top: `${area.positionY}px`,
                                width: `${area.width}px`,
                                height: `${area.height}px`,
                                backgroundColor: `${area.color}20`,
                                borderColor: area.color,
                                borderStyle: editMode && draggedArea?.id === area.id ? 'dashed' : 'solid'
                            }}
                            onMouseDown={(e) => handleMouseDown(e, area)}
                            onClick={() => {
                                if (editMode) {
                                    handleOpenAreaEditor(area);
                                }
                            }}
                        >
                            <Icon size={24} style={{ color: area.color }} />
                            <span className="text-sm font-medium text-center px-2" style={{ color: area.color }}>
                                {area.name}
                            </span>
                            <span className="text-xs text-gray-600">
                                {area.tables ? `${area.tables.length} маси` : '0 маси'}
                            </span>

                            {editMode && (
                                <div className="absolute top-1 right-1 flex gap-1">
                                    <button
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            handleOpenAreaEditor(area);
                                        }}
                                        className="w-6 h-6 bg-blue-500 text-white rounded-full flex items-center justify-center hover:bg-blue-600"
                                    >
                                        <Edit3 size={12} />
                                    </button>
                                    <button
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            deleteArea(area.id);
                                        }}
                                        className="w-6 h-6 bg-red-500 text-white rounded-full flex items-center justify-center hover:bg-red-600"
                                    >
                                        <Trash2 size={12} />
                                    </button>
                                </div>
                            )}
                        </div>
                    );
                })}

                {areas.length === 0 && (
                    <div className="absolute inset-0 flex items-center justify-center text-gray-500">
                        <div className="text-center">
                            <Grid size={48} className="mx-auto mb-4 opacity-50" />
                            <p className="text-lg">Нема области</p>
                            <p className="text-sm">Кликнете "Додај област" за да започнете</p>
                        </div>
                    </div>
                )}
            </div>

            {/* Area Statistics */}
            <div className="mt-6 grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="bg-white rounded-lg shadow p-4">
                    <h3 className="font-bold text-gray-800 mb-2">Вкупно области</h3>
                    <p className="text-2xl font-bold text-blue-600">{areas.length}</p>
                </div>
                <div className="bg-white rounded-lg shadow p-4">
                    <h3 className="font-bold text-gray-800 mb-2">Вкупно маси</h3>
                    <p className="text-2xl font-bold text-green-600">
                        {areas.reduce((total, area) => total + (area.tables ? area.tables.length : 0), 0)}
                    </p>
                </div>
                <div className="bg-white rounded-lg shadow p-4">
                    <h3 className="font-bold text-gray-800 mb-2">Типови области</h3>
                    <div className="space-y-1">
                        {Object.entries(
                            areas.reduce((acc, area) => {
                                acc[area.type] = (acc[area.type] || 0) + 1;
                                return acc;
                            }, {})
                        ).map(([type, count]) => (
                            <div key={type} className="flex justify-between text-sm">
                                <span>{areaTypes[type]?.name || type}</span>
                                <span className="font-medium">{count}</span>
                            </div>
                        ))}
                    </div>
                </div>
            </div>

            {/* Area Editor Modal */}
            <AreaEditor
                isOpen={showAreaEditor}
                onClose={handleCloseAreaEditor}
                selectedArea={selectedArea}
                onSave={handleSaveArea}
                floorPlanId={floorPlan?.id}
            />
        </div>
    );
};

export default FloorPlanManager;