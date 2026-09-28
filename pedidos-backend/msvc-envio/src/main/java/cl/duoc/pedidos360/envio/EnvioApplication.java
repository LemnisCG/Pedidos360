package cl.duoc.pedidos360.envio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Punto de entrada del microservicio de envíos y notificaciones por correo. */
@SpringBootApplication
public class EnvioApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnvioApplication.class, args);
    }
}
