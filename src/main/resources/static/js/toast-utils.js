/**
 * Utilidad común para gestión de tostadas (toast notifications)
 * Elimina la duplicación de código entre security.js e incident-notifications.js
 */

class ToastManager {
    constructor() {
        this.toastContainer = this.createToastContainer();
    }

    /**
     * Crea el contenedor de tostadas si no existe
     */
    createToastContainer() {
        let container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            container.style.cssText = `
                position: fixed;
                top: 20px;
                right: 20px;
                z-index: 10000;
                max-width: 400px;
            `;
            document.body.appendChild(container);
        }
        return container;
    }

    /**
     * Muestra una tostada con el mensaje y tipo especificados
     * @param {string} message - Mensaje a mostrar
     * @param {string} type - Tipo de tostada: 'success', 'error', 'warning', 'info'
     * @param {number} duration - Duración en ms (por defecto 3000)
     * @param {Object} options - Opciones adicionales
     */
    showToast(message, type = 'info', duration = 3000, options = {}) {
        const toast = this.createToastElement(message, type, options);
        this.toastContainer.appendChild(toast);

        // Animación de entrada
        setTimeout(() => {
            toast.classList.add('toast-show');
        }, 10);

        // Auto-remover después de la duración especificada
        setTimeout(() => {
            this.removeToast(toast);
        }, duration);

        // Remover al hacer clic
        toast.addEventListener('click', () => {
            this.removeToast(toast);
        });

        return toast;
    }

    /**
     * Crea el elemento HTML de la tostada
     */
    createToastElement(message, type, options) {
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;

        const styles = this.getToastStyles(type);
        toast.style.cssText = `
            ${styles}
            opacity: 0;
            transform: translateX(100%);
            transition: all 0.3s ease-in-out;
            margin-bottom: 10px;
            padding: 15px 20px;
            border-radius: 8px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            cursor: pointer;
            font-family: Arial, sans-serif;
            font-size: 14px;
            line-height: 1.4;
            word-wrap: break-word;
            position: relative;
            min-width: 250px;
            max-width: 400px;
        `;

        // Icono según el tipo
        const icon = this.getIcon(type);

        toast.innerHTML = `
            <div style="display: flex; align-items: center;">
                <span style="margin-right: 10px; font-size: 18px;">${icon}</span>
                <div style="flex: 1;">
                    ${options.title ? `<div style="font-weight: bold; margin-bottom: 4px;">${options.title}</div>` : ''}
                    <div>${message}</div>
                </div>
                <span style="margin-left: 10px; opacity: 0.7; font-size: 18px;">×</span>
            </div>
        `;

        return toast;
    }

    /**
     * Obtiene los estilos CSS según el tipo de tostada
     */
    getToastStyles(type) {
        const styles = {
            success: 'background: linear-gradient(135deg, #28a745, #20c997); color: white;',
            error: 'background: linear-gradient(135deg, #dc3545, #e74c3c); color: white;',
            warning: 'background: linear-gradient(135deg, #ffc107, #ff8c00); color: #212529;',
            info: 'background: linear-gradient(135deg, #17a2b8, #007bff); color: white;'
        };
        return styles[type] || styles.info;
    }

    /**
     * Obtiene el icono según el tipo de tostada
     */
    getIcon(type) {
        const icons = {
            success: '✅',
            error: '❌',
            warning: '⚠️',
            info: 'ℹ️'
        };
        return icons[type] || icons.info;
    }

    /**
     * Remueve una tostada con animación
     */
    removeToast(toast) {
        if (toast && toast.parentNode) {
            toast.style.opacity = '0';
            toast.style.transform = 'translateX(100%)';

            setTimeout(() => {
                if (toast.parentNode) {
                    toast.parentNode.removeChild(toast);
                }
            }, 300);
        }
    }

    /**
     * Métodos de conveniencia para diferentes tipos de tostadas
     */
    success(message, duration = 3000, options = {}) {
        return this.showToast(message, 'success', duration, options);
    }

    error(message, duration = 5000, options = {}) {
        return this.showToast(message, 'error', duration, options);
    }

    warning(message, duration = 4000, options = {}) {
        return this.showToast(message, 'warning', duration, options);
    }

    info(message, duration = 3000, options = {}) {
        return this.showToast(message, 'info', duration, options);
    }

    /**
     * Limpia todas las tostadas
     */
    clearAll() {
        const toasts = this.toastContainer.querySelectorAll('.toast');
        toasts.forEach(toast => this.removeToast(toast));
    }
}

// Añadir estilos CSS para las animaciones
const style = document.createElement('style');
style.textContent = `
    .toast-show {
        opacity: 1 !important;
        transform: translateX(0) !important;
    }
    
    .toast:hover {
        transform: translateX(-5px) !important;
        box-shadow: 0 6px 20px rgba(0,0,0,0.25) !important;
    }
`;
document.head.appendChild(style);

// Crear instancia global
window.ToastManager = ToastManager;

// Exportar para uso como módulo ES6 si está disponible
if (typeof module !== 'undefined' && module.exports) {
    module.exports = ToastManager;
}
