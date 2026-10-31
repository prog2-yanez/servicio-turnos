-- Cuentas de usuario de la app, compatibles con el usuario de JHipster.
-- login y email se guardan en minúsculas, así la unicidad no depende de mayúsculas.
-- La contraseña nunca se guarda: solo su hash Argon2id.

CREATE TABLE usuario (
    id               BIGSERIAL    PRIMARY KEY,
    login            VARCHAR(50)  NOT NULL UNIQUE,
    contrasenia_hash VARCHAR(255) NOT NULL,
    nombre           VARCHAR(50),
    apellido         VARCHAR(50),
    email            VARCHAR(254) NOT NULL UNIQUE,
    imagen_url       VARCHAR(256),
    activado         BOOLEAN      NOT NULL,
    idioma           VARCHAR(10)  NOT NULL,
    creado_por       VARCHAR(50)  NOT NULL,
    creado_en        TIMESTAMPTZ  NOT NULL,
    modificado_por   VARCHAR(50),
    modificado_en    TIMESTAMPTZ
);

CREATE TABLE rol (
    nombre VARCHAR(50) PRIMARY KEY
);

CREATE TABLE usuario_rol (
    usuario_id BIGINT      NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    rol_nombre VARCHAR(50) NOT NULL REFERENCES rol (nombre),
    PRIMARY KEY (usuario_id, rol_nombre)
);

INSERT INTO rol (nombre) VALUES ('ROLE_USER'), ('ROLE_ADMIN');
