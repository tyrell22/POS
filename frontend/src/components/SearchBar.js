import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Search, XCircle } from 'lucide-react';
import { menuAPI } from '../services/api';

const SearchBar = ({ onSearchResults }) => {
    const [searchQuery, setSearchQuery] = useState('');
    const [isSearching, setIsSearching] = useState(false);
    const [resultCount, setResultCount] = useState(0);
    const searchInputRef = useRef(null);
    const debounceRef = useRef(null);

    // Debounced search effect - FIXED: Proper dependency management
    useEffect(() => {
        // Clear previous timeout
        if (debounceRef.current) {
            clearTimeout(debounceRef.current);
        }

        // Don't search for empty or very short queries
        if (!searchQuery || searchQuery.trim().length < 2) {
            onSearchResults(null); // null indicates no active search
            setIsSearching(false);
            setResultCount(0);
            return;
        }

        const searchProducts = async (query) => {
            try {
                setIsSearching(true);

                const response = await menuAPI.search(query.trim());
                const results = response.data || [];

                onSearchResults(results); // Pass actual results array
                setResultCount(results.length);
                setIsSearching(false);
            } catch (err) {
                console.error('Error searching products:', err);
                onSearchResults([]); // Empty array indicates search with no results
                setResultCount(0);
                setIsSearching(false);
            }
        };

        debounceRef.current = setTimeout(() => {
            searchProducts(searchQuery);
        }, 300); // 300ms debounce delay

        return () => {
            if (debounceRef.current) {
                clearTimeout(debounceRef.current);
            }
        };
    }, [searchQuery]); // Only depend on searchQuery, not onSearchResults

    // Handle input change
    const handleSearchChange = useCallback((e) => {
        const value = e.target.value;
        setSearchQuery(value);
    }, []);

    // Clear search function
    const clearSearch = useCallback(() => {
        setSearchQuery('');
        onSearchResults(null); // null indicates no active search
        setIsSearching(false);
        setResultCount(0);

        // Keep focus on search input after clearing
        setTimeout(() => {
            if (searchInputRef.current) {
                searchInputRef.current.focus();
            }
        }, 0);
    }, []); // Remove onSearchResults from dependencies

    // Handle key events
    const handleKeyDown = useCallback((e) => {
        if (e.key === 'Escape') {
            clearSearch();
        }
    }, [clearSearch]);

    return (
        <div className="mb-6">
            <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <Search className="h-5 w-5 text-gray-400" />
                </div>
                <input
                    ref={searchInputRef}
                    type="text"
                    placeholder="Пребарајте производи..."
                    value={searchQuery}
                    onChange={handleSearchChange}
                    onKeyDown={handleKeyDown}
                    className="w-full pl-10 pr-10 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    autoComplete="off"
                    spellCheck="false"
                />
                {searchQuery && (
                    <button
                        onClick={clearSearch}
                        className="absolute inset-y-0 right-0 pr-3 flex items-center"
                        type="button"
                        tabIndex={-1}
                    >
                        <XCircle className="h-5 w-5 text-gray-400 hover:text-gray-600 transition-colors" />
                    </button>
                )}
            </div>

            {/* Search Status */}
            {isSearching && (
                <div className="mt-2 text-sm text-gray-500 flex items-center gap-2">
                    <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-blue-500"></div>
                    Пребарување...
                </div>
            )}

            {searchQuery.trim().length >= 2 && !isSearching && (
                <div className="mt-2 text-sm text-gray-600">
                    {resultCount > 0
                        ? `Пронајдени ${resultCount} резултати за "${searchQuery}"`
                        : `Нема резултати за "${searchQuery}"`
                    }
                </div>
            )}

            {searchQuery.trim().length > 0 && searchQuery.trim().length < 2 && (
                <div className="mt-2 text-sm text-gray-500">
                    Внесете најмалку 2 карактери за пребарување
                </div>
            )}
        </div>
    );
};

export default SearchBar;