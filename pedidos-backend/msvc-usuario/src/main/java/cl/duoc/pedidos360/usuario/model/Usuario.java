package cl.duoc.pedidos360.usuario.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Perfil local de un usuario autenticado en Microsoft Entra External ID.
 * Las credenciales viven en Entra; aquí solo se guarda lo que Pedidos360 necesita mostrar.
 */
@Entity
@Table(name = "usuarios", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class Usuario {
    public static final String ENTRA_EXTERNAL_ID_IDENTITY_PROVIDER = "entra-external-id";

    /** Es el claim "oid" del token: el mismo identificador que usan carrito y pedidos. */
    @Id
    private UUID id;
    @Column(nullable = false, length = 80) private String nombre;
    @Column(nullable = false, length = 80) private String apellido;
    @Column(nullable = false, unique = true, length = 180) private String email;
    @Column(name = "provider", nullable = false, length = 30)
    private String identityProvider = ENTRA_EXTERNAL_ID_IDENTITY_PROVIDER;
    @Column(nullable = false) private Instant creadoEn = Instant.now();

    protected Usuario() {
    }

    public Usuario(UUID entraUserObjectId) {
        this.id = entraUserObjectId;
    }

    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getIdentityProvider() { return identityProvider; }
    public Instant getCreadoEn() { return creadoEn; }
}
