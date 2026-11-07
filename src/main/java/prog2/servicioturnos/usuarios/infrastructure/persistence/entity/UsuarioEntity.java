package prog2.servicioturnos.usuarios.infrastructure.persistence.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String login;

    @Column(name = "contrasenia_hash", nullable = false)
    private String contraseniaHash;

    @Column(length = 50)
    private String nombre;

    @Column(length = 50)
    private String apellido;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "imagen_url", length = 256)
    private String imagenUrl;

    @Column(nullable = false)
    private boolean activado;

    @Column(nullable = false, length = 10)
    private String idioma;

    // Los roles se leen siempre junto con el usuario: son pocos y el JWT los necesita.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_rol", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "rol_nombre", nullable = false)
    private Set<String> roles = new HashSet<>();

    @Column(name = "creado_por", nullable = false, length = 50)
    private String creadoPor;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "modificado_por", length = 50)
    private String modificadoPor;

    @Column(name = "modificado_en")
    private Instant modificadoEn;
}
