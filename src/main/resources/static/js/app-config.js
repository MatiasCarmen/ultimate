/**
 * Configuración global de la aplicación 
 * Centraliza URLs, configuraciones de entorno y parámetros configurables
 */

// Detectar entorno basado en hostname o variable global
const getEnvironment = () => {
    const hostname = window.location.hostname;

    if (hostname === 'localhost' || hostname === '127.0.0.1') {
        return 'development';
    } else if (hostname.includes('staging') || hostname.includes('test')) {
        return 'staging';
    } else {
        return 'production';
    }
};

// Configuraciones por entorno
const configs = {
    development: {
        API_BASE_URL: 'http://localhost:8081/api',
        WS_URL: 'ws://localhost:8081/ws/notifications',
        NOTIFICATION_SOUND_URL: '/sounds/notification.wav',
        DEBUG_MODE: true,
        RETRY_ATTEMPTS: 5,
        REQUEST_TIMEOUT: 10000
    },
    staging: {
        API_BASE_URL: 'https://staging-api.vcsystems.com/api',
        WS_URL: 'wss://staging-api.vcsystems.com/ws/notifications',
        NOTIFICATION_SOUND_URL: '/sounds/notification.wav',
        DEBUG_MODE: true,
        RETRY_ATTEMPTS: 3,
        REQUEST_TIMEOUT: 15000
    },
    production: {
        API_BASE_URL: 'https://api.vcsystems.com/api',
        WS_URL: 'wss://api.vcsystems.com/ws/notifications',
        NOTIFICATION_SOUND_URL: '/sounds/notification.wav',
        DEBUG_MODE: false,
        RETRY_ATTEMPTS: 3,
        REQUEST_TIMEOUT: 20000
    }
};

// Configuración activa basada en el entorno
const currentEnvironment = getEnvironment();
const config = configs[currentEnvironment];

// Permite override desde variables globales o meta tags
const getConfigValue = (key, defaultValue) => {
    // 1. Verificar variable global
    if (window.ENV_CONFIG && window.ENV_CONFIG[key]) {
        return window.ENV_CONFIG[key];
    }

    // 2. Verificar meta tag
    const metaTag = document.querySelector(`meta[name="app-${key.toLowerCase()}"]`);
    if (metaTag) {
        const value = metaTag.getAttribute('content');
        // Convertir strings a tipos apropiados
        if (value === 'true') return true;
        if (value === 'false') return false;
        if (!isNaN(value)) return Number(value);
        return value;
    }

    // 3. Usar valor por defecto de configuración
    return defaultValue;
};

// Configuración final exportada
window.APP_CONFIG = {
    ENVIRONMENT: currentEnvironment,
    API_BASE_URL: getConfigValue('API_BASE_URL', config.API_BASE_URL),
    WS_URL: getConfigValue('WS_URL', config.WS_URL),
    NOTIFICATION_SOUND_URL: getConfigValue('NOTIFICATION_SOUND_URL', config.NOTIFICATION_SOUND_URL),
    DEBUG_MODE: getConfigValue('DEBUG_MODE', config.DEBUG_MODE),
    RETRY_ATTEMPTS: getConfigValue('RETRY_ATTEMPTS', config.RETRY_ATTEMPTS),
    REQUEST_TIMEOUT: getConfigValue('REQUEST_TIMEOUT', config.REQUEST_TIMEOUT),

    // Configuraciones adicionales
    TOAST_DEFAULT_DURATION: 3000,
    TOAST_ERROR_DURATION: 5000,
    MAX_NOTIFICATIONS: 50,
    RECONNECT_DELAY: 3000,
    MAX_RECONNECT_DELAY: 30000
};

// Logging de configuración (solo en desarrollo)
if (window.APP_CONFIG.DEBUG_MODE) {
    console.log('🔧 Configuración cargada:', {
        environment: window.APP_CONFIG.ENVIRONMENT,
        apiUrl: window.APP_CONFIG.API_BASE_URL,
        wsUrl: window.APP_CONFIG.WS_URL,
        debugMode: window.APP_CONFIG.DEBUG_MODE
    });
}

// Exportar para uso como módulo ES6 si está disponible
if (typeof module !== 'undefined' && module.exports) {
    module.exports = window.APP_CONFIG;
}
