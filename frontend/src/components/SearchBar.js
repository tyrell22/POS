import React, { useState, useEffect, useRef, useCallback } from 'react';
import { Search, XCircle } from 'lucide-react';
import { menuAPI } from '../services/api';

const SearchBar = ({ onSearchResults }) => {
    const [searchQuery, setSearchQuery] = useState('');
    const [isSearching, setIsSearching] = useState(false);
    const [resultCount, setResultCount] = useState(0);
    const [error, setError] = useState('');
    const searchInputRef = useRef(null);
    const debounceRef = useRef(null);

    // Debounced search effect with enhanced debugging
    useEffect(() => {
        console.log('🔍 Search effect triggered. Query:', searchQuery);

        // Clear previous timeout
        if (debounceRef.current) {
            clearTimeout(debounceRef.current);
        }

        // Don't search for empty or very short queries
        if (!searchQuery || searchQuery.trim().length < 1) {
            console.log('🔍 Query too short, clearing results');
            onSearchResults(null); // null indicates no active search
            setIsSearching(false);
            setResultCount(0);
            setError('');
            return;
        }

        const searchProducts = async (query) => {
            try {
                console.log('🔍 Starting search for:', query);
                setIsSearching(true);
                setError('');

                // Direct API call with debugging
                console.log('🔍 Calling menuAPI.search...');
                const response = await menuAPI.search(query.trim());

                console.log('🔍 Raw API response:', response);
                console.log('🔍 Response data:', response.data);
                console.log('🔍 Response data type:', typeof response.data);
                console.log('🔍 Response data is array:', Array.isArray(response.data));

                const results = response.data || [];

                console.log('🔍 Processed results:', results);
                console.log('🔍 Results count:', results.length);

                // Ensure onSearchResults is called properly
                if (typeof onSearchResults === 'function') {
                    console.log('🔍 Calling onSearchResults with:', results);
                    onSearchResults(results);
                } else {
                    console.error('🔍 onSearchResults is not a function:', onSearchResults);
                }

                setResultCount(results.length);
                setIsSearching(false);

                if (results.length === 0) {
                    console.log('🔍 No results found for query:', query);
                } else {
                    console.log('🔍 Found results:', results.map(r => r.name));
                }

            } catch (err) {
                console.error('🔍 Search API error:', err);
                console.error('🔍 Error response:', err.response);
                console.error('🔍 Error response data:', err.response?.data);

                setError('Грешка при пребарување');
                if (typeof onSearchResults === 'function') {
                    onSearchResults([]); // Empty array indicates search with no results
                }
                setResultCount(0);
                setIsSearching(false);
            }
        };

        debounceRef.current = setTimeout(() => {
            console.log('🔍 Debounce timeout triggered, searching for:', searchQuery);
            searchProducts(searchQuery);
        }, 300); // 300ms debounce delay

        return () => {
            if (debounceRef.current) {
                clearTimeout(debounceRef.current);
            }
        };
    }, [searchQuery]); // Only depend on searchQuery - onSearchResults should be stable

    // Handle input change
    const handleSearchChange = useCallback((e) => {
        const value = e.target.value;
        console.log('🔍 Input changed to:', value);
        setSearchQuery(value);
    }, []);

    // Clear search function
    const clearSearch = useCallback(() => {
        console.log('🔍 Clearing search');
        setSearchQuery('');
        if (typeof onSearchResults === 'function') {
            onSearchResults(null); // null indicates no active search
        }
        setIsSearching(false);
        setResultCount(0);
        setError('');

        // Keep focus on search input after clearing
        setTimeout(() => {
            if (searchInputRef.current) {
                searchInputRef.current.focus();
            }
        }, 0);
    }, []); // Remove onSearchResults dependency

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

            {error && (
                <div className="mt-2 text-sm text-red-600">
                    {error}
                </div>
            )}

            {searchQuery.trim().length >= 2 && !isSearching && !error && (
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

            {/* Debug info - remove this in production */}
            {process.env.NODE_ENV === 'development' && (
                <div className="mt-2 text-xs text-gray-400 bg-gray-50 p-2 rounded">
                    Debug: Query="{searchQuery}" | Searching={isSearching.toString()} | Count={resultCount} | onSearchResults={typeof onSearchResults}
                </div>
            )}
        </div>
    );
};

export default SearchBar;