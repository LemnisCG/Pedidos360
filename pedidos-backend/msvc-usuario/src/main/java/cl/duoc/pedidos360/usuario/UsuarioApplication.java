package cl.duoc.pedidos360.usuario;

import cl.duoc.pedidos360.usuario.oauth.OAuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/** Microservicio dueño de las identidades locales y federadas de Pedidos360. */
@SpringBootApplication
@EnableConfigurationProperties(OAuthProperties.class)
public class UsuarioApplication {
    public static void main(String[] args) {
        SpringApplication.run(UsuarioApplication.class, args);
    }
}
