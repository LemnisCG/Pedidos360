package cl.duoc.pedidos360.bff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Deshabilitado para APIs REST stateless
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated() // Toda petición exige un token Bearer
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {})); 

        return http.build();
    }
}