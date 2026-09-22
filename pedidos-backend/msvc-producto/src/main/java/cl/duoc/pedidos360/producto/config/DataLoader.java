package cl.duoc.pedidos360.producto.config;

import cl.duoc.pedidos360.producto.model.Producto;
import cl.duoc.pedidos360.producto.repository.ProductoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class DataLoader {

    @Bean
    CommandLineRunner cargarCatalogo(ProductoRepository repo) {
        return args -> {
            /*
             * Catálogo principal de Pedidos360.
             *
             * Las portadas se encuentran en el frontend dentro de:
             * public/assets/games/
             *
             * El backend solo guarda la ruta pública que Angular utilizará
             * para mostrar cada imagen.
             */
            List<Producto> catalogo = List.of(
                    producto(
                            "ASTRO BOT",
                            "Aventura de plataformas con mundos coloridos, desafíos y exploración.",
                            "59990",
                            20,
                            "Aventura",
                            "/assets/games/astro-bot.webp",
                            LocalDate.of(2024, 9, 6)
                    ),
                    producto(
                            "Mario Kart 8 Deluxe",
                            "Carreras arcade con circuitos, personajes y modos multijugador.",
                            "49990",
                            25,
                            "Carreras",
                            "/assets/games/mario-kart-8-deluxe.jpg",
                            LocalDate.of(2017, 4, 28)
                    ),
                    producto(
                            "Minecraft",
                            "Construye, explora y sobrevive en un mundo abierto creado con bloques.",
                            "29990",
                            35,
                            "Sandbox",
                            "/assets/games/minecraft.jpg",
                            LocalDate.of(2018, 6, 21)
                    ),
                    producto(
                            "Street Fighter II Ultra",
                            "Combate arcade clásico con luchadores, combos y enfrentamientos competitivos.",
                            "9990",
                            18,
                            "Lucha",
                            "/assets/games/street-fighter-ii-ultra.jpg",
                            LocalDate.of(2017, 5, 26)
                    ),
                    producto(
                            "Super Mario Bros. 3",
                            "Plataformas clásicas con mundos, poderes y niveles llenos de desafíos.",
                            "19990",
                            22,
                            "Plataformas",
                            "/assets/games/super-mario-bros-3.jpg",
                            LocalDate.of(1988, 10, 23)
                    ),

                    // Segunda fila: cinco juegos adicionales.
                    producto(
                            "Red Dead Redemption 2",
                            "Aventura de mundo abierto ambientada en el ocaso del Viejo Oeste estadounidense.",
                            "29990",
                            16,
                            "Acción y aventura",
                            "/assets/games/red-dead-redemption-2.webp",
                            LocalDate.of(2018, 10, 26)
                    ),
                    producto(
                            "The Witcher 3: Wild Hunt",
                            "RPG de mundo abierto donde Geralt de Rivia recorre un continente lleno de contratos y decisiones.",
                            "24990",
                            19,
                            "RPG",
                            "/assets/games/the-witcher-3-wild-hunt.webp",
                            LocalDate.of(2015, 5, 19)
                    ),
                    producto(
                            "Super Mario Odyssey",
                            "Aventura de plataformas 3D donde Mario recorre distintos reinos acompañado por Cappy.",
                            "49990",
                            21,
                            "Plataformas",
                            "/assets/games/super-mario-odyssey.jpg",
                            LocalDate.of(2017, 10, 27)
                    ),
                    producto(
                            "Elden Ring",
                            "RPG de acción y exploración en un vasto mundo fantástico lleno de desafíos y secretos.",
                            "52990",
                            14,
                            "RPG de acción",
                            "/assets/games/elden-ring.webp",
                            LocalDate.of(2022, 2, 25)
                    ),
                    producto(
                            "God of War Ragnarök",
                            "Kratos y Atreus emprenden un viaje por los Nueve Reinos mientras se aproxima el Ragnarök.",
                            "49990",
                            17,
                            "Acción y aventura",
                            "/assets/games/god-of-war-ragnarok.webp",
                            LocalDate.of(2022, 11, 9)
                    ),

                    // Tercera fila: tres productos que el frontend centra automáticamente.
                    producto(
                            "Tekken 8",
                            "Combate 3D de nueva generación con enfrentamientos rápidos, combos y personajes icónicos.",
                            "59990",
                            13,
                            "Lucha",
                            "/assets/games/tekken-8.webp",
                            LocalDate.of(2024, 1, 26)
                    ),
                    producto(
                            "The Legend of Zelda: Tears of the Kingdom",
                            "Explora Hyrule y sus cielos utilizando nuevas habilidades para resolver acertijos y combatir enemigos.",
                            "54990",
                            18,
                            "Aventura",
                            "/assets/games/zelda-tears-of-the-kingdom.jpg",
                            LocalDate.of(2023, 5, 12)
                    ),
                    producto(
                            "Hollow Knight",
                            "Metroidvania de exploración y combate ambientado en el misterioso reino subterráneo de Hallownest.",
                            "14990",
                            28,
                            "Metroidvania",
                            "/assets/games/hollow-knight.webp",
                            LocalDate.of(2017, 2, 24)
                    )
            );

            /*
             * Compatibilidad con la primera versión demo que tenía otros nombres.
             * Si alguno todavía existe en PostgreSQL se transforma al producto real
             * correspondiente, sin tener que eliminar el volumen de Docker.
             */
            Map<String, Producto> reemplazosDemo = Map.of(
                    "Neon Drift", catalogo.get(0),
                    "Pixel Realms", catalogo.get(1),
                    "Cyber Striker", catalogo.get(2),
                    "Astro Rescue", catalogo.get(3),
                    "Kingdom Quest", catalogo.get(4)
            );

            List<Producto> existentes = repo.findAll();
            for (Producto actual : existentes) {
                Producto reemplazo = reemplazosDemo.get(actual.getNombre());
                if (reemplazo != null) {
                    // Migración única desde el catálogo ficticio: aquí sí asignamos el stock inicial.
                    copiarDatos(reemplazo, actual, true);
                }
            }
            repo.saveAll(existentes);

            /*
             * Upsert del catálogo:
             * - actualiza los productos que ya existen;
             * - inserta los ocho juegos nuevos si todavía no están en la base de datos;
             * - conserva cualquier producto extra que hayas creado manualmente.
             */
            Map<String, Producto> existentesPorNombre = new LinkedHashMap<>();
            for (Producto actual : repo.findAll()) {
                existentesPorNombre.put(actual.getNombre(), actual);
            }

            for (Producto fuente : catalogo) {
                Producto actual = existentesPorNombre.get(fuente.getNombre());
                if (actual == null) {
                    repo.save(fuente);
                } else {
                    // IMPORTANTE: no sobrescribimos el stock existente.
                    // Así las compras siguen descontadas incluso si Docker se reinicia.
                    copiarDatos(fuente, actual, false);
                    repo.save(actual);
                }
            }
        };
    }

    private Producto producto(
            String nombre,
            String descripcion,
            String precio,
            int stock,
            String categoria,
            String imagenUrl,
            LocalDate fecha
    ) {
        return Producto.builder()
                .nombre(nombre)
                .descripcion(descripcion)
                .precio(new BigDecimal(precio))
                .stock(stock)
                .categoria(categoria)
                .imagenUrl(imagenUrl)
                .fechaLanzamiento(fecha)
                .build();
    }

    private void copiarDatos(Producto origen, Producto destino, boolean actualizarStock) {
        destino.setNombre(origen.getNombre());
        destino.setDescripcion(origen.getDescripcion());
        destino.setPrecio(origen.getPrecio());
        if (actualizarStock) {
            destino.setStock(origen.getStock());
        }
        destino.setCategoria(origen.getCategoria());
        destino.setImagenUrl(origen.getImagenUrl());
        destino.setFechaLanzamiento(origen.getFechaLanzamiento());
    }
}
