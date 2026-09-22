package cl.duoc.carrito.service;

import cl.duoc.carrito.dto.CarritoRequest;
import cl.duoc.carrito.dto.CarritoResponse;
import cl.duoc.carrito.model.entity.Carrito;
import cl.duoc.carrito.repository.CarritoRepository;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CarritoService {

    private final CarritoRepository carritoRepository;

    public CarritoService(CarritoRepository carritoRepository) {
        this.carritoRepository = carritoRepository;
    }

    public List<CarritoResponse> listarCarrito() {

        return carritoRepository.findAll()
                .stream()
                .map(this::convertirAResponse)
                .toList();
    }

    public CarritoResponse buscarPorId(Long id) {

        Carrito carrito = carritoRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Producto del carrito no encontrado con ID: " + id));

        return convertirAResponse(carrito);
    }

    public CarritoResponse agregarProducto(CarritoRequest request) {

        BigDecimal subtotal = request.getPrecioUnitario()
                .multiply(BigDecimal.valueOf(request.getCantidad()));

        Carrito carrito = Carrito.builder()
                .productoId(request.getProductoId())
                .cantidad(request.getCantidad())
                .precioUnitario(request.getPrecioUnitario())
                .subtotal(subtotal)
                .build();

        Carrito guardado = carritoRepository.save(carrito);

        return convertirAResponse(guardado);
    }

    public CarritoResponse actualizarProducto(
            Long id,
            CarritoRequest request) {

        Carrito carrito = carritoRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Producto del carrito no encontrado con ID: " + id));

        BigDecimal subtotal = request.getPrecioUnitario()
                .multiply(BigDecimal.valueOf(request.getCantidad()));

        carrito.setProductoId(request.getProductoId());
        carrito.setCantidad(request.getCantidad());
        carrito.setPrecioUnitario(request.getPrecioUnitario());
        carrito.setSubtotal(subtotal);

        Carrito actualizado = carritoRepository.save(carrito);

        return convertirAResponse(actualizado);
    }

    public void eliminarProducto(Long id) {

        Carrito carrito = carritoRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Producto del carrito no encontrado con ID: " + id));

        carritoRepository.delete(carrito);
    }

    public void vaciarCarrito() {
        carritoRepository.deleteAll();
    }

    private CarritoResponse convertirAResponse(Carrito carrito) {

        return CarritoResponse.builder()
                .id(carrito.getId())
                .productoId(carrito.getProductoId())
                .cantidad(carrito.getCantidad())
                .precioUnitario(carrito.getPrecioUnitario())
                .subtotal(carrito.getSubtotal())
                .build();
    }
}