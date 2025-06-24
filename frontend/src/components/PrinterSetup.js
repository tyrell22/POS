import React, { useState, useEffect, useRef } from 'react';
import { Printer, Settings, Wifi, Usb, Bluetooth, CheckCircle, XCircle, RefreshCw } from 'lucide-react';

const PrinterSetup = ({ onBack, adminCode }) => {
    const [config, setConfig] = useState({
        fiscalEnabled: true,
        fiscalPort: 'COM1',
        fiscalType: 'SYNERGY',
        thermalEnabled: true,
        thermalName: 'Epson TM-T20II',
        thermalConnection: 'USB',
        thermalIP: '192.168.1.100',
        thermalPort: 9100
    });

    const [testResults, setTestResults] = useState(null);
    const [availablePorts, setAvailablePorts] = useState(['COM1', 'COM2', 'COM3', 'COM4']);
    const [availablePrinters, setAvailablePrinters] = useState(['Epson TM-T20II', 'Epson TM-T88V', 'Generic ESC/POS Printer']);
    const [fiscalTypes, setFiscalTypes] = useState([
        { value: 'SYNERGY', label: 'Synergy PF-500/PF-550' },
        { value: 'EXPERT_SX', label: 'David Expert SX' },
        { value: 'FIDITEK', label: 'Fiditek Expert SX' },
        { value: 'GENERIC', label: 'Generic Macedonian Fiscal Printer' }
    ]);
    const [connectionTypes, setConnectionTypes] = useState([
        { value: 'USB', label: 'USB Connection' },
        { value: 'NETWORK', label: 'Network/Ethernet Connection' },
        { value: 'BLUETOOTH', label: 'Bluetooth Connection' }
    ]);

    const [loading, setLoading] = useState(false);
    const [testing, setTesting] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    // Use ref to prevent multiple API calls
    const hasLoaded = useRef(false);

    // Load data when component mounts and adminCode is available
    useEffect(() => {
        if (adminCode && adminCode.trim() && !hasLoaded.current) {
            hasLoaded.current = true;
            loadInitialData();
        } else if (!adminCode || !adminCode.trim()) {
            setError('Потребен е валиден админ код за пристап до принтер конфигурацијата');
        }
    }, [adminCode]);

    const loadInitialData = async () => {
        console.log('Loading initial printer data...');
        setLoading(true);

        try {
            // Load configuration
            await loadPrinterConfiguration();

            // Load available options in parallel
            await Promise.all([
                loadAvailablePorts(),
                loadAvailablePrinters(),
                loadStaticData()
            ]);
        } catch (err) {
            console.error('Error loading initial data:', err);
        } finally {
            setLoading(false);
        }
    };

    const loadPrinterConfiguration = async () => {
        try {
            console.log('Fetching printer configuration...');
            const response = await fetch('/api/printer/config', {
                headers: { 'Admin-Code': adminCode }
            });

            if (response.ok) {
                const data = await response.json();
                setConfig(data);
                console.log('✅ Printer configuration loaded');
            } else {
                console.log('❌ Failed to load printer configuration:', response.status);
            }
        } catch (err) {
            console.error('Error loading printer configuration:', err);
        }
    };

    const loadAvailablePorts = async () => {
        try {
            const response = await fetch('/api/printer/ports', {
                headers: { 'Admin-Code': adminCode }
            });
            if (response.ok) {
                const data = await response.json();
                setAvailablePorts(data.ports || ['COM1', 'COM2', 'COM3', 'COM4']);
                console.log('✅ COM ports loaded');
            }
        } catch (err) {
            console.error('Error loading COM ports:', err);
        }
    };

    const loadAvailablePrinters = async () => {
        try {
            const response = await fetch('/api/printer/thermal-printers', {
                headers: { 'Admin-Code': adminCode }
            });
            if (response.ok) {
                const data = await response.json();
                setAvailablePrinters(data.printers || ['Epson TM-T20II', 'Epson TM-T88V', 'Generic ESC/POS Printer']);
                console.log('✅ Thermal printers loaded');
            }
        } catch (err) {
            console.error('Error loading thermal printers:', err);
        }
    };

    const loadStaticData = async () => {
        try {
            // Load fiscal types
            const fiscalResponse = await fetch('/api/printer/fiscal-types');
            if (fiscalResponse.ok) {
                const fiscalData = await fiscalResponse.json();
                setFiscalTypes(fiscalData.fiscalTypes || fiscalTypes);
            }

            // Load connection types
            const connectionResponse = await fetch('/api/printer/connection-types');
            if (connectionResponse.ok) {
                const connectionData = await connectionResponse.json();
                setConnectionTypes(connectionData.connectionTypes || connectionTypes);
            }

            console.log('✅ Static data loaded');
        } catch (err) {
            console.error('Error loading static data:', err);
        }
    };

    const handleConfigChange = (field, value) => {
        setConfig(prev => ({ ...prev, [field]: value }));
        setError('');
        setSuccess('');
    };

    const testPrinterConnections = async () => {
        if (!adminCode) {
            setError('Админ код е потребен за тестирање');
            return;
        }

        try {
            setTesting(true);
            setError('');

            const response = await fetch('/api/printer/test', {
                method: 'POST',
                headers: { 'Admin-Code': adminCode }
            });

            if (response.ok) {
                const results = await response.json();
                setTestResults(results);
                console.log('✅ Printer test completed:', results);
            } else if (response.status === 401) {
                setError('Неавторизиран пристап - проверете го админ кодот');
            } else {
                const errorData = await response.json();
                setError(errorData.error || 'Грешка при тестирање на принтерите');
            }
        } catch (err) {
            console.error('Error testing printers:', err);
            setError('Грешка при поврзување со серверот');
        } finally {
            setTesting(false);
        }
    };

    const saveConfiguration = async () => {
        if (!adminCode) {
            setError('Админ код е потребен за зачувување');
            return;
        }

        try {
            setLoading(true);
            setError('');
            setSuccess('');

            const response = await fetch('/api/printer/config', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Admin-Code': adminCode
                },
                body: JSON.stringify(config)
            });

            if (response.ok) {
                setSuccess('Конфигурацијата е зачувана успешно!');
                console.log('✅ Configuration saved successfully');
                // Auto-test after save
                setTimeout(testPrinterConnections, 500);
            } else if (response.status === 401) {
                setError('Неавторизиран пристап - проверете го админ кодот');
            } else {
                const errorData = await response.json();
                setError(errorData.error || 'Грешка при зачувување на конфигурацијата');
            }
        } catch (err) {
            console.error('Error saving configuration:', err);
            setError('Грешка при поврзување со серверот');
        } finally {
            setLoading(false);
        }
    };

    const initializePrinters = async () => {
        if (!adminCode) {
            setError('Админ код е потребен за иницијализација');
            return;
        }

        try {
            setLoading(true);
            setError('');

            const response = await fetch('/api/printer/initialize', {
                method: 'POST',
                headers: { 'Admin-Code': adminCode }
            });

            if (response.ok) {
                setSuccess('Принтерите се иницијализирани успешно!');
                console.log('✅ Printers initialized successfully');
                setTimeout(testPrinterConnections, 500);
            } else if (response.status === 401) {
                setError('Неавторизиран пристап - проверете го админ кодот');
            } else {
                const errorData = await response.json();
                setError(errorData.error || 'Грешка при иницијализација на принтерите');
            }
        } catch (err) {
            console.error('Error initializing printers:', err);
            setError('Грешка при поврзување со серверот');
        } finally {
            setLoading(false);
        }
    };

    const getConnectionIcon = (type) => {
        switch (type) {
            case 'USB': return <Usb size={16} />;
            case 'NETWORK': return <Wifi size={16} />;
            case 'BLUETOOTH': return <Bluetooth size={16} />;
            default: return <Settings size={16} />;
        }
    };

    // Show loading message if no admin code
    if (!adminCode) {
        return (
            <div className="p-6">
                <div className="flex justify-between items-center mb-6">
                    <div>
                        <h1 className="text-3xl font-bold text-gray-800">Конфигурација на Принтери</h1>
                        <p className="text-gray-600">Подесување на фискални и термални принтери</p>
                    </div>
                    <button
                        onClick={onBack}
                        className="flex items-center gap-2 px-4 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors"
                    >
                        Назад
                    </button>
                </div>

                <div className="bg-yellow-100 border border-yellow-400 text-yellow-700 p-4 rounded">
                    <strong>⚠️ Потребна е авторизација</strong>
                    <br />
                    Потребен е валиден админ код за пристап до принтер конфигурацијата.
                </div>
            </div>
        );
    }

    return (
        <div className="p-6">
            {/* Header */}
            <div className="flex justify-between items-center mb-6">
                <div>
                    <h1 className="text-3xl font-bold text-gray-800">Конфигурација на Принтери</h1>
                    <p className="text-gray-600">Подесување на фискални и термални принтери</p>
                </div>
                <div className="flex gap-2">
                    <button
                        onClick={testPrinterConnections}
                        className="flex items-center gap-2 px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors"
                        disabled={testing || loading}
                    >
                        <RefreshCw size={20} className={testing ? 'animate-spin' : ''} />
                        {testing ? 'Тестирам...' : 'Тестирај'}
                    </button>
                    <button
                        onClick={onBack}
                        className="flex items-center gap-2 px-4 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors"
                    >
                        Назад
                    </button>
                </div>
            </div>

            {/* Loading indicator */}
            {loading && (
                <div className="mb-4 p-4 bg-blue-100 border border-blue-400 text-blue-700 rounded">
                    <div className="flex items-center gap-2">
                        <RefreshCw size={16} className="animate-spin" />
                        Се вчитува конфигурацијата...
                    </div>
                </div>
            )}

            {/* Error/Success Messages */}
            {error && (
                <div className="mb-4 p-4 bg-red-100 border border-red-400 text-red-700 rounded">
                    {error}
                    <button onClick={() => setError('')} className="ml-2 font-bold">×</button>
                </div>
            )}

            {success && (
                <div className="mb-4 p-4 bg-green-100 border border-green-400 text-green-700 rounded">
                    {success}
                    <button onClick={() => setSuccess('')} className="ml-2 font-bold">×</button>
                </div>
            )}

            {/* Test Results */}
            {testResults && (
                <div className="mb-6 bg-white rounded-lg shadow-md p-6">
                    <h2 className="text-xl font-bold mb-4">Статус на Принтери</h2>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        {/* Fiscal Printer Status */}
                        <div className="border rounded-lg p-4">
                            <div className="flex items-center gap-2 mb-2">
                                <Printer size={20} />
                                <h3 className="font-bold">Фискален Принтер</h3>
                                {testResults.fiscal?.connected ? (
                                    <CheckCircle size={20} className="text-green-500" />
                                ) : (
                                    <XCircle size={20} className="text-red-500" />
                                )}
                            </div>
                            <p className={`text-sm ${testResults.fiscal?.connected ? 'text-green-600' : 'text-red-600'}`}>
                                {testResults.fiscal?.status}
                            </p>
                            {testResults.fiscal?.port && (
                                <p className="text-xs text-gray-500">
                                    {testResults.fiscal.type} на {testResults.fiscal.port}
                                </p>
                            )}
                        </div>

                        {/* Thermal Printer Status */}
                        <div className="border rounded-lg p-4">
                            <div className="flex items-center gap-2 mb-2">
                                <Printer size={20} />
                                <h3 className="font-bold">Термален Принтер</h3>
                                {testResults.thermal?.connected ? (
                                    <CheckCircle size={20} className="text-green-500" />
                                ) : (
                                    <XCircle size={20} className="text-red-500" />
                                )}
                            </div>
                            <p className={`text-sm ${testResults.thermal?.connected ? 'text-green-600' : 'text-red-600'}`}>
                                {testResults.thermal?.status}
                            </p>
                            {testResults.thermal?.name && (
                                <p className="text-xs text-gray-500">
                                    {testResults.thermal.name} - {testResults.thermal.connection}
                                    {testResults.thermal.ip && ` (${testResults.thermal.ip}:${testResults.thermal.port})`}
                                </p>
                            )}
                        </div>
                    </div>
                </div>
            )}

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Fiscal Printer Configuration */}
                <div className="bg-white rounded-lg shadow-md p-6">
                    <div className="flex items-center gap-2 mb-4">
                        <Printer size={24} />
                        <h2 className="text-xl font-bold">Фискален Принтер</h2>
                    </div>

                    <div className="space-y-4">
                        {/* Enable/Disable */}
                        <div className="flex items-center gap-2">
                            <input
                                type="checkbox"
                                id="fiscalEnabled"
                                checked={config.fiscalEnabled}
                                onChange={(e) => handleConfigChange('fiscalEnabled', e.target.checked)}
                                className="w-4 h-4"
                            />
                            <label htmlFor="fiscalEnabled" className="font-medium">
                                Овозможи фискален принтер
                            </label>
                        </div>

                        {config.fiscalEnabled && (
                            <>
                                {/* Fiscal Printer Type */}
                                <div>
                                    <label className="block text-sm font-medium mb-1">Тип на принтер:</label>
                                    <select
                                        value={config.fiscalType}
                                        onChange={(e) => handleConfigChange('fiscalType', e.target.value)}
                                        className="w-full p-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    >
                                        {fiscalTypes.map(type => (
                                            <option key={type.value} value={type.value}>
                                                {type.label}
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                {/* COM Port */}
                                <div>
                                    <label className="block text-sm font-medium mb-1">COM Порта:</label>
                                    <select
                                        value={config.fiscalPort}
                                        onChange={(e) => handleConfigChange('fiscalPort', e.target.value)}
                                        className="w-full p-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    >
                                        {availablePorts.map(port => (
                                            <option key={port} value={port}>{port}</option>
                                        ))}
                                    </select>
                                </div>
                            </>
                        )}
                    </div>
                </div>

                {/* Thermal Printer Configuration */}
                <div className="bg-white rounded-lg shadow-md p-6">
                    <div className="flex items-center gap-2 mb-4">
                        <Printer size={24} />
                        <h2 className="text-xl font-bold">Термален Принтер</h2>
                    </div>

                    <div className="space-y-4">
                        {/* Enable/Disable */}
                        <div className="flex items-center gap-2">
                            <input
                                type="checkbox"
                                id="thermalEnabled"
                                checked={config.thermalEnabled}
                                onChange={(e) => handleConfigChange('thermalEnabled', e.target.checked)}
                                className="w-4 h-4"
                            />
                            <label htmlFor="thermalEnabled" className="font-medium">
                                Овозможи термален принтер
                            </label>
                        </div>

                        {config.thermalEnabled && (
                            <>
                                {/* Printer Name */}
                                <div>
                                    <label className="block text-sm font-medium mb-1">Име на принтер:</label>
                                    <select
                                        value={config.thermalName}
                                        onChange={(e) => handleConfigChange('thermalName', e.target.value)}
                                        className="w-full p-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    >
                                        {availablePrinters.map(printer => (
                                            <option key={printer} value={printer}>{printer}</option>
                                        ))}
                                    </select>
                                </div>

                                {/* Connection Type */}
                                <div>
                                    <label className="block text-sm font-medium mb-1">Тип на врска:</label>
                                    <select
                                        value={config.thermalConnection}
                                        onChange={(e) => handleConfigChange('thermalConnection', e.target.value)}
                                        className="w-full p-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                                    >
                                        {connectionTypes.map(type => (
                                            <option key={type.value} value={type.value}>
                                                {type.label}
                                            </option>
                                        ))}
                                    </select>
                                </div>

                                {/* Network Settings */}
                                {config.thermalConnection === 'NETWORK' && (
                                    <>
                                        <div>
                                            <label className="block text-sm font-medium mb-1">IP Адреса:</label>
                                            <input
                                                type="text"
                                                value={config.thermalIP}
                                                onChange={(e) => handleConfigChange('thermalIP', e.target.value)}
                                                className="w-full p-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                                                placeholder="192.168.1.100"
                                            />
                                        </div>
                                        <div>
                                            <label className="block text-sm font-medium mb-1">Порта:</label>
                                            <input
                                                type="number"
                                                value={config.thermalPort}
                                                onChange={(e) => handleConfigChange('thermalPort', parseInt(e.target.value) || 9100)}
                                                className="w-full p-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                                                placeholder="9100"
                                                min="1024"
                                                max="65535"
                                            />
                                        </div>
                                    </>
                                )}
                            </>
                        )}
                    </div>
                </div>
            </div>

            {/* Action Buttons */}
            <div className="mt-6 flex gap-4 justify-center">
                <button
                    onClick={saveConfiguration}
                    className="flex items-center gap-2 px-6 py-3 bg-green-500 text-white rounded-lg hover:bg-green-600 transition-colors disabled:opacity-50"
                    disabled={loading}
                >
                    <Settings size={20} />
                    {loading ? 'Зачувувам...' : 'Зачувај Конфигурација'}
                </button>

                <button
                    onClick={initializePrinters}
                    className="flex items-center gap-2 px-6 py-3 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors disabled:opacity-50"
                    disabled={loading}
                >
                    <RefreshCw size={20} />
                    Иницијализирај Принтери
                </button>
            </div>

            {/* Instructions */}
            <div className="mt-8 bg-blue-50 border border-blue-200 rounded-lg p-6">
                <h3 className="font-bold text-blue-800 mb-2">Упатства:</h3>
                <ul className="text-sm text-blue-700 space-y-1">
                    <li>• <strong>Фискален принтер:</strong> Се користи за печатење на фискални сметки (задржете F при затворање на нарачка)</li>
                    <li>• <strong>Термален принтер:</strong> Се користи за печатење на билети за кујна/бар и обични сметки</li>
                    <li>• За да работи фискалниот принтер, проверете дека е правилно поврзан на COM портата</li>
                    <li>• За мрежни термални принтери, проверете дека IP адресата и портата се точни</li>
                    <li>• Кликнете "Тестирај" за да проверите дали принтерите се правилно конфигурирани</li>
                </ul>
            </div>
        </div>
    );
};

export default PrinterSetup;