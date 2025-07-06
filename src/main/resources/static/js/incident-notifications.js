// Manejo de notificaciones en tiempo real con WebSocket
class IncidentNotificationManager {
    constructor(config = {}) {
        // Configuración externalizada
        this.config = {
            wsUrl: config.wsUrl || this.getWebSocketUrl(),
            reconnectAttempts: config.reconnectAttempts || 5,
            reconnectDelay: config.reconnectDelay || 3000,
            maxReconnectDelay: config.maxReconnectDelay || 30000,
            notificationSoundUrl: config.notificationSoundUrl || null, // URL externa para sonido
            maxNotifications: config.maxNotifications || 50,
            ...config
        };

        this.socket = null;
        this.reconnectAttempts = 0;
        this.reconnectDelay = this.config.reconnectDelay;
        this.notifications = [];
        this.isConnected = false;
        this.toastManager = new ToastManager(); // Usar módulo común de tostadas
        this.notificationSound = null;

        this.init();
    }

    //  Obtener URL del WebSocket desde configuración o variables de entorno
    getWebSocketUrl() {
        // Prioridad: 1) Variable de entorno, 2) Meta tag, 3) Configuración por defecto
        const envUrl = window.APP_CONFIG?.WS_URL;
        const metaTag = document.querySelector('meta[name="ws-url"]');
        const metaUrl = metaTag?.getAttribute('content');

        return envUrl || metaUrl || `ws://${window.location.hostname}:8081/ws/notifications`;
    }

    //  Cargar sonido de notificación desde archivo externo
    async loadNotificationSound() {
        if (this.config.notificationSoundUrl) {
            try {
                this.notificationSound = new Audio(this.config.notificationSoundUrl);
                this.notificationSound.preload = 'auto';
                console.log('Sonido de notificación cargado desde:', this.config.notificationSoundUrl);
            } catch (error) {
                console.warn('No se pudo cargar el sonido de notificación:', error);
                this.createDefaultNotificationSound();
            }
        } else {
            this.createDefaultNotificationSound();
        }
    }

    //Crear sonido de notificación por defecto (data URL como fallback)
    createDefaultNotificationSound() {
        const dataUrl = 'data:audio/wav;base64,UklGRnoGAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQoGAACBhYqFbF1fdJivrJBhNjVgodDbq2EcBj+a2/LDciUFLIHO8tiJNwgZaLvt559NEAxQp+PwtmMcBjiR1/LMeSwFJHfH8N2QQAoUXrTp66hVFApGn+DyvmwhCjs2hqmMhYU...'; // Sonido corto y simple
        try {
            this.notificationSound = new Audio(dataUrl);
        } catch (error) {
            console.warn('No se pudo crear sonido de notificación por defecto:', error);
        }
    }

    // Inicializar conexión WebSocket
    async init() {
        if (!securityManager.isAuthenticated()) {
            console.log('Usuario no autenticado, no se iniciará WebSocket');
            return;
        }

        await this.loadNotificationSound();
        this.connect();
        this.setupEventListeners();
    }

    // Conectar al WebSocket - MEJORADO
    connect() {
        try {
            const wsUrl = this.config.wsUrl;
            console.log('Conectando WebSocket a:', wsUrl);

            this.socket = new WebSocket(wsUrl);

            this.socket.onopen = (event) => {
                console.log('WebSocket conectado exitosamente');
                this.isConnected = true;
                this.reconnectAttempts = 0;
                this.reconnectDelay = this.config.reconnectDelay; // Resetear delay
                this.updateConnectionStatus(true);
                this.toastManager.success('Conectado al sistema de notificaciones', 3000);
            };

            this.socket.onmessage = (event) => {
                try {
                    const notification = JSON.parse(event.data);
                    this.handleNotification(notification);
                } catch (error) {
                    console.error('Error procesando notificación:', error);
                    this.toastManager.warning('Error procesando notificación recibida', 3000);
                }
            };

            this.socket.onclose = (event) => {
                console.log('WebSocket desconectado:', event.code, event.reason);
                this.isConnected = false;
                this.updateConnectionStatus(false);

                if (!event.wasClean && this.reconnectAttempts < this.config.reconnectAttempts) {
                    this.scheduleReconnect();
                } else if (this.reconnectAttempts >= this.config.reconnectAttempts) {
                    this.toastManager.error('No se pudo mantener conexión con notificaciones', 5000);
                }
            };

            this.socket.onerror = (error) => {
                console.error('Error en WebSocket:', error);
                this.isConnected = false;
                this.updateConnectionStatus(false);
            };

        } catch (error) {
            console.error('Error conectando WebSocket:', error);
            this.toastManager.error('Error al conectar notificaciones', 4000);
            this.scheduleReconnect();
        }
    }

    // Programar reconexión - MEJORADO ya no da erroricito sii
    scheduleReconnect() {
        if (this.reconnectAttempts >= this.config.reconnectAttempts) {
            this.toastManager.warning(
                `Sin conexión a notificaciones tras ${this.config.reconnectAttempts} intentos`,
                0, // No auto-remover
                { title: 'Sistema offline' }
            );
            return;
        }

        this.reconnectAttempts++;
        console.log(`Reintentando conexión en ${this.reconnectDelay}ms (intento ${this.reconnectAttempts}/${this.config.reconnectAttempts})`);

        setTimeout(() => {
            if (!this.isConnected) {
                this.connect();
            }
        }, this.reconnectDelay);

        // Incrementar delay exponencialmente
        this.reconnectDelay = Math.min(this.reconnectDelay * 1.5, this.config.maxReconnectDelay);
    }

    // Manejar notificación recibida - MEJORADO
    handleNotification(notification) {
        console.log('Notificación recibida:', notification);

        // Validar estructura de notificación
        if (!notification || typeof notification !== 'object') {
            console.warn('Notificación inválida recibida:', notification);
            return;
        }

        // Agregar timestamp y estado
        const processedNotification = {
            id: notification.id || Date.now(),
            title: notification.title || 'Nueva notificación',
            message: notification.message || '',
            type: notification.type || 'info',
            timestamp: new Date(),
            read: false,
            ...notification
        };

        // Agregar a lista de notificaciones
        this.notifications.unshift(processedNotification);

        // Limitar número de notificaciones almacenadas
        if (this.notifications.length > this.config.maxNotifications) {
            this.notifications = this.notifications.slice(0, this.config.maxNotifications);
        }

        // Mostrar notificación visual
        this.showNotificationToast(processedNotification);

        // Reproducir sonido si está habilitado
        this.playNotificationSound();

        // Actualizar contador en UI
        this.updateNotificationBadge();

        // Disparar evento personalizado
        this.dispatchNotificationEvent(processedNotification);
    }

    // Mostrar notificación usando el módulo común de tostadas
    showNotificationToast(notification) {
        const options = {
            title: notification.title
        };

        switch (notification.type) {
            case 'incident':
                this.toastManager.warning(notification.message, 5000, options);
                break;
            case 'success':
                this.toastManager.success(notification.message, 4000, options);
                break;
            case 'error':
                this.toastManager.error(notification.message, 6000, options);
                break;
            default:
                this.toastManager.info(notification.message, 4000, options);
        }
    }

    //  Reproducir sonido de notificación
    playNotificationSound() {
        if (this.notificationSound && this.isNotificationSoundEnabled()) {
            try {
                this.notificationSound.currentTime = 0; // Reiniciar sonido
                this.notificationSound.play().catch(error => {
                    console.warn('No se pudo reproducir sonido de notificación:', error);
                });
            } catch (error) {
                console.warn('Error reproduciendo sonido:', error);
            }
        }
    }

    // NUEVO: Verificar si el sonido está habilitado
    isNotificationSoundEnabled() {
        return localStorage.getItem('notification-sound') !== 'false';
    }

    // NUEVO: Alternar sonido de notificaciones
    toggleNotificationSound() {
        const enabled = this.isNotificationSoundEnabled();
        localStorage.setItem('notification-sound', !enabled);

        const message = enabled ? 'Sonido de notificaciones deshabilitado' : 'Sonido de notificaciones habilitado';
        this.toastManager.info(message, 2000);

        return !enabled;
    }

    // Actualizar estado de conexión en UI
    updateConnectionStatus(connected) {
        const statusElements = document.querySelectorAll('.connection-status');

        statusElements.forEach(element => {
            if (connected) {
                element.innerHTML = '<i class="fas fa-wifi text-success"></i> Conectado';
                element.className = 'connection-status text-success';
            } else {
                element.innerHTML = '<i class="fas fa-wifi text-danger"></i> Desconectado';
                element.className = 'connection-status text-danger';
            }
        });
    }

    // Configurar event listeners
    setupEventListeners() {
        // Reconectar cuando la ventana recupere el foco
        window.addEventListener('focus', () => {
            if (!this.isConnected && securityManager.isAuthenticated()) {
                this.connect();
            }
        });

        // Cerrar conexión cuando se cierre la ventana
        window.addEventListener('beforeunload', () => {
            this.disconnect();
        });

        // Manejar cambios de visibilidad
        document.addEventListener('visibilitychange', () => {
            if (document.visibilityState === 'visible' && !this.isConnected) {
                if (securityManager.isAuthenticated()) {
                    this.connect();
                }
            }
        });
    }

    // Desconectar WebSocket
    disconnect() {
        if (this.socket && this.socket.readyState === WebSocket.OPEN) {
            this.socket.close();
        }
        this.isConnected = false;
    }

    // Marcar notificación como leída
    markAsRead(notificationId) {
        const notification = this.notifications.find(n => n.id === notificationId);
        if (notification) {
            notification.read = true;
            this.updateNotificationBadge();
        }
    }

    // Marcar todas como leídas
    markAllAsRead() {
        this.notifications.forEach(n => n.read = true);
        this.updateNotificationBadge();
    }

    // Obtener notificaciones no leídas
    getUnreadNotifications() {
        return this.notifications.filter(n => !n.read);
    }

    // Obtener todas las notificaciones
    getAllNotifications() {
        return this.notifications;
    }

    // Limpiar notificaciones
    clearNotifications() {
        this.notifications = [];
        this.updateNotificationBadge();
    }

    // Enviar mensaje al servidor (si es necesario)
    sendMessage(message) {
        if (this.socket && this.socket.readyState === WebSocket.OPEN) {
            this.socket.send(JSON.stringify(message));
        }
    }
}

// Instancia global del manager de notificaciones
let notificationManager = null;

// Inicializar cuando el DOM esté listo
document.addEventListener('DOMContentLoaded', function() {
    // Solo inicializar si el usuario está autenticado
    if (securityManager.isAuthenticated()) {
        notificationManager = new IncidentNotificationManager();
        window.notificationManager = notificationManager;
    }
});

// Reinicializar si el usuario se autentica
document.addEventListener('userAuthenticated', function() {
    if (!notificationManager) {
        notificationManager = new IncidentNotificationManager();
        window.notificationManager = notificationManager;
    }
});

// Exportar para uso global
window.IncidentNotificationManager = IncidentNotificationManager;
