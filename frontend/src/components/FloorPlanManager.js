import React, { useState, useEffect } from 'react';
import {
    Grid, Plus, Edit3, Trash2, Move, Save, X,
    MapPin, Eye, Settings, Home, Palette, Users, Square
} from 'lucide-react';
import { floorPlanAPI, areaAPI } from '../services/api';
import AreaEditor from './AreaEditor';
import TableEditor from './TableEditor';

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

// Table shape configurations
const tableShapes = {
    'RECTANGLE': { name: 'Правоаголна' },
    'CIRCLE': { name: 'Кружна' },
    'SQUARE': { name: 'Квадратна' }
};

const FloorPlanManager = ({ onBack }) => {
    const [floorPlan, setFloorPlan] = useState(null);
    const [areas, setAreas] = useState([]);
    const [editMode, setEditMode] = useState(false);
    const [selectedArea, setSelectedArea] = useState(null);
    const [selectedTable, setSelectedTable] = useState(null);
    const [showAreaEditor, setShowAreaEditor] = useState(false);
    const [showTableEditor, setShowTableEditor] = useState(false);
    const [draggedItem, setDraggedItem] = useState(null);
    const [dragOffset, setDragOffset] = useState({ x: 0, y: 0 });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [addingTable, setAddingTable] = useState(false);

    useEffect(() => {
        loadActiveFloorPlan();
    }, []);

    const refreshFloorPlan = async () => {
        try {
            setLoading(true);
            setError('');

            // Re-fetch the active floor plan and its areas
            const response = await floorPlanAPI.getActive();
            setFloorPlan(response.data);
            setAreas(response.data.areas || []);

            // Clear any selected items
            setSelectedArea(null);
            setSelectedTable(null);
            setAddingTable(false);

            console.log('Floor plan refreshed successfully');

        } catch (err) {
            console.error('Error refreshing floor plan:', err);
            const errorMessage = err.response?.data?.error || 'Грешка при освежување на планот';
            setError(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    const loadActiveFloorPlan = async () => {
        try {
            setLoading(true);
            setError('');

            try {
                const response = await floorPlanAPI.getActive();
                setFloorPlan(response.data);
                setAreas(response.data.areas || []);
            } catch (err) {
                if (err.response?.status === 404) {
                    await createDefaultFloorPlan();
                } else {
                    throw err;
                }
            }
        } catch (err) {
            console.error('Error in loadActiveFloorPlan:', err);
            setError('Грешка при вчитување на планот: ' + (err.response?.data?.error || err.message));
        } finally {
            setLoading(false);
        }
    };

    const createDefaultFloorPlan = async () => {
        try {
            const response = await floorPlanAPI.createDefault();
            setFloorPlan(response.data);
            setAreas(response.data.areas || []);
            setError('');
        } catch (err) {
            console.error('Error creating default floor plan:', err);
            const errorMessage = err.response?.data?.error || 'Грешка при креирање на основен план';
            setError(errorMessage);
        }
    };

    const handleSaveArea = async (areaData, existingArea) => {
        try {
            setError('');

            if (existingArea) {
                const response = await areaAPI.update(existingArea.id, areaData);
                const updatedArea = response.data;
                setAreas(prev => prev.map(area =>
                    area.id === existingArea.id ? updatedArea : area
                ));
            } else {
                const response = await areaAPI.create(areaData);
                const area = response.data;
                setAreas(prev => [...prev, area]);
            }
            setShowAreaEditor(false);
            setSelectedArea(null);
        } catch (err) {
            const errorMessage = err.response?.data?.error || 'Грешка при зачувување на област';
            setError(errorMessage);
            throw new Error('Failed to save area');
        }
    };

    const handleSaveTable = async (tableData, existingTable) => {
        try {
            setError('');

            // Check if this is an existing table (has an ID) or a new table
            const isExistingTable = existingTable && existingTable.id;

            console.log('handleSaveTable called:', {
                isExistingTable,
                existingTable,
                tableData,
                selectedAreaId: selectedArea?.id
            });

            if (isExistingTable) {
                // Update existing table
                console.log('Updating existing table with ID:', existingTable.id, 'Data:', tableData);
                const response = await areaAPI.updateTable(existingTable.id, tableData);
                const updatedTable = response.data;

                // Update the table in the areas state
                setAreas(prev => prev.map(area => ({
                    ...area,
                    tables: area.tables ? area.tables.map(table =>
                        table.id === existingTable.id ? updatedTable : table
                    ) : []
                })));
            } else {
                // Add new table to area
                if (!selectedArea || !selectedArea.id) {
                    const errorMsg = 'Мора да избериш област пред да додадеш маса';
                    console.error(errorMsg, { selectedArea });
                    setError(errorMsg);
                    return;
                }

                console.log('Adding new table to area:', selectedArea.id, 'Table data:', tableData);

                // Validate table data before sending
                if (!tableData.tableNumber) {
                    setError('Бројот на маса е задолжителен');
                    return;
                }

                const response = await areaAPI.addTable(selectedArea.id, tableData);
                console.log('Server response:', response.data);
                const updatedArea = response.data;

                setAreas(prev => prev.map(area =>
                    area.id === selectedArea.id ? updatedArea : area
                ));
            }

            setShowTableEditor(false);
            setSelectedTable(null);
            setAddingTable(false);

            // Refresh the entire floor plan to ensure we have the latest data
            await refreshFloorPlan();

        } catch (err) {
            console.error('Error saving table:', err);
            console.error('Error response:', err.response?.data);
            console.error('Error status:', err.response?.status);

            let errorMessage = 'Грешка при зачувување на масата';
            if (err.response?.data?.error) {
                errorMessage = err.response.data.error;
            } else if (err.response?.data?.message) {
                errorMessage = err.response.data.message;
            } else if (err.message) {
                errorMessage = err.message;
            }

            setError(errorMessage);
        }
    };

    const deleteTable = async (tableId, areaId) => {
        if (!window.confirm('Дали сте сигурни дека сакате да ја избришете оваа маса?')) {
            return;
        }

        try {
            await areaAPI.deleteTable(tableId);

            // Remove table from areas state
            setAreas(prev => prev.map(area => ({
                ...area,
                tables: area.tables.filter(table => table.id !== tableId)
            })));

            setSelectedTable(null);
        } catch (err) {
            const errorMessage = err.response?.data?.error || 'Грешка при бришење на масата';
            setError(errorMessage);
        }
    };

    const updateItemPosition = async (item, newPosition, isTable = false) => {
        try {
            if (isTable) {
                await areaAPI.updateTablePosition(item.id, newPosition);
            } else {
                await areaAPI.updatePosition(item.id, newPosition);
            }
        } catch (err) {
            console.error('Error updating position:', err);
            setError('Грешка при ажурирање на позицијата');
        }
    };

    const deleteArea = async (areaId) => {
        const area = areas.find(a => a.id === areaId);
        const hasTablesMessage = area && area.tables && area.tables.length > 0
            ? ` (содржи ${area.tables.length} маси)`
            : '';

        if (!window.confirm(`Дали сте сигурни дека сакате да ја избришете областа "${area?.name || 'Непозната'}"${hasTablesMessage}?`)) {
            return;
        }

        try {
            setError('');

            // If area has tables, use force delete
            const force = area && area.tables && area.tables.length > 0;

            if (force) {
                console.log(`Force deleting area ${areaId} with ${area.tables.length} tables`);
            } else {
                console.log(`Deleting empty area ${areaId}`);
            }

            await areaAPI.delete(areaId, force);
            setAreas(prev => prev.filter(area => area.id !== areaId));
            setSelectedArea(null);
        } catch (err) {
            console.error('Error deleting area:', err);
            console.error('Error response:', err.response?.data);
            const errorMessage = err.response?.data?.error || 'Грешка при бришење на област';
            setError(errorMessage);
        }
    };

    const handleMouseDown = (e, item, isTable = false) => {
        if (!editMode) return;

        e.preventDefault();
        e.stopPropagation();

        const rect = e.currentTarget.getBoundingClientRect();

        setDraggedItem({ ...item, isTable });
        setDragOffset({
            x: e.clientX - rect.left,
            y: e.clientY - rect.top
        });
    };

    const handleMouseMove = (e) => {
        if (!draggedItem || !editMode) return;

        e.preventDefault();
        const containerRect = e.currentTarget.getBoundingClientRect();

        const newX = Math.max(0, e.clientX - containerRect.left - dragOffset.x);
        const newY = Math.max(0, e.clientY - containerRect.top - dragOffset.y);

        if (draggedItem.isTable) {
            // Update table position
            setAreas(prev => prev.map(area => ({
                ...area,
                tables: area.tables.map(table =>
                    table.id === draggedItem.id
                        ? { ...table, positionX: Math.round(newX), positionY: Math.round(newY) }
                        : table
                )
            })));
        } else {
            // Update area position
            setAreas(prev => prev.map(area =>
                area.id === draggedItem.id
                    ? { ...area, positionX: Math.round(newX), positionY: Math.round(newY) }
                    : area
            ));
        }
    };

    const handleMouseUp = () => {
        if (draggedItem && editMode) {
            let item;

            if (draggedItem.isTable) {
                // Find the updated table
                for (const area of areas) {
                    const table = area.tables.find(t => t.id === draggedItem.id);
                    if (table) {
                        item = table;
                        break;
                    }
                }
            } else {
                // Find the updated area
                item = areas.find(a => a.id === draggedItem.id);
            }

            if (item) {
                updateItemPosition(item, {
                    x: item.positionX,
                    y: item.positionY,
                    width: item.width,
                    height: item.height
                }, draggedItem.isTable);
            }
        }

        setDraggedItem(null);
        setDragOffset({ x: 0, y: 0 });
    };

    const handleCanvasClick = (e) => {
        // Only handle canvas clicks when in table adding mode
        if (!editMode || !addingTable || !selectedArea || !selectedArea.id) return;

        // Make sure this is a direct click on the canvas, not a child element
        if (e.target !== e.currentTarget) return;

        const rect = e.currentTarget.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;

        console.log('Canvas clicked at:', x, y);
        console.log('Selected area:', selectedArea);

        // Check if click is within the selected area bounds
        if (x < selectedArea.positionX ||
            y < selectedArea.positionY ||
            x > selectedArea.positionX + selectedArea.width ||
            y > selectedArea.positionY + selectedArea.height) {
            setError('Кликнете внатре во избраната област за да додадете маса');
            return;
        }

        // Calculate position relative to the selected area
        const relativeX = Math.max(10, x - selectedArea.positionX);
        const relativeY = Math.max(10, y - selectedArea.positionY);

        // Create a new table object (without ID, indicating it's new)
        const newTable = {
            // No ID property - this indicates it's a new table
            positionX: Math.round(relativeX),
            positionY: Math.round(relativeY),
            width: 80,
            height: 80,
            capacity: 4,
            shape: 'RECTANGLE',
            tableNumber: getNextTableNumber(),
            status: 'AVAILABLE',
            isNew: true // Flag to indicate this is a new table
        };

        console.log('Creating new table:', newTable);
        setSelectedTable(newTable);
        setShowTableEditor(true);
    };

    const getNextTableNumber = () => {
        const allTables = areas.flatMap(area => area.tables || []);
        if (allTables.length === 0) return 1;

        const maxTableNumber = Math.max(...allTables.map(table => table.tableNumber || 0));
        return maxTableNumber + 1;
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
                    {floorPlan && (
                        <>
                            <button
                                onClick={() => setEditMode(!editMode)}
                                className={`flex items-center gap-2 px-4 py-2 rounded-lg transition-colors ${editMode ? 'bg-green-500 text-white' : 'bg-gray-200'
                                    }`}
                            >
                                <Edit3 size={20} />
                                {editMode ? 'Заврши уредување' : 'Уреди план'}
                            </button>
                            <button
                                onClick={() => setShowAreaEditor(true)}
                                className="flex items-center gap-2 px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors"
                            >
                                <Plus size={20} />
                                Додај област
                            </button>
                        </>
                    )}
                    <button
                        onClick={refreshFloorPlan}
                        className="flex items-center gap-2 px-4 py-2 bg-gray-500 text-white rounded-lg hover:bg-gray-600 transition-colors"
                        disabled={loading}
                    >
                        🔄 Освежи
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

            {/* Edit Mode Tools */}
            {editMode && floorPlan && (
                <div className="mb-4 p-4 bg-yellow-100 border border-yellow-400 rounded">
                    <div className="flex items-center gap-4 mb-2">
                        <strong>Режим на уредување:</strong>
                        <select
                            value={selectedArea?.id || ''}
                            onChange={(e) => {
                                const areaId = e.target.value;
                                if (areaId === '') {
                                    setSelectedArea(null);
                                } else {
                                    const area = areas.find(a => a.id === parseInt(areaId));
                                    console.log('Selected area:', area); // Debug log
                                    setSelectedArea(area);
                                }
                                setAddingTable(false);
                            }}
                            className="px-3 py-1 border rounded"
                        >
                            <option value="">Избери област за додавање маси</option>
                            {areas.map(area => (
                                <option key={area.id} value={area.id}>{area.name}</option>
                            ))}
                        </select>

                        {selectedArea && (
                            <button
                                onClick={() => setAddingTable(!addingTable)}
                                className={`px-4 py-1 rounded transition-colors ${addingTable
                                        ? 'bg-red-500 text-white'
                                        : 'bg-green-500 text-white hover:bg-green-600'
                                    }`}
                            >
                                {addingTable ? 'Откажи додавање' : 'Додај маса'}
                            </button>
                        )}
                    </div>
                    <p className="text-sm">
                        {addingTable
                            ? 'Кликнете во областа за да додадете маса'
                            : 'Повлечете ги областите и масите за да ги преместите. Кликнете за уредување.'
                        }
                    </p>
                </div>
            )}

            {/* Floor Plan Canvas */}
            {floorPlan && (
                <div
                    className={`relative bg-gray-50 border-2 border-gray-300 rounded-lg overflow-hidden ${addingTable ? 'cursor-crosshair' : ''
                        }`}
                    style={{ width: '100%', height: '600px', minHeight: '600px' }}
                    onMouseMove={handleMouseMove}
                    onMouseUp={handleMouseUp}
                    onMouseLeave={handleMouseUp}
                    onClick={handleCanvasClick}
                >
                    {/* Render Areas */}
                    {areas.map(area => {
                        const areaType = areaTypes[area.type] || areaTypes.DINING;
                        const Icon = areaType.icon;

                        return (
                            <div key={`area-${area.id}`}>
                                {/* Area Container */}
                                <div
                                    className={`absolute border-2 rounded-lg flex flex-col items-center justify-center transition-all ${editMode && !addingTable ? 'cursor-move hover:shadow-lg' : ''
                                        } ${selectedArea?.id === area.id ? 'ring-4 ring-blue-400' : ''}`}
                                    style={{
                                        left: `${area.positionX}px`,
                                        top: `${area.positionY}px`,
                                        width: `${area.width}px`,
                                        height: `${area.height}px`,
                                        backgroundColor: `${area.color}20`,
                                        borderColor: area.color,
                                        borderStyle: editMode && draggedItem?.id === area.id && !draggedItem.isTable ? 'dashed' : 'solid',
                                        zIndex: 1,
                                        pointerEvents: addingTable && selectedArea?.id === area.id ? 'none' : 'auto' // Disable pointer events when adding table to this area
                                    }}
                                    onMouseDown={(e) => {
                                        if (!addingTable) {
                                            handleMouseDown(e, area, false);
                                        }
                                    }}
                                    onClick={(e) => {
                                        e.stopPropagation();
                                        if (editMode && !addingTable) {
                                            setSelectedArea(area);
                                            setShowAreaEditor(true);
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
                                                    setSelectedArea(area);
                                                    setShowAreaEditor(true);
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

                                {/* Render Tables in Area */}
                                {area.tables && area.tables.map(table => (
                                    <div
                                        key={`table-${table.id}`}
                                        className={`absolute border-2 border-gray-600 rounded flex items-center justify-center font-bold text-white transition-all ${editMode ? 'cursor-move hover:shadow-lg' : 'cursor-pointer hover:bg-opacity-80'
                                            }`}
                                        style={{
                                            left: `${area.positionX + (table.positionX || 0)}px`,
                                            top: `${area.positionY + (table.positionY || 0)}px`,
                                            width: `${table.width || 80}px`,
                                            height: `${table.height || 80}px`,
                                            backgroundColor: table.status === 'OCCUPIED' ? '#ef4444' :
                                                table.status === 'RESERVED' ? '#f59e0b' : '#10b981',
                                            borderRadius: table.shape === 'CIRCLE' ? '50%' :
                                                table.shape === 'SQUARE' ? '8px' : '12px',
                                            borderStyle: editMode && draggedItem?.id === table.id && draggedItem.isTable ? 'dashed' : 'solid',
                                            zIndex: 10
                                        }}
                                        onMouseDown={(e) => {
                                            if (!addingTable) {
                                                handleMouseDown(e, table, true);
                                            }
                                        }}
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            if (editMode && !addingTable) {
                                                // For existing tables, pass the full table object
                                                setSelectedTable(table);
                                                setShowTableEditor(true);
                                            }
                                            // TODO: Open order screen when not in edit mode
                                        }}
                                    >
                                        <span className="text-sm">{table.tableNumber}</span>

                                        {editMode && (
                                            <button
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    deleteTable(table.id, area.id);
                                                }}
                                                className="absolute -top-2 -right-2 w-5 h-5 bg-red-500 text-white rounded-full flex items-center justify-center hover:bg-red-600 text-xs"
                                            >
                                                ×
                                            </button>
                                        )}
                                    </div>
                                ))}
                            </div>
                        );
                    })}

                    {/* Empty State */}
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
            )}

            {/* Statistics */}
            {floorPlan && (
                <div className="mt-6 grid grid-cols-1 md:grid-cols-4 gap-4">
                    <div className="bg-white rounded-lg shadow p-4">
                        <h3 className="font-bold text-gray-800 mb-2">Области</h3>
                        <p className="text-2xl font-bold text-blue-600">{areas.length}</p>
                    </div>
                    <div className="bg-white rounded-lg shadow p-4">
                        <h3 className="font-bold text-gray-800 mb-2">Маси</h3>
                        <p className="text-2xl font-bold text-green-600">
                            {areas.reduce((total, area) => total + (area.tables ? area.tables.length : 0), 0)}
                        </p>
                    </div>
                    <div className="bg-white rounded-lg shadow p-4">
                        <h3 className="font-bold text-gray-800 mb-2">Капацитет</h3>
                        <p className="text-2xl font-bold text-purple-600">
                            {areas.reduce((total, area) =>
                                total + (area.tables ? area.tables.reduce((sum, table) => sum + (table.capacity || 0), 0) : 0), 0
                            )}
                        </p>
                    </div>
                    <div className="bg-white rounded-lg shadow p-4">
                        <h3 className="font-bold text-gray-800 mb-2">Достапни</h3>
                        <p className="text-2xl font-bold text-emerald-600">
                            {areas.reduce((total, area) =>
                                total + (area.tables ? area.tables.filter(table => table.status === 'AVAILABLE').length : 0), 0
                            )}
                        </p>
                    </div>
                </div>
            )}

            {/* Area Editor Modal */}
            <AreaEditor
                isOpen={showAreaEditor}
                onClose={() => {
                    setShowAreaEditor(false);
                    setSelectedArea(null);
                }}
                selectedArea={selectedArea}
                onSave={handleSaveArea}
                floorPlanId={floorPlan?.id}
            />

            {/* Table Editor Modal */}
            <TableEditor
                isOpen={showTableEditor}
                onClose={() => {
                    setShowTableEditor(false);
                    setSelectedTable(null);
                    setAddingTable(false);
                }}
                selectedTable={selectedTable}
                onSave={handleSaveTable}
                nextTableNumber={getNextTableNumber()}
                tableShapes={tableShapes}
            />
        </div>
    );
};

export default FloorPlanManager;