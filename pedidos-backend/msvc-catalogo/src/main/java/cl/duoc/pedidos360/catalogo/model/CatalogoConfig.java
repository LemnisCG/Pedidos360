package cl.duoc.pedidos360.catalogo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Configuración editable del banner principal del catálogo. */
@Entity
@Table(name = "catalogo_config")
public class CatalogoConfig {

    @Id
    private Long id = 1L;

    private String etiqueta;
    private String titulo;

    @Column(length = 500)
    private String subtitulo;

    protected CatalogoConfig() {
    }

    public CatalogoConfig(String etiqueta, String titulo, String subtitulo) {
        this.etiqueta = etiqueta;
        this.titulo = titulo;
        this.subtitulo = subtitulo;
    }

    public Long getId() {
        return id;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getSubtitulo() {
        return subtitulo;
    }
}
