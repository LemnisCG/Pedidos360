package cl.duoc.pedidos360.producto.config;

import cl.duoc.pedidos360.producto.model.Producto;
import cl.duoc.pedidos360.producto.repository.ProductoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Carga el catálogo demostrativo al iniciar el servicio.
 * Si el producto ya existe, actualiza sus datos descriptivos pero conserva el stock descontado por compras previas.
 */
@Configuration
public class DataLoader {

    @Bean
    CommandLineRunner seed(ProductoRepository repository) {
        return args -> {
            List<Producto> catalogo = List.of(
                    product(
                            "ASTRO BOT",
                            "Aventura de plataformas para PS5 con mundos llenos de color, rescates de robots, "
                                    + "coleccionables y desafíos pensados para partidas ágiles.",
                            59_990, 20, "Aventura", "/assets/games/astro-bot.webp", LocalDate.of(2024, 9, 6)
                    ),
                    product(
                            "Mario Kart 8 Deluxe",
                            "Carreras arcade de Nintendo con decenas de circuitos, objetos, personajes y modalidades "
                                    + "competitivas o cooperativas.",
                            49_990, 25, "Carreras", "/assets/games/mario-kart-8-deluxe.jpg", LocalDate.of(2017, 4, 28)
                    ),
                    product(
                            "Minecraft",
                            "Sandbox de construcción, exploración y supervivencia en mundos procedurales donde cada "
                                    + "partida puede convertirse en una aventura distinta.",
                            29_990, 35, "Sandbox", "/assets/games/minecraft.jpg", LocalDate.of(2018, 6, 21)
                    ),
                    product(
                            "Street Fighter II Ultra",
                            "Combate arcade clásico con luchadores emblemáticos, movimientos especiales y partidas "
                                    + "competitivas de ritmo rápido.",
                            9_990, 18, "Lucha", "/assets/games/street-fighter-ii-ultra.jpg", LocalDate.of(2017, 5, 26)
                    ),
                    product(
                            "Super Mario Bros. 3",
                            "Plataformas clásicas con mundos temáticos, poderes, secretos y niveles que marcaron una "
                                    + "generación de videojuegos.",
                            19_990, 22, "Plataformas", "/assets/games/super-mario-bros-3.jpg", LocalDate.of(1988, 10, 23)
                    ),
                    product(
                            "Red Dead Redemption 2",
                            "Aventura narrativa de mundo abierto en el ocaso del Viejo Oeste, con exploración, "
                                    + "misiones, decisiones y un extenso sistema de interacción.",
                            29_990, 16, "Acción y aventura", "/assets/games/red-dead-redemption-2.webp", LocalDate.of(2018, 10, 26)
                    ),
                    product(
                            "The Witcher 3: Wild Hunt",
                            "RPG de mundo abierto donde Geralt de Rivia completa contratos, toma decisiones y recorre "
                                    + "un continente repleto de historias.",
                            24_990, 19, "RPG", "/assets/games/the-witcher-3-wild-hunt.webp", LocalDate.of(2015, 5, 19)
                    ),
                    product(
                            "Super Mario Odyssey",
                            "Aventura de plataformas 3D por distintos reinos, con exploración libre y habilidades "
                                    + "especiales junto a Cappy.",
                            49_990, 21, "Plataformas", "/assets/games/super-mario-odyssey.jpg", LocalDate.of(2017, 10, 27)
                    ),
                    product(
                            "Elden Ring",
                            "RPG de acción y exploración en un amplio mundo fantástico con jefes exigentes, secretos "
                                    + "y múltiples estilos de combate.",
                            52_990, 14, "RPG de acción", "/assets/games/elden-ring.webp", LocalDate.of(2022, 2, 25)
                    ),
                    product(
                            "God of War Ragnarök",
                            "Kratos y Atreus recorren los Nueve Reinos en una aventura cinematográfica con combate, "
                                    + "exploración y desarrollo narrativo.",
                            49_990, 17, "Acción y aventura", "/assets/games/god-of-war-ragnarok.webp", LocalDate.of(2022, 11, 9)
                    ),
                    product(
                            "Tekken 8",
                            "Combate 3D de nueva generación con enfrentamientos veloces, sistema Heat, combos y un "
                                    + "amplio elenco de luchadores.",
                            59_990, 13, "Lucha", "/assets/games/tekken-8.webp", LocalDate.of(2024, 1, 26)
                    ),
                    product(
                            "The Legend of Zelda: Tears of the Kingdom",
                            "Explora Hyrule, sus islas celestes y profundidades utilizando nuevas habilidades para "
                                    + "crear, resolver acertijos y combatir.",
                            54_990, 18, "Aventura", "/assets/games/zelda-tears-of-the-kingdom.jpg", LocalDate.of(2023, 5, 12)
                    ),
                    product(
                            "Hollow Knight",
                            "Metroidvania de exploración y combate en Hallownest, con rutas interconectadas, secretos "
                                    + "y desafiantes enfrentamientos.",
                            14_990, 28, "Metroidvania", "/assets/games/hollow-knight.webp", LocalDate.of(2017, 2, 24)
                    )
            );

            // Upsert descriptivo: nunca se repone automáticamente el stock de un producto existente.
            for (Producto seed : catalogo) {
                repository.findByNombre(seed.getNombre()).ifPresentOrElse(existing -> {
                    existing.setDescripcion(seed.getDescripcion());
                    existing.setPrecio(seed.getPrecio());
                    existing.setCategoria(seed.getCategoria());
                    existing.setImagenUrl(seed.getImagenUrl());
                    existing.setFechaLanzamiento(seed.getFechaLanzamiento());
                    repository.save(existing);
                }, () -> repository.save(seed));
            }
        };
    }

    private Producto product(
            String nombre,
            String descripcion,
            int precio,
            int stock,
            String categoria,
            String imagenUrl,
            LocalDate fechaLanzamiento
    ) {
        Producto producto = new Producto();
        producto.setNombre(nombre);
        producto.setDescripcion(descripcion);
        producto.setPrecio(BigDecimal.valueOf(precio));
        producto.setStock(stock);
        producto.setCategoria(categoria);
        producto.setImagenUrl(imagenUrl);
        producto.setFechaLanzamiento(fechaLanzamiento);
        return producto;
    }
}
