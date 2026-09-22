package cl.duoc.pedidos360.perfil.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "perfiles")
public class Perfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_perfil")
    private Long idPerfil;

    @Column(name = "id_provider")
    private UUID idProvider;

    @Column(name = "email")
    private String email;
 
    @Column(name = "nombre")
    private String nombre;
    
    @Column(name = "apellido")
    private String apellido;
    
    @Column(name = "direccion_envio")
    private String direccionEnvio;
    
    @Column(name = "telefono")
    private String telefono;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
   
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;


    protected Perfil() {
    }

    public Perfil(UUID idProvider, String email, String nombre, String apellido,
                  String direccionEnvio, String telefono) {
        this.idProvider = idProvider;
        this.email = email;
        this.nombre = nombre;
        this.apellido = apellido;
        this.direccionEnvio = direccionEnvio;
        this.telefono = telefono;
    }

    @PrePersist
    private void beforeInsert() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return idPerfil;
    }
    public UUID getIdProvider() {
        return idProvider;
    }
    public String getEmail() {
        return email;
    }
    public String getNombre() {
        return nombre;
    }
    public String getApellido() {
        return apellido;
    }
    public String getDireccionEnvio() {
        return direccionEnvio;
    }
    public String getTelefono() {
        return telefono;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
}