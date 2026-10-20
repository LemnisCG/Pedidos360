package cl.duoc.pedidos360.usuario;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Microservicio dueño de los perfiles locales de los usuarios autenticados en Entra External ID. */
@SpringBootApplication
public class UsuarioApplication {
    public static void main(String[] args) {
        SpringApplication.run(UsuarioApplication.class, args);
    }
}
