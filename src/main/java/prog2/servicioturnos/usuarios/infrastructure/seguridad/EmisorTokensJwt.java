package prog2.servicioturnos.usuarios.infrastructure.seguridad;

import java.time.Clock;
import java.time.Instant;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;
import prog2.servicioturnos.config.JwtProperties;
import prog2.servicioturnos.usuarios.domain.model.Rol;
import prog2.servicioturnos.usuarios.domain.model.TokenAcceso;
import prog2.servicioturnos.usuarios.domain.model.Usuario;
import prog2.servicioturnos.usuarios.domain.ports.out.EmisorTokens;

// El JWT lleva el login (sub), el id del usuario y sus roles con el mismo formato que JHipster ("auth").
@Component
public class EmisorTokensJwt implements EmisorTokens {

    static final String EMISOR = "servicio-turnos";

    private final JwtEncoder codificador;
    private final JwtProperties propiedades;
    private final Clock reloj;

    public EmisorTokensJwt(JwtEncoder codificador, JwtProperties propiedades, Clock reloj) {
        this.codificador = codificador;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    @Override
    public TokenAcceso emitir(Usuario usuario) {
        Instant ahora = reloj.instant();
        Instant vence = ahora.plus(propiedades.duracion());
        JwtClaimsSet datos = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .subject(usuario.login())
                .issuedAt(ahora)
                .expiresAt(vence)
                .claim("userId", usuario.id())
                .claim("auth", usuario.roles().stream().map(Rol::nombre).sorted().collect(Collectors.joining(" ")))
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        String valor = codificador.encode(JwtEncoderParameters.from(cabecera, datos)).getTokenValue();
        return new TokenAcceso(valor, vence);
    }
}
