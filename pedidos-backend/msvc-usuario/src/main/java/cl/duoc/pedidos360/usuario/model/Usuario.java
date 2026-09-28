package cl.duoc.pedidos360.usuario.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/** Usuario unificado: puede provenir del formulario local o de un proveedor OAuth. */
@Entity
@Table(name = "usuarios", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, length = 80) private String nombre;
    @Column(nullable = false, length = 80) private String apellido;
    @Column(nullable = false, unique = true, length = 180) private String email;
    private String passwordHash;
    @Column(nullable = false, length = 30) private String provider;
    private String providerUserId;
    @Column(nullable = false) private Instant creadoEn = Instant.now();

    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellido() { return apellido; }
    public void setApellido(String apellido) { this.apellido = apellido; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getProviderUserId() { return providerUserId; }
    public void setProviderUserId(String providerUserId) { this.providerUserId = providerUserId; }
    public Instant getCreadoEn() { return creadoEn; }
    public String nombreCompleto() { return (nombre + " " + apellido).trim(); }
}
