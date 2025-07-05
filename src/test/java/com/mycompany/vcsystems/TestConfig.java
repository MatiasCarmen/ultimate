package com.mycompany.vcsystems;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.mycompany.vcsystems.modelo.repository.*;
import com.mycompany.vcsystems.security.JwtTokenProvider;

@TestConfiguration
public class TestConfig {

    @MockBean
    private IncidenciaRepository incidenciaRepository;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
