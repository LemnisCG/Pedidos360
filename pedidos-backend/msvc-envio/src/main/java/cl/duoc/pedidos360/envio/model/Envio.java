package cl.duoc.pedidos360.envio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Registro de la preparación de envío y del resultado de la notificación SMTP. */
@Entity
@Table(name = "envios")
public class Envio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID pedidoId;

    @Column(nullable = false)
    private UUID usuarioId;

    @Column(nullable = false)
    private String numeroPedido;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 600)
    private String direccion;

    @Column(nullable = false)
    private String estado;

    @Column(nullable = false)
    private boolean emailEnviado;

    @Column(nullable = false)
    private Instant creadoEn = Instant.now();

    public UUID getId() {
        return id;
    }

    public void setPedidoId(UUID pedidoId) {
        this.pedidoId = pedidoId;
    }

    public void setUsuarioId(UUID usuarioId) {
        this.usuarioId = usuarioId;
    }

    public void setNumeroPedido(String numeroPedido) {
        this.numeroPedido = numeroPedido;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public void setEmailEnviado(boolean emailEnviado) {
        this.emailEnviado = emailEnviado;
    }

    public boolean isEmailEnviado() {
        return emailEnviado;
    }
}
