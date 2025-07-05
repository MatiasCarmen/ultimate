// Manejo de notificaciones en tiempo real con WebSocket
class IncidentNotificationManager {
    constructor() {
        this.socket = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectDelay = 3000;
        this.notifications = [];
        this.isConnected = false;

        this.init();
    }

    // Inicializar conexión WebSocket
    init() {
        if (!securityManager.isAuthenticated()) {
            console.log('Usuario no autenticado, no se iniciará WebSocket');
            return;
        }

        this.connect();
        this.setupEventListeners();
    }

    // Conectar al WebSocket
    connect() {
        try {
            const wsUrl = `ws://localhost:8081/ws/notifications?token=${securityManager.getToken()}`;
            this.socket = new WebSocket(wsUrl);

            this.socket.onopen = (event) => {
                console.log('WebSocket conectado');
                this.isConnected = true;
                this.reconnectAttempts = 0;
                this.updateConnectionStatus(true);
                this.showToast('Conectado al sistema de notificaciones', 'success');
            };

            this.socket.onmessage = (event) => {
                try {
                    const notification = JSON.parse(event.data);
                    this.handleNotification(notification);
                } catch (error) {
                    console.error('Error procesando notificación:', error);
                }
            };

            this.socket.onclose = (event) => {
                console.log('WebSocket desconectado:', event.code, event.reason);
                this.isConnected = false;
                this.updateConnectionStatus(false);

                if (!event.wasClean && this.reconnectAttempts < this.maxReconnectAttempts) {
                    this.scheduleReconnect();
                }
            };

            this.socket.onerror = (error) => {
                console.error('Error en WebSocket:', error);
                this.isConnected = false;
                this.updateConnectionStatus(false);
            };

        } catch (error) {
            console.error('Error conectando WebSocket:', error);
            this.scheduleReconnect();
        }
    }

    // Programar reconexión
    scheduleReconnect() {
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            this.showToast('No se pudo conectar al sistema de notificaciones', 'warning');
            return;
        }

        this.reconnectAttempts++;
        console.log(`Intentando reconectar en ${this.reconnectDelay}ms (intento ${this.reconnectAttempts})`);

        setTimeout(() => {
            if (!this.isConnected) {
                this.connect();
            }
        }, this.reconnectDelay);

        // Incrementar delay para próximos intentos
        this.reconnectDelay = Math.min(this.reconnectDelay * 1.5, 30000);
    }

    // Manejar notificación recibida
    handleNotification(notification) {
        console.log('Notificación recibida:', notification);

        // Agregar a lista de notificaciones
        this.notifications.unshift({
            ...notification,
            timestamp: new Date(),
            read: false
        });

        // Limitar número de notificaciones almacenadas
        if (this.notifications.length > 50) {
            this.notifications = this.notifications.slice(0, 50);
        }

        // Mostrar notificación según tipo
        this.displayNotification(notification);

        // Actualizar badge de notificaciones
        this.updateNotificationBadge();

        // Actualizar tabla si es relevante
        this.updateRelevantTable(notification);

        // Reproducir sonido si está habilitado
        this.playNotificationSound();
    }

    // Mostrar notificación visual
    displayNotification(notification) {
        const { tipo, titulo, mensaje, prioridad } = notification;

        let toastType = 'info';
        let icon = 'info-circle';

        switch (tipo) {
            case 'INCIDENCIA_NUEVA':
                toastType = 'primary';
                icon = 'plus-circle';
                break;
            case 'INCIDENCIA_ASIGNADA':
                toastType = 'info';
                icon = 'user-check';
                break;
            case 'INCIDENCIA_ACTUALIZADA':
                toastType = 'warning';
                icon = 'edit';
                break;
            case 'INCIDENCIA_RESUELTA':
                toastType = 'success';
                icon = 'check-circle';
                break;
            case 'INCIDENCIA_CERRADA':
                toastType = 'secondary';
                icon = 'times-circle';
                break;
            default:
                toastType = 'info';
                icon = 'bell';
        }

        // Ajustar tipo según prioridad
        if (prioridad === 'ALTA') {
            toastType = 'danger';
        }

        this.showToast(mensaje, toastType, titulo, icon, 8000);
    }

    // Mostrar toast personalizado
    showToast(message, type = 'info', title = 'Notificación', icon = 'bell', duration = 5000) {
        const toastContainer = this.getToastContainer();
        const toastId = 'toast-' + Date.now();

        const toastHtml = `
            <div id="${toastId}" class="toast" role="alert" aria-live="assertive" aria-atomic="true" data-bs-delay="${duration}">
                <div class="toast-header">
                    <i class="fas fa-${icon} me-2 text-${type}"></i>
                    <strong class="me-auto">${title}</strong>
                    <small class="text-muted">ahora</small>
                    <button type="button" class="btn-close" data-bs-dismiss="toast"></button>
                </div>
                <div class="toast-body">
                    ${message}
                </div>
            </div>
        `;

        toastContainer.insertAdjacentHTML('beforeend', toastHtml);

        const toastElement = document.getElementById(toastId);
        const toast = new bootstrap.Toast(toastElement);
        toast.show();

        // Limpiar después de ocultar
        toastElement.addEventListener('hidden.bs.toast', () => {
            toastElement.remove();
        });
    }

    // Obtener o crear contenedor de toasts
    getToastContainer() {
        let container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            container.className = 'toast-container position-fixed top-0 end-0 p-3';
            container.style.zIndex = '1055';
            document.body.appendChild(container);
        }
        return container;
    }

    // Actualizar badge de notificaciones
    updateNotificationBadge() {
        const unreadCount = this.notifications.filter(n => !n.read).length;
        const badges = document.querySelectorAll('.notification-badge');

        badges.forEach(badge => {
            if (unreadCount > 0) {
                badge.textContent = unreadCount > 99 ? '99+' : unreadCount;
                badge.style.display = 'flex';
            } else {
                badge.style.display = 'none';
            }
        });

        // Actualizar título de la página si hay notificaciones
        if (unreadCount > 0) {
            document.title = `(${unreadCount}) ${this.getOriginalTitle()}`;
        } else {
            document.title = this.getOriginalTitle();
        }
    }

    // Obtener título original de la página
    getOriginalTitle() {
        const titles = {
            'gerente.html': 'Panel de Gerente - VCSystems',
            'tecnico.html': 'Panel de Técnico - VCSystems',
            'cliente.html': 'Panel de Cliente - VCSystems'
        };

        const currentPage = window.location.pathname.split('/').pop();
        return titles[currentPage] || 'VCSystems';
    }

    // Actualizar tabla relevante
    updateRelevantTable(notification) {
        const { tipo } = notification;

        // Recargar tabla de incidencias si existe
        if (typeof window.reloadIncidenciasTable === 'function') {
            window.reloadIncidenciasTable();
        }

        // Actualizar específicamente según el tipo
        switch (tipo) {
            case 'INCIDENCIA_NUEVA':
            case 'INCIDENCIA_ASIGNADA':
            case 'INCIDENCIA_ACTUALIZADA':
            case 'INCIDENCIA_RESUELTA':
            case 'INCIDENCIA_CERRADA':
                this.refreshIncidenciasData();
                break;
        }
    }

    // Refrescar datos de incidencias
    async refreshIncidenciasData() {
        try {
            // Solo refrescar si estamos en una página que muestra incidencias
            const currentPage = window.location.pathname.split('/').pop();
            if (['gerente.html', 'tecnico.html', 'cliente.html'].includes(currentPage)) {
                // Disparar evento personalizado para que las páginas puedan escuchar
                document.dispatchEvent(new CustomEvent('incidenciasUpdated'));
            }
        } catch (error) {
            console.error('Error refrescando datos:', error);
        }
    }

    // Reproducir sonido de notificación
    playNotificationSound() {
        if (this.isNotificationSoundEnabled()) {
            // Crear elemento audio temporal
            const audio = new Audio('data:audio/wav;base64,UklGRnoGAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQoGAACBhYqFbF1fdJivrJBhNjVgodDbq2EcBj+a2/LDciUFLIHO8tiJNwgZaLvt559NEAxQp+PwtmMcBjiR1/LMeSwFJHfH8N2QQAoUXrTp66hVFApGn+DyvmAaBkOa2+7CeDIGnzz/'); // Tono simple
            audio.volume = 0.3;
            audio.play().catch(e => console.log('No se pudo reproducir sonido:', e));
        }
    }

    // Verificar si el sonido está habilitado
    isNotificationSoundEnabled() {
        return localStorage.getItem('notification_sound') !== 'false';
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
