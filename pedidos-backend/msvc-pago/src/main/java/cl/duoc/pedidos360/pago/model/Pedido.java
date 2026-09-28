package cl.duoc.pedidos360.pago.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Representa una compra confirmada.
 * El servicio conserva historial y datos no sensibles del pago; nunca guarda CVV ni el número completo.
 */
@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String numeroPedido;

    @Column(nullable = false)
    private UUID usuarioId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String nombreUsuario;

    @Column(nullable = false, length = 600)
    private String direccion;

    @Column(nullable = false)
    private long total;

    @Column(nullable = false)
    private String metodoPago;

    @Column(nullable = false, length = 4)
    private String tarjetaUltimos4;

    @Column(nullable = false)
    private String estado;

    @Column(nullable = false)
    private boolean emailEnviado;

    @Column(nullable = false)
    private Instant creadoEn = Instant.now();

    @OneToMany(
            mappedBy = "pedido",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private List<PedidoItem> items = new ArrayList<>();

    /** Mantiene ambos lados de la relación Pedido -> PedidoItem sincronizados. */
    public void add(PedidoItem item) {
        item.setPedido(this);
        items.add(item);
    }

    public UUID getId() {
        return id;
    }

    public String getNumeroPedido() {
        return numeroPedido;
    }

    public void setNumeroPedido(String numeroPedido) {
        this.numeroPedido = numeroPedido;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(UUID usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getTarjetaUltimos4() {
        return tarjetaUltimos4;
    }

    public void setTarjetaUltimos4(String tarjetaUltimos4) {
        this.tarjetaUltimos4 = tarjetaUltimos4;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public boolean isEmailEnviado() {
        return emailEnviado;
    }

    public void setEmailEnviado(boolean emailEnviado) {
        this.emailEnviado = emailEnviado;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public List<PedidoItem> getItems() {
        return items;
    }
}
