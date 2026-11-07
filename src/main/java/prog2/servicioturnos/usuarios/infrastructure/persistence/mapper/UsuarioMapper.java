package prog2.servicioturnos.usuarios.infrastructure.persistence.mapper;

import java.util.HashSet;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import prog2.servicioturnos.usuarios.domain.model.Rol;
import prog2.servicioturnos.usuarios.domain.model.Usuario;
import prog2.servicioturnos.usuarios.infrastructure.persistence.entity.UsuarioEntity;

@Component
public class UsuarioMapper {

    public UsuarioEntity aEntidad(Usuario usuario) {
        UsuarioEntity entidad = new UsuarioEntity();
        entidad.setId(usuario.id());
        entidad.setLogin(usuario.login());
        entidad.setContraseniaHash(usuario.contraseniaHash());
        entidad.setNombre(usuario.nombre());
        entidad.setApellido(usuario.apellido());
        entidad.setEmail(usuario.email());
        entidad.setImagenUrl(usuario.imagenUrl());
        entidad.setActivado(usuario.activado());
        entidad.setIdioma(usuario.idioma());
        entidad.setRoles(usuario.roles().stream().map(Rol::nombre).collect(Collectors.toCollection(HashSet::new)));
        entidad.setCreadoPor(usuario.creadoPor());
        entidad.setCreadoEn(usuario.creadoEn());
        entidad.setModificadoPor(usuario.modificadoPor());
        entidad.setModificadoEn(usuario.modificadoEn());
        return entidad;
    }

    public Usuario aDominio(UsuarioEntity entidad) {
        return new Usuario(
                entidad.getId(),
                entidad.getLogin(),
                entidad.getContraseniaHash(),
                entidad.getNombre(),
                entidad.getApellido(),
                entidad.getEmail(),
                entidad.getImagenUrl(),
                entidad.isActivado(),
                entidad.getIdioma(),
                entidad.getRoles().stream().map(Rol::desdeNombre).collect(Collectors.toSet()),
                entidad.getCreadoPor(),
                entidad.getCreadoEn(),
                entidad.getModificadoPor(),
                entidad.getModificadoEn());
    }
}
