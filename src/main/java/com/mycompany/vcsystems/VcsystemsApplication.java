package com.mycompany.vcsystems;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class VcsystemsApplication {

 private static final Logger log = LoggerFactory.getLogger(VcsystemsApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(VcsystemsApplication.class, args);
    }

 @Bean
 public CommandLineRunner passwordEncoderTest(PasswordEncoder passwordEncoder) {
 return args -> {
 log.info("Encoding 'password123': {}", passwordEncoder.encode("password123"));
 };
 }
}
