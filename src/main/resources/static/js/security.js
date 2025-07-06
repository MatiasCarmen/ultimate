// Manejo de autenticación y seguridad
class SecurityManager {
    constructor() {
        this.userInfo = JSON.parse(localStorage.getItem('user_info') || null);
        this.apiBase = '/api';
        this.toastManager = new ToastManager(); // Usar el módulo común de tostadas
        this.checkAuthentication();
    }

    // Verificar si el usuario está autenticado
    isAuthenticated() {
        // Consideramos autenticado si tenemos la información del usuario
        return this.userInfo !== null && this.userInfo.idUsuario != null;
    }

    // Login del usuario
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

            if (response.ok && data.success && data.usuario) {
                // No manejamos tokens, solo la info del usuario
                this.userInfo = data.usuario;

                // Guardar en localStorage
                localStorage.setItem('user_info', JSON.stringify(this.userInfo));

                this.hideLoading();
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

    async makeRequest(url, options = {}) {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 10000); // 10 segundos timeout

        try {
            const response = await fetch(url, {
                ...options,
                signal: controller.signal,
                headers: {
                    'Content-Type': 'application/json',
                    ...options.headers // Permitir otras cabeceras manualmente
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
    logout() { // Simplificado para remover solo info del usuario
        this.userInfo = null;
        localStorage.removeItem('user_info');
        window.location.href = '/pages/login.html';
    }

    // Redireccionar según el rol del usuario
    redirectByRole() {
        if (!this.isAuthenticated()) {
            window.location.href = '/pages/login.html';
            return;
        }

        const role = this.getUserInfo().rol; // Usar getUserInfo para consistencia
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
        const isLoginPage = currentPage.includes('login.html') || currentPage === '/' || currentPage.includes('registro.html'); // Considerar '/' y registro.html como páginas de login

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

    // Verificar autorización por rol (usar en las páginas HTML)
    hasRole(requiredRole) {
        // Asegurarse de que userInfo esté cargado
        if (!this.userInfo || !this.userInfo.rol) {
            // console.warn('hasRole called before userInfo is available.');
            return false; // No tiene el rol si no hay info del usuario
        }
        return this.userInfo.rol === requiredRole;
    }

    // Eliminado: authenticatedRequest ya no es necesario sin JWT auth header
    // Si necesitas enviar idUsuario/rol a la API, crea un nuevo método.
    // authenticatedRequest(url, options = {}) { /* Logic removed */ }
    // Eliminado: handleAuthenticatedRequestError tampoco es necesario sin authenticatedRequest
    // handleAuthenticatedRequestError(error, originalUrl, originalOptions) { /* Logic removed */ }

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
                // Usar makeRequest para reintentar
                await this.makeRequest(url, options);
            } catch (error) {
                console.error('Error en reintento:', error);
            }
        });
    }

    // Obtener información del usuario
    getUserInfo() {
        // Asegurarse de cargar desde localStorage si no está en memoria (ej. después de refresh)
        if (!this.userInfo || !this.userInfo.idUsuario) {
            const storedUserInfo = localStorage.getItem('user_info');
            if (storedUserInfo) {
                this.userInfo = JSON.parse(storedUserInfo);
            }
        }
        return this.userInfo;
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

// Agregar evento al formulario de login (si existe)
const loginForm = document.getElementById('loginForm');
if (loginForm) {
    loginForm.addEventListener('submit', async function(event) {
        event.preventDefault(); // Prevenir el envío normal del formulario

        const email = document.getElementById('email').value;
        const password = document.getElementById('password').value;

        // Llamar al método login del securityManager
        await securityManager.login(email, password);
    });
}

// Exportar para uso global
window.securityManager = securityManager;
