package prog2.servicioturnos.usuarios.infrastructure.persistence.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import prog2.servicioturnos.usuarios.infrastructure.persistence.entity.UsuarioEntity;

public interface JpaUsuarioRepository extends JpaRepository<UsuarioEntity, Long> {

    boolean existsByLogin(String login);

    boolean existsByEmail(String email);

    Optional<UsuarioEntity> findByLogin(String login);
}
