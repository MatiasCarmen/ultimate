package com.mycompany.vcsystems.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.scheduling.annotation.EnableAsync;

import com.mycompany.vcsystems.security.JwtAuthenticationFilter;
import com.mycompany.vcsystems.security.JwtTokenProvider;
import com.mycompany.vcsystems.security.TokenService;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableAsync // Habilitar procesamiento asíncrono para eventos
public class SecurityConfig {

    private final TokenService tokenService;

    // Inyección por constructor para evitar dependencias circulares
    public SecurityConfig(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // Crear el filtro JWT localmente usando los beans disponibles
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(tokenService);
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenProvider);

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Recursos estáticos y login
                        .requestMatchers("/", "/login.html", "/js/**", "/css/**").permitAll()
                        // Endpoints públicos de autenticación
                        .requestMatchers("/api/auth/login", "/api/auth/refresh").permitAll()
                        // Rutas específicas por rol
                        .requestMatchers("/api/reportes/**").hasAnyRole("GERENTE", "ADMIN")
                        .requestMatchers("/api/incidencias/**").hasAnyRole("ADMIN", "GERENTE", "TECNICO", "CLIENTE")
                        .requestMatchers("/api/solicitudes/**").hasAnyRole("ADMIN", "TECNICO", "GERENTE")
                        // Cualquier otra ruta requiere autenticación
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:8081"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
