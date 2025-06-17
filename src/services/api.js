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

    // Update item quantity
    updateQuantity: (orderId, itemId, quantity) =>
        api.patch(`/orders/${orderId}/items/${itemId}/quantity`, { quantity }),

    // Send order to kitchen/bar
    send: (orderId) => api.post(`/orders/${orderId}/send`),

    // Close order and print receipt
    close: (orderId) => api.post(`/orders/${orderId}/close`),
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
    addTable: (areaId, tableData) => api.post(`/floor-plans/areas/${areaId}/tables`, tableData)
};

// Admin API
export const adminAPI = {
    // Admin login
    login: (adminCode) => api.post('/admin/login', { adminCode }),
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
        return num >= 1 && num <= 12;
    },

    // Check if API is available
    checkHealth: async () => {
        try {
            await api.get('/menu-items/available');
            return true;
        } catch (error) {
            return false;
        }
    }
};

export default api;