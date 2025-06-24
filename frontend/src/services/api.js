import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

const api = axios.create({
    baseURL: API_BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Request interceptor for logging
api.interceptors.request.use(
    (config) => {
        console.log(`API Request: ${config.method?.toUpperCase()} ${config.url}`);
        return config;
    },
    (error) => {
        console.error('API Request Error:', error);
        return Promise.reject(error);
    }
);

// Response interceptor for error handling
api.interceptors.response.use(
    (response) => {
        console.log(`API Response: ${response.status} ${response.config.url}`);
        return response;
    },
    (error) => {
        console.error('API Response Error:', error.response?.data || error.message);

        // Handle common errors
        if (error.response?.status === 404) {
            console.error('Resource not found');
        } else if (error.response?.status === 500) {
            console.error('Server error');
        } else if (error.response?.status === 401) {
            console.error('Unauthorized');
        }

        return Promise.reject(error);
    }
);

// Menu Items API
export const menuAPI = {
    // Get all menu items (for admin)
    getAll: () => api.get('/menu-items'),

    // Get only available menu items (for ordering)
    getAvailable: () => api.get('/menu-items/available'),

    // Get menu item by ID
    getById: (id) => api.get(`/menu-items/${id}`),

    // Get menu items by category
    getByCategory: (category) => api.get(`/menu-items/category/${category}`),

    // Create new menu item
    create: (menuItem) => api.post('/menu-items', menuItem),

    // Update existing menu item
    update: (id, menuItem) => api.put(`/menu-items/${id}`, menuItem),

    // Delete menu item
    delete: (id) => api.delete(`/menu-items/${id}`),

    // Toggle item availability
    toggleAvailability: (id) => api.patch(`/menu-items/${id}/toggle-availability`),

    // Search menu items
    search: (query) => api.get(`/menu-items/search?q=${encodeURIComponent(query)}`),
};

// Orders API
export const orderAPI = {
    // Get all active orders
    getAll: () => api.get('/orders'),

    // Get order by ID
    getById: (id) => api.get(`/orders/${id}`),

    // Get orders by status
    getByStatus: (status) => api.get(`/orders/status/${status}`),

    // Get or create order for table
    getOrCreateForTable: (tableNumber) => api.get(`/orders/table/${tableNumber}`),

    // Add item to order
    addItem: (orderId, item) => api.post(`/orders/${orderId}/items`, item),

    // Remove item from order
    removeItem: (orderId, itemId) => api.delete(`/orders/${orderId}/items/${itemId}`),

    // Remove item from order with admin code
    removeItemAdmin: (orderId, itemId, adminCode) =>
        api.delete(`/orders/${orderId}/items/${itemId}/admin`, {
            data: { adminCode: adminCode }
        }),

    // Move order to different table
    moveToTable: (orderId, newTableNumber) => api.post(`/orders/${orderId}/move`, {
        newTableNumber: newTableNumber
    }),

    // Update item quantity
    updateQuantity: (orderId, itemId, quantity) =>
        api.patch(`/orders/${orderId}/items/${itemId}/quantity`, { quantity }),

    // Send order to kitchen/bar
    send: (orderId) => api.post(`/orders/${orderId}/send`),

    // Close order with thermal printer (default)
    close: (orderId) => api.post(`/orders/${orderId}/close`),

    // NEW: Close order with fiscal printer
    closeFiscal: (orderId) => api.post(`/orders/${orderId}/close-fiscal`),
};

// Floor Plan API
export const floorPlanAPI = {
    // Get all floor plans
    getAll: () => api.get('/floor-plans'),

    // Get active floor plan
    getActive: () => api.get('/floor-plans/active'),

    // Get floor plan by ID
    getById: (id) => api.get(`/floor-plans/${id}`),

    // Create new floor plan
    create: (floorPlan) => api.post('/floor-plans', floorPlan),

    // Update floor plan
    update: (id, floorPlan) => api.put(`/floor-plans/${id}`, floorPlan),

    // Delete floor plan
    delete: (id) => api.delete(`/floor-plans/${id}`),

    // Activate floor plan
    activate: (id) => api.post(`/floor-plans/${id}/activate`),

    // Create default floor plan
    createDefault: () => api.post('/floor-plans/default'),

    // Get areas for floor plan
    getAreas: (floorPlanId) => api.get(`/floor-plans/${floorPlanId}/areas`),

    // Get area types
    getAreaTypes: () => api.get('/floor-plans/area-types'),

    // Get table shapes
    getTableShapes: () => api.get('/floor-plans/table-shapes'),

    // Get all tables with orders
    getAllTables: () => api.get('/floor-plans/tables'),

    // Get table by number
    getTableByNumber: (tableNumber) => api.get(`/floor-plans/tables/${tableNumber}`)
};

// Area API
export const areaAPI = {
    // Create new area
    create: (area) => api.post('/floor-plans/areas', area),

    // Update area
    update: (id, area) => api.put(`/floor-plans/areas/${id}`, area),

    // Update area position
    updatePosition: (id, position) => api.patch(`/floor-plans/areas/${id}/position`, position),

    // Delete area
    delete: (id, force = false) => api.delete(`/floor-plans/areas/${id}?force=${force}`),

    // Get area by ID
    getById: (id) => api.get(`/floor-plans/areas/${id}`),

    // Add table to area
    addTable: (areaId, tableData) => api.post(`/floor-plans/areas/${areaId}/tables`, tableData),

    // Update table using the correct TableController endpoint
    updateTable: (tableId, tableData) => api.put(`/tables/${tableId}`, tableData),

    // Update table position using the correct TableController endpoint
    updateTablePosition: (tableId, position) => api.patch(`/tables/${tableId}/position`, position),

    // Delete table using the correct TableController endpoint
    deleteTable: (tableId) => api.delete(`/tables/${tableId}`)
};

// Table API
export const tableAPI = {
    // Get all tables
    getAll: () => api.get('/tables'),

    // Get table by ID
    getById: (id) => api.get(`/tables/${id}`),

    // Get table by number
    getByNumber: (tableNumber) => api.get(`/tables/number/${tableNumber}`),

    // Update table
    update: (id, tableData) => api.put(`/tables/${id}`, tableData),

    // Update table position
    updatePosition: (id, position) => api.patch(`/tables/${id}/position`, position),

    // Update table status
    updateStatus: (id, status) => api.patch(`/tables/${id}/status`, { status }),

    // Delete table
    delete: (id) => api.delete(`/tables/${id}`)
};

// Admin API
export const adminAPI = {
    // Admin login
    login: (adminCode) => api.post('/admin/login', { adminCode }),
};

// NEW: Printer API
export const printerAPI = {
    // Get printer configuration
    getConfig: (adminCode) => api.get('/printer/config', {
        headers: { 'Admin-Code': adminCode }
    }),

    // Update printer configuration
    updateConfig: (config, adminCode) => api.put('/printer/config', config, {
        headers: { 'Admin-Code': adminCode }
    }),

    // Test printer connections
    testConnections: (adminCode) => api.post('/printer/test', {}, {
        headers: { 'Admin-Code': adminCode }
    }),

    // Get available COM ports
    getAvailablePorts: (adminCode) => api.get('/printer/ports', {
        headers: { 'Admin-Code': adminCode }
    }),

    // Get available thermal printers
    getAvailablePrinters: (adminCode) => api.get('/printer/thermal-printers', {
        headers: { 'Admin-Code': adminCode }
    }),

    // Initialize printers
    initializePrinters: (adminCode) => api.post('/printer/initialize', {}, {
        headers: { 'Admin-Code': adminCode }
    }),

    // Get supported fiscal printer types
    getFiscalTypes: () => api.get('/printer/fiscal-types'),

    // Get supported connection types
    getConnectionTypes: () => api.get('/printer/connection-types')
};

// Debug API (for troubleshooting)
export const debugAPI = {
    // Health check
    health: () => api.get('/debug/health'),

    // Database cleanup
    cleanup: () => api.post('/debug/cleanup'),

    // Floor plan info
    floorPlanInfo: () => api.get('/debug/floor-plans')
};

// Utility functions
export const apiUtils = {
    // Handle API errors with user-friendly messages
    handleError: (error) => {
        if (error.response) {
            // Server responded with error status
            const status = error.response.status;
            const data = error.response.data;

            switch (status) {
                case 400:
                    return data.error || 'Неважечки податоци';
                case 401:
                    return 'Неавторизиран пристап';
                case 403:
                    return 'Забранет пристап';
                case 404:
                    return 'Ресурсот не е пронајден';
                case 500:
                    return 'Грешка на серверот';
                default:
                    return data.error || `Грешка: ${status}`;
            }
        } else if (error.request) {
            // Network error
            return 'Грешка при поврзување со серверот';
        } else {
            // Other error
            return error.message || 'Неочекувана грешка';
        }
    },

    // Format currency
    formatCurrency: (amount) => {
        return `${parseFloat(amount).toFixed(2)} ден`;
    },

    // Format date
    formatDate: (dateString) => {
        const date = new Date(dateString);
        return date.toLocaleDateString('mk-MK', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });
    },

    // Validate table number
    isValidTableNumber: (tableNumber) => {
        const num = parseInt(tableNumber);
        return num >= 1 && num <= 100; // Increased limit for more tables
    },

    // Check if API is available
    checkHealth: async () => {
        try {
            await debugAPI.health();
            return true;
        } catch (error) {
            return false;
        }
    },

    // Get table status color
    getTableStatusColor: (status) => {
        switch (status) {
            case 'AVAILABLE':
                return '#10b981'; // Green
            case 'OCCUPIED':
                return '#ef4444'; // Red
            case 'RESERVED':
                return '#f59e0b'; // Orange
            case 'CLEANING':
                return '#6b7280'; // Gray
            case 'OUT_OF_ORDER':
                return '#374151'; // Dark gray
            default:
                return '#10b981'; // Default green
        }
    },

    // Get area type color
    getAreaTypeColor: (type) => {
        const colors = {
            'DINING': '#3B82F6',
            'BAR': '#8B5CF6',
            'TERRACE': '#10B981',
            'VIP': '#F59E0B',
            'PRIVATE_ROOM': '#EF4444',
            'OUTDOOR': '#059669',
            'SMOKING': '#6B7280',
            'NON_SMOKING': '#14B8A6'
        };
        return colors[type] || '#3B82F6';
    }
};

export default api;