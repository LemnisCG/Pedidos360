package cl.duoc.catalogo.service;

import cl.duoc.catalogo.dto.ProductoRequest;
import cl.duoc.catalogo.dto.ProductoResponse;
import cl.duoc.catalogo.model.entity.Producto;
import cl.duoc.catalogo.repository.CatalogoRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CatalogoService {

    private final CatalogoRepository catalogoRepository;

    public CatalogoService(CatalogoRepository catalogoRepository) {
        this.catalogoRepository = catalogoRepository;
    }

    // LISTAR TODOS LOS PRODUCTOS
    public List<ProductoResponse> listarProductos() {

        return catalogoRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    // BUSCAR PRODUCTO POR ID
    public ProductoResponse buscarPorId(Long id) {

        Producto producto = catalogoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));

        return convertirAResponse(producto);
    }

    // CREAR PRODUCTO
    public ProductoResponse crearProducto(ProductoRequest request) {

        Producto producto = Producto.builder()
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .precio(request.getPrecio())
                .stock(request.getStock())
                .build();

        Producto productoGuardado = catalogoRepository.save(producto);

        return convertirAResponse(productoGuardado);
    }

    // ACTUALIZAR PRODUCTO
    public ProductoResponse actualizarProducto(
            Long id,
            ProductoRequest request) {

        Producto producto = catalogoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));

        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setPrecio(request.getPrecio());
        producto.setStock(request.getStock());

        Producto productoActualizado = catalogoRepository.save(producto);

        return convertirAResponse(productoActualizado);
    }

    // ELIMINAR PRODUCTO
    public void eliminarProducto(Long id) {

        Producto producto = catalogoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));

        catalogoRepository.delete(producto);
    }

    // CONVERTIR ENTITY A DTO RESPONSE
    private ProductoResponse convertirAResponse(Producto producto) {

        return ProductoResponse.builder()
                .id(producto.getId())
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .precio(producto.getPrecio())
                .stock(producto.getStock())
                .build();
    }
}