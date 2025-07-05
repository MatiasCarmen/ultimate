// Manejo de autenticación y seguridad
class SecurityManager {
    constructor() {
        this.token = localStorage.getItem('jwt_token');
        this.userInfo = JSON.parse(localStorage.getItem('user_info') || '{}');
        this.apiBase = '/api';
        this.toastManager = new ToastManager(); // Usar el módulo común de tostadas

        // Verificar autenticación al cargar
        this.checkAuthentication();

        // Configurar interceptor para requests
        this.setupHttpInterceptor();
    }

    // Verificar si el usuario está autenticado
    isAuthenticated() {
        return this.token && this.token !== 'null' && !this.isTokenExpired();
    }

    // Verificar si el token ha expirado
    isTokenExpired() {
        if (!this.token) return true;

        try {
            const payload = JSON.parse(atob(this.token.split('.')[1]));
            const currentTime = Date.now() / 1000;
            return payload.exp < currentTime;
        } catch (error) {
            console.error('Error verificando token:', error);
            return true;
        }
    }

    // Login del usuario - MEJORADO con manejo robusto de errores
    async login(email, password) {
        try {
            this.showLoading('Iniciando sesión...');

            const response = await this.makeRequest(`${this.apiBase}/auth/login`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ correo: email, contrasena: password })
            });

            const data = await response.json();

            if (response.ok && data.token) {
                this.token = data.token;
                this.userInfo = data.usuario;

                // Guardar en localStorage
                localStorage.setItem('jwt_token', this.token);
                localStorage.setItem('user_info', JSON.stringify(this.userInfo));

                this.hideLoading();
                this.toastManager.success('¡Inicio de sesión exitoso!', 2000);
                setTimeout(() => this.redirectByRole(), 1000);
                return { success: true, data };
            } else {
                this.hideLoading();
                throw new Error(data.message || 'Credenciales inválidas');
            }
        } catch (error) {
            this.hideLoading();
            this.handleLoginError(error);
            return { success: false, error: error.message };
        }
    }

    // NUEVO: Manejo específico de errores de login
    handleLoginError(error) {
        let errorMessage = 'Error de autenticación';
        let errorDetails = error.message;

        if (error.name === 'NetworkError' || error.message.includes('fetch')) {
            errorMessage = 'Error de conexión';
            errorDetails = 'No se pudo conectar con el servidor. Verifica tu conexión a internet.';
        } else if (error.message.includes('401') || error.message.includes('Credenciales')) {
            errorMessage = 'Credenciales inválidas';
            errorDetails = 'El email o contraseña son incorrectos.';
        } else if (error.message.includes('500')) {
            errorMessage = 'Error del servidor';
            errorDetails = 'Error interno del servidor. Intenta nuevamente en unos minutos.';
        }

        this.toastManager.error(errorDetails, 5000, { title: errorMessage });
    }

    // NUEVO: Método para realizar requests con manejo robusto de errores
    async makeRequest(url, options = {}) {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 10000); // 10 segundos timeout

        try {
            const response = await fetch(url, {
                ...options,
                signal: controller.signal,
                headers: {
                    'Content-Type': 'application/json',
                    ...options.headers
                }
            });

            clearTimeout(timeoutId);

            // Verificar si la respuesta es válida
            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            return response;
        } catch (error) {
            clearTimeout(timeoutId);

            if (error.name === 'AbortError') {
                throw new Error('La solicitud tardó demasiado tiempo. Verifica tu conexión.');
            } else if (error.name === 'TypeError' && error.message.includes('fetch')) {
                throw new Error('Error de red. Verifica tu conexión a internet.');
            }

            throw error;
        }
    }

    // Logout del usuario
    logout() {
        this.token = null;
        this.userInfo = {};
        localStorage.removeItem('jwt_token');
        localStorage.removeItem('user_info');
        window.location.href = '/pages/login.html';
    }

    // Redireccionar según el rol del usuario
    redirectByRole() {
        if (!this.isAuthenticated()) {
            window.location.href = '/pages/login.html';
            return;
        }

        const role = this.userInfo.rol;
        switch (role) {
            case 'GERENTE':
                window.location.href = '/pages/gerente.html';
                break;
            case 'TECNICO':
                window.location.href = '/pages/tecnico.html';
                break;
            case 'CLIENTE':
                window.location.href = '/pages/cliente.html';
                break;
            default:
                console.error('Rol no reconocido:', role);
                this.logout();
        }
    }

    // Verificar autenticación y redireccionar si es necesario
    checkAuthentication() {
        const currentPage = window.location.pathname;
        const isLoginPage = currentPage.includes('login.html');

        if (!this.isAuthenticated() && !isLoginPage) {
            window.location.href = '/pages/login.html';
            return false;
        }

        if (this.isAuthenticated() && isLoginPage) {
            this.redirectByRole();
            return false;
        }

        return true;
    }

    // Verificar autorización por rol
    hasRole(requiredRole) {
        return this.userInfo.rol === requiredRole;
    }

    // Configurar interceptor HTTP para incluir token
    setupHttpInterceptor() {
        const originalFetch = window.fetch;

        window.fetch = async (url, options = {}) => {
            // Solo agregar token para requests a la API
            if (url.startsWith(this.apiBase) || url.startsWith('/api')) {
                options.headers = {
                    ...options.headers,
                    'Authorization': `Bearer ${this.token}`,
                    'Content-Type': 'application/json'
                };
            }

            const response = await originalFetch(url, options);

            // Manejar errores 401 (no autorizado)
            if (response.status === 401) {
                this.logout();
                return response;
            }

            return response;
        };
    }

    // Realizar request autenticado - MEJORADO
    async authenticatedRequest(url, options = {}) {
        if (!this.isAuthenticated()) {
            this.toastManager.warning('Sesión expirada. Redirigiendo al login...', 3000);
            setTimeout(() => this.logout(), 2000);
            return null;
        }

        try {
            const response = await this.makeRequest(url, {
                ...options,
                headers: {
                    'Authorization': `Bearer ${this.token}`,
                    'Content-Type': 'application/json',
                    ...options.headers
                }
            });

            return response;
        } catch (error) {
            return this.handleAuthenticatedRequestError(error, url, options);
        }
    }

    // NUEVO: Manejo específico de errores en requests autenticados
    async handleAuthenticatedRequestError(error, originalUrl, originalOptions) {
        if (error.message.includes('401')) {
            // Token inválido o expirado
            this.toastManager.warning('Sesión expirada. Redirigiendo al login...', 3000);
            setTimeout(() => this.logout(), 2000);
            return null;
        } else if (error.message.includes('403')) {
            this.toastManager.error('No tienes permisos para realizar esta acción', 4000);
            return null;
        } else if (error.message.includes('500')) {
            this.toastManager.error('Error del servidor. Intenta nuevamente.', 4000);
            return null;
        } else if (error.message.includes('conexión') || error.message.includes('red')) {
            // Error de red - mostrar opción de reintentar
            this.showNetworkErrorRetry(originalUrl, originalOptions);
            return null;
        } else {
            this.toastManager.error(`Error inesperado: ${error.message}`, 5000);
            throw error;
        }
    }

    // NUEVO: Mostrar error de red con opción de reintentar
    showNetworkErrorRetry(url, options) {
        const retryToast = this.toastManager.error(
            'Error de conexión. Haz clic para reintentar.',
            0, // No auto-remover
            { title: 'Sin conexión' }
        );

        retryToast.style.cursor = 'pointer';
        retryToast.addEventListener('click', async () => {
            this.toastManager.removeToast(retryToast);
            this.toastManager.info('Reintentando...', 2000);

            try {
                await this.authenticatedRequest(url, options);
            } catch (error) {
                console.error('Error en reintento:', error);
            }
        });
    }

    // Obtener información del usuario
    getUserInfo() {
        return this.userInfo;
    }

    // Obtener token
    getToken() {
        return this.token;
    }

    // Mostrar alerta
    showAlert(message, type = 'info') {
        // Crear toast de Bootstrap
        const toastContainer = document.getElementById('toast-container') || this.createToastContainer();

        const toastId = 'toast-' + Date.now();
        const toastHtml = `
            <div id="${toastId}" class="toast" role="alert" aria-live="assertive" aria-atomic="true">
                <div class="toast-header">
                    <i class="fas fa-${this.getIconForType(type)} me-2 text-${type}"></i>
                    <strong class="me-auto">Sistema</strong>
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

        // Remover después de mostrar
        toastElement.addEventListener('hidden.bs.toast', () => {
            toastElement.remove();
        });
    }

    // Crear contenedor de toasts si no existe
    createToastContainer() {
        const container = document.createElement('div');
        container.id = 'toast-container';
        container.className = 'toast-container position-fixed top-0 end-0 p-3';
        container.style.zIndex = '1055';
        document.body.appendChild(container);
        return container;
    }

    // Obtener icono según tipo de alerta
    getIconForType(type) {
        const icons = {
            success: 'check-circle',
            danger: 'exclamation-triangle',
            warning: 'exclamation-circle',
            info: 'info-circle'
        };
        return icons[type] || 'info-circle';
    }

    // Mostrar loading
    showLoading(message = 'Cargando...') {
        const existing = document.getElementById('loading-overlay');
        if (existing) existing.remove();

        const loadingHtml = `
            <div id="loading-overlay" class="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center" 
                 style="background: rgba(0,0,0,0.5); z-index: 1060;">
                <div class="card p-4 text-center">
                    <div class="loading-spinner mx-auto mb-3"></div>
                    <p class="mb-0">${message}</p>
                </div>
            </div>
        `;

        document.body.insertAdjacentHTML('beforeend', loadingHtml);
    }

    // Ocultar loading
    hideLoading() {
        const loading = document.getElementById('loading-overlay');
        if (loading) loading.remove();
    }

    // Inicializar tema
    initializeTheme() {
        const savedTheme = localStorage.getItem('theme') || 'light';
        document.documentElement.setAttribute('data-theme', savedTheme);

        // Crear botón de tema si no existe
        if (!document.getElementById('theme-toggle')) {
            const themeButton = document.createElement('button');
            themeButton.id = 'theme-toggle';
            themeButton.className = 'theme-toggle btn';
            themeButton.innerHTML = '<i class="fas fa-moon"></i>';
            themeButton.title = 'Cambiar tema';

            themeButton.addEventListener('click', this.toggleTheme);
            document.body.appendChild(themeButton);
        }

        this.updateThemeIcon();
    }

    // Cambiar tema
    toggleTheme() {
        const currentTheme = document.documentElement.getAttribute('data-theme');
        const newTheme = currentTheme === 'dark' ? 'light' : 'dark';

        document.documentElement.setAttribute('data-theme', newTheme);
        localStorage.setItem('theme', newTheme);

        securityManager.updateThemeIcon();
    }

    // Actualizar icono de tema
    updateThemeIcon() {
        const themeButton = document.getElementById('theme-toggle');
        if (themeButton) {
            const currentTheme = document.documentElement.getAttribute('data-theme');
            const icon = themeButton.querySelector('i');
            icon.className = currentTheme === 'dark' ? 'fas fa-sun' : 'fas fa-moon';
        }
    }

    // Formatear fecha
    formatDate(dateString) {
        const date = new Date(dateString);
        return date.toLocaleDateString('es-ES', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    // Validar formulario
    validateForm(formElement) {
        const inputs = formElement.querySelectorAll('input[required], select[required], textarea[required]');
        let isValid = true;

        inputs.forEach(input => {
            if (!input.value.trim()) {
                input.classList.add('is-invalid');
                isValid = false;
            } else {
                input.classList.remove('is-invalid');
                input.classList.add('is-valid');
            }
        });

        return isValid;
    }

    // Limpiar validación de formulario
    clearFormValidation(formElement) {
        const inputs = formElement.querySelectorAll('.is-valid, .is-invalid');
        inputs.forEach(input => {
            input.classList.remove('is-valid', 'is-invalid');
        });
    }
}

// Instancia global del manager de seguridad
const securityManager = new SecurityManager();

// Inicializar cuando el DOM esté listo
document.addEventListener('DOMContentLoaded', function() {
    securityManager.initializeTheme();

    // Configurar logout si existe el botón
    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', (e) => {
            e.preventDefault();
            securityManager.logout();
        });
    }
});

// Exportar para uso global
window.securityManager = securityManager;
