package cl.duoc.pedidos360.producto.service;

import cl.duoc.pedidos360.producto.dto.StockRequest;
import cl.duoc.pedidos360.producto.model.Producto;
import cl.duoc.pedidos360.producto.repository.ProductoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Operaciones de consulta y descuento transaccional del inventario. */
@Service
public class ProductoService {

    private final ProductoRepository repository;

    public ProductoService(ProductoRepository repository) {
        this.repository = repository;
    }

    public List<Producto> all() {
        return repository.findAll();
    }

    public Producto one(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    /**
     * Descuenta cada producto con bloqueo pesimista.
     * Si un ítem no tiene stock suficiente, la transacción completa se revierte.
     */
    @Transactional
    public void descontar(List<StockRequest> items) {
        for (StockRequest item : items) {
            Producto producto = repository.findByIdForUpdate(item.productoId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Producto no encontrado: " + item.productoId()
                    ));

            if (producto.getStock() < item.cantidad()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Stock insuficiente para " + producto.getNombre()
                );
            }

            producto.setStock(producto.getStock() - item.cantidad());
            repository.save(producto);
        }
    }
}
