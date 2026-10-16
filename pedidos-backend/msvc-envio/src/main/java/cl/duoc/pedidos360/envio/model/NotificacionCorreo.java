package cl.duoc.pedidos360.envio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Resultado de la notificación por correo de un pedido.
 * Existe a lo más una por pedido: así un mensaje duplicado no vuelve a enviar el correo.
 */
@Entity
@Table(name = "notificaciones_correo")
public class NotificacionCorreo {

    public static final String ESTADO_ENVIADA = "ENVIADA";
    public static final String ESTADO_OMITIDA_SMTP_DESHABILITADO = "OMITIDA_SMTP_DESHABILITADO";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID pedidoId;

    @Column(nullable = false)
    private String numeroPedido;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String estado;

    @Column(nullable = false)
    private Instant creadoEn = Instant.now();

    public UUID getId() {
        return id;
    }

    public UUID getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(UUID pedidoId) {
        this.pedidoId = pedidoId;
    }

    public String getNumeroPedido() {
        return numeroPedido;
    }

    public void setNumeroPedido(String numeroPedido) {
        this.numeroPedido = numeroPedido;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
