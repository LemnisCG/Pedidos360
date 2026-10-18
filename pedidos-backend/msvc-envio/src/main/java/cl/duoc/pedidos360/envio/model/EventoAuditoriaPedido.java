package cl.duoc.pedidos360.envio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * Una entrada del historial de auditoría de un pedido: qué le pasó (tipoEvento = routing key) y cuándo se registró.
 * Existe a lo más una por pedido y tipo de evento: así un mensaje duplicado no se registra dos veces.
 */
@Entity
@Table(
        name = "eventos_auditoria_pedido",
        uniqueConstraints = @UniqueConstraint(columnNames = {"pedidoId", "tipoEvento"})
)
public class EventoAuditoriaPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID pedidoId;

    @Column(nullable = false)
    private String numeroPedido;

    @Column(nullable = false)
    private String tipoEvento;

    @Column(nullable = false)
    private Instant registradoEn = Instant.now();

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

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public Instant getRegistradoEn() {
        return registradoEn;
    }
}
