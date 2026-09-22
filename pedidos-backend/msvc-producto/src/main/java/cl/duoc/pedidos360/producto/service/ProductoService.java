package cl.duoc.pedidos360.producto.service;

import cl.duoc.pedidos360.producto.model.Producto;
import cl.duoc.pedidos360.producto.repository.ProductoRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductoService {
    private final ProductoRepository repo;

    public ProductoService(ProductoRepository repo) {
        this.repo = repo;
    }

    // El orden por ID mantiene estable la distribución visual 5 + 5 + 3 del catálogo.
    public List<Producto> listar() {
        return repo.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    public Producto buscar(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));
    }

    public Producto guardar(Producto p) {
        return repo.save(p);
    }

    public Producto actualizar(Long id, Producto p) {
        Producto actual = buscar(id);
        actual.setNombre(p.getNombre());
        actual.setDescripcion(p.getDescripcion());
        actual.setPrecio(p.getPrecio());
        actual.setStock(p.getStock());
        actual.setImagenUrl(p.getImagenUrl());
        actual.setCategoria(p.getCategoria());
        actual.setFechaLanzamiento(p.getFechaLanzamiento());
        return repo.save(actual);
    }

    public void eliminar(Long id) {
        repo.delete(buscar(id));
    }

    /**
     * Descuenta el inventario real cuando msvc-orden confirma una compra.
     * Primero valida todos los productos y recién después aplica cambios;
     * así no queda una compra parcialmente descontada por falta de stock.
     */
    @Transactional
    public List<Producto> descontarStock(List<StockItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("El pedido no contiene productos");
        }

        // Agrupa por producto por seguridad, aunque el carrito normalmente ya viene sin duplicados.
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (StockItem item : items) {
            if (item.productoId() == null || item.cantidad() == null || item.cantidad() <= 0) {
                throw new IllegalArgumentException("Producto o cantidad inválida");
            }
            cantidades.merge(item.productoId(), item.cantidad(), Integer::sum);
        }

        List<Producto> productos = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : cantidades.entrySet()) {
            Producto producto = repo.buscarParaActualizar(entry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + entry.getKey()));

            int stockActual = producto.getStock() == null ? 0 : producto.getStock();
            if (stockActual < entry.getValue()) {
                throw new IllegalStateException(
                        "Stock insuficiente para " + producto.getNombre() +
                        ". Disponible: " + stockActual + ", solicitado: " + entry.getValue()
                );
            }
            productos.add(producto);
        }

        // Todos pasaron la validación: ahora sí se descuenta.
        for (Producto producto : productos) {
            int cantidad = cantidades.get(producto.getId());
            producto.setStock(producto.getStock() - cantidad);
        }

        return repo.saveAll(productos);
    }

    public record StockItem(Long productoId, Integer cantidad) {}
}
