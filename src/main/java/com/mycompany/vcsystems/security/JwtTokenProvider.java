package com.mycompany.vcsystems.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import com.mycompany.vcsystems.modelo.entidades.Usuario;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.HashSet;

@Component
public class JwtTokenProvider {

    private final TokenService tokenService;

    // Lista negra de tokens invalidados
    private Set<String> blacklistedTokens = new HashSet<>();

    // Inyección por constructor para evitar dependencias circulares
    public JwtTokenProvider(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    public String createToken(String username, Collection<? extends GrantedAuthority> authorities) {
        return tokenService.generateToken(username, authorities);
    }

    public String createTokenForUser(Usuario user) {
        List<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_" + user.getRol().name())
        );
        return tokenService.generateToken(user.getCorreo(), authorities);
    }

    public String generateRefreshToken(String username) {
        return tokenService.generateRefreshToken(username);
    }

    public String getUsernameFromToken(String token) {
        return tokenService.getUsernameFromToken(token);
    }

    public Authentication getAuthentication(String token) {
        String username = tokenService.getUsernameFromToken(token);
        String roles = tokenService.getRolesFromToken(token);

        Collection<? extends GrantedAuthority> authorities;
        if (roles != null && !roles.isEmpty()) {
            authorities = Arrays.stream(roles.split(","))
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        } else {
            authorities = List.of();
        }

        return new UsernamePasswordAuthenticationToken(username, "", authorities);
    }

    public boolean validateToken(String token) {
        // Verificar si el token está en la lista negra
        if (blacklistedTokens.contains(token)) {
            return false;
        }
        return tokenService.validateToken(token);
    }

    public String refrescarToken(String oldToken) {
        return tokenService.refreshToken(oldToken);
    }

    // Método para invalidar tokens (lista negra)
    public void invalidateToken(String token) {
        blacklistedTokens.add(token);
    }

    // Método para limpiar tokens expirados de la lista negra
    public void cleanupExpiredTokens() {
        blacklistedTokens.removeIf(token -> !tokenService.validateToken(token));
    }
}
