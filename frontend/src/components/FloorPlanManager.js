import React, { useState, useEffect } from 'react';
import {
    Grid, Plus, Trash2, Home, Users, Square, Eye, Settings, Palette, MapPin, Edit2
} from 'lucide-react';
import { floorPlanAPI, areaAPI, tableAPI } from '../services/api';

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
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [editingTable, setEditingTable] = useState(null);
    const [newTableNumber, setNewTableNumber] = useState('');

    useEffect(() => {
        loadActiveFloorPlan();
    }, []);

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

    const refreshFloorPlan = async () => {
        try {
            setLoading(true);
            setError('');

            const response = await floorPlanAPI.getActive();
            setFloorPlan(response.data);
            setAreas(response.data.areas || []);

            console.log('Floor plan refreshed successfully');

        } catch (err) {
            console.error('Error refreshing floor plan:', err);
            const errorMessage = err.response?.data?.error || 'Грешка при освежување на планот';
            setError(errorMessage);
        } finally {
            setLoading(false);
        }
    };

    const addTableToArea = async (areaId) => {
        try {
            setError('');

            // With the backend fix, we can now simply use the next available number
            // The backend will handle finding the first available number properly

            // Let the backend determine the next available table number
            const tableData = {
                tableNumber: 1, // Backend will automatically find the next available number
                capacity: 4,
                shape: 'RECTANGLE',
                status: 'AVAILABLE'
            };

            console.log('Adding table to area:', areaId, 'Table data:', tableData);

            const response = await areaAPI.addTable(areaId, tableData);
            const updatedArea = response.data;

            setAreas(prev => prev.map(area =>
                area.id === areaId ? updatedArea : area
            ));

            await refreshFloorPlan();

        } catch (err) {
            console.error('Error adding table:', err);
            const errorMessage = err.response?.data?.error || 'Грешка при додавање на масата';
            setError(errorMessage);
        }
    };

    const deleteTable = async (tableId, areaId) => {
        if (!window.confirm('Дали сте сигурни дека сакате да ја избришете оваа маса?')) {
            return;
        }

        try {
            setError('');

            // Use soft delete to allow table number reuse
            await areaAPI.deleteTable(tableId);

            // Remove table from areas state and reorganize
            setAreas(prev => prev.map(area => ({
                ...area,
                tables: area.tables ? area.tables.filter(table => table.id !== tableId) : []
            })));

            // Refresh to get reorganized layout
            await refreshFloorPlan();

            console.log(`Successfully deleted table ${tableId}`);

        } catch (err) {
            console.error('Error deleting table:', err);
            const errorMessage = err.response?.data?.error || 'Грешка при бришење на масата';
            setError(errorMessage);
        }
    };

    const startEditingTableNumber = (table) => {
        setEditingTable(table.id);
        setNewTableNumber(table.tableNumber.toString());
    };

    const cancelEditingTableNumber = () => {
        setEditingTable(null);
        setNewTableNumber('');
    };

    const saveTableNumber = async (tableId) => {
        try {
            setError('');

            // Validate the new table number
            const tableNum = parseInt(newTableNumber);
            if (!tableNum || tableNum < 1) {
                setError('Бројот на маса мора да биде позитивен број');
                return;
            }

            // Check if table number already exists (excluding current table)
            const allTables = areas.flatMap(area => area.tables || []);
            const currentTable = allTables.find(t => t.id === tableId);

            if (!currentTable) {
                setError('Масата не е пронајдена');
                setEditingTable(null);
                setNewTableNumber('');
                return;
            }

            // If the number hasn't changed, just exit
            if (currentTable.tableNumber === tableNum) {
                setEditingTable(null);
                setNewTableNumber('');
                return;
            }

            const existingTable = allTables.find(t => t.tableNumber === tableNum && t.id !== tableId);
            if (existingTable) {
                setError(`Масата со број ${tableNum} веќе постои`);
                setEditingTable(null);
                setNewTableNumber('');
                return;
            }

            console.log(`Updating table ${tableId} from number ${currentTable.tableNumber} to ${tableNum}`);

            // Use the existing tableAPI.update method
            const response = await tableAPI.update(tableId, {
                tableNumber: tableNum,
                capacity: currentTable.capacity,
                shape: currentTable.shape || 'RECTANGLE',
                status: currentTable.status || 'AVAILABLE'
            });

            console.log('Table update response:', response.data);

            // Update local state with the response data
            setAreas(prev => prev.map(area => ({
                ...area,
                tables: area.tables ? area.tables.map(table =>
                    table.id === tableId ? { ...table, tableNumber: tableNum } : table
                ) : []
            })));

            // Clear editing state
            setEditingTable(null);
            setNewTableNumber('');

            console.log(`Successfully updated table ${tableId} to number ${tableNum}`);

        } catch (err) {
            console.error('Error updating table number:', err);

            // Handle specific error cases with better messages
            if (err.response?.status === 404) {
                setError('Масата не е пронајдена. Обновете ја страницата.');
                // Refresh the floor plan to sync data
                await refreshFloorPlan();
            } else if (err.response?.data?.error?.includes('Unique') ||
                err.response?.data?.error?.includes('table_number')) {
                setError(`Бројот на маса ${newTableNumber} веќе се користи`);
            } else if (err.response?.data?.error) {
                setError(err.response.data.error);
            } else {
                setError('Грешка при ажурирање на бројот на масата');
            }

            // Always reset editing state on error
            setEditingTable(null);
            setNewTableNumber('');
        }
    };

    const TableGrid = ({ area }) => {
        const tables = area.tables || [];

        return (
            <div className="bg-white rounded-lg shadow-md p-6">
                <div className="flex justify-between items-center mb-4">
                    <div className="flex items-center gap-2">
                        {React.createElement(areaTypes[area.type]?.icon || Users, {
                            size: 20,
                            style: { color: area.color }
                        })}
                        <h3 className="text-lg font-bold" style={{ color: area.color }}>
                            {area.name}
                        </h3>
                        <span className="text-sm text-gray-600">
                            ({tables.length} маси)
                        </span>
                    </div>
                    <button
                        onClick={() => addTableToArea(area.id)}
                        className="flex items-center gap-1 px-3 py-1 bg-green-500 text-white rounded hover:bg-green-600 transition-colors text-sm"
                    >
                        <Plus size={16} />
                        Додај маса
                    </button>
                </div>

                {/* Simple 2-column grid */}
                <div className="grid grid-cols-2 gap-4">
                    {tables.map((table, index) => (
                        <div
                            key={table.id}
                            className="relative border-2 border-gray-300 rounded-lg p-4 bg-gray-50 hover:bg-gray-100 transition-colors"
                        >
                            <div className="text-center">
                                {/* Editable table number */}
                                <div className="font-bold text-lg text-gray-800 mb-1 flex items-center justify-center gap-2">
                                    {editingTable === table.id ? (
                                        <div className="flex items-center gap-2">
                                            <span className="text-sm">Маса</span>
                                            <input
                                                type="number"
                                                value={newTableNumber}
                                                onChange={(e) => setNewTableNumber(e.target.value)}
                                                className="w-16 px-2 py-1 text-center border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                                                min="1"
                                                autoFocus
                                                onKeyDown={(e) => {
                                                    if (e.key === 'Enter') {
                                                        saveTableNumber(table.id);
                                                    } else if (e.key === 'Escape') {
                                                        cancelEditingTableNumber();
                                                    }
                                                }}
                                                onBlur={() => saveTableNumber(table.id)}
                                            />
                                        </div>
                                    ) : (
                                        <div
                                            className="cursor-pointer hover:text-blue-600 flex items-center gap-1"
                                            onClick={() => startEditingTableNumber(table)}
                                            title="Кликни за да го промениш бројот"
                                        >
                                            Маса {table.tableNumber}
                                            <Edit2 size={14} className="opacity-50" />
                                        </div>
                                    )}
                                </div>
                                <div className="text-sm text-gray-600 mb-2">
                                    {table.capacity} места
                                </div>
                                <div className={`inline-block px-2 py-1 rounded text-xs font-medium ${table.status === 'AVAILABLE' ? 'bg-green-100 text-green-700' :
                                        table.status === 'OCCUPIED' ? 'bg-red-100 text-red-700' :
                                            table.status === 'RESERVED' ? 'bg-yellow-100 text-yellow-700' :
                                                'bg-gray-100 text-gray-700'
                                    }`}>
                                    {table.status === 'AVAILABLE' ? 'Достапна' :
                                        table.status === 'OCCUPIED' ? 'Зафатена' :
                                            table.status === 'RESERVED' ? 'Резервирана' : table.status}
                                </div>
                            </div>

                            <button
                                onClick={() => deleteTable(table.id, area.id)}
                                className="absolute top-2 right-2 w-6 h-6 bg-red-500 text-white rounded-full flex items-center justify-center hover:bg-red-600 text-xs"
                            >
                                ×
                            </button>
                        </div>
                    ))}

                    {/* Empty state for area with no tables */}
                    {tables.length === 0 && (
                        <div className="col-span-2 text-center py-8 text-gray-500">
                            <Grid size={48} className="mx-auto mb-4 opacity-50" />
                            <p>Нема маси во оваа област</p>
                            <p className="text-sm">Кликнете "Додај маса" за да додадете</p>
                        </div>
                    )}
                </div>

                {/* Instructions */}
                {tables.length > 0 && (
                    <div className="mt-4 p-3 bg-blue-50 border border-blue-200 rounded text-sm text-blue-700">
                        💡 <strong>Совет:</strong> Кликнете на бројот на масата за да го промените
                    </div>
                )}
            </div>
        );
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

            {/* Areas with Table Grids */}
            {floorPlan && (
                <div className="space-y-6">
                    {areas.map(area => (
                        <TableGrid key={area.id} area={area} />
                    ))}

                    {/* Empty State */}
                    {areas.length === 0 && (
                        <div className="text-center py-12 text-gray-500">
                            <Grid size={64} className="mx-auto mb-4 opacity-50" />
                            <p className="text-xl mb-2">Нема области</p>
                            <p>Планот нема конфигурирани области</p>
                        </div>
                    )}
                </div>
            )}

            {/* Statistics */}
            {floorPlan && (
                <div className="mt-8 grid grid-cols-1 md:grid-cols-4 gap-4">
                    <div className="bg-white rounded-lg shadow p-4">
                        <h3 className="font-bold text-gray-800 mb-2">Области</h3>
                        <p className="text-2xl font-bold text-blue-600">{areas.length}</p>
                    </div>
                    <div className="bg-white rounded-lg shadow p-4">
                        <h3 className="font-bold text-gray-800 mb-2">Вкупно Маси</h3>
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
        </div>
    );
};

export default FloorPlanManager;