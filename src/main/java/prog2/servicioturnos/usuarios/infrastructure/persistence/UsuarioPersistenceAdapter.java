package prog2.servicioturnos.usuarios.infrastructure.persistence;

import org.springframework.stereotype.Component;
import prog2.servicioturnos.usuarios.domain.model.Usuario;
import prog2.servicioturnos.usuarios.domain.ports.out.UsuarioRepository;
import prog2.servicioturnos.usuarios.infrastructure.persistence.mapper.UsuarioMapper;
import prog2.servicioturnos.usuarios.infrastructure.persistence.repository.JpaUsuarioRepository;

@Component
public class UsuarioPersistenceAdapter implements UsuarioRepository {

    private final JpaUsuarioRepository usuarios;
    private final UsuarioMapper mapper;

    public UsuarioPersistenceAdapter(JpaUsuarioRepository usuarios, UsuarioMapper mapper) {
        this.usuarios = usuarios;
        this.mapper = mapper;
    }

    @Override
    public boolean existeLogin(String login) {
        return usuarios.existsByLogin(login);
    }

    @Override
    public boolean existeEmail(String email) {
        return usuarios.existsByEmail(email);
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        return mapper.aDominio(usuarios.save(mapper.aEntidad(usuario)));
    }
}
