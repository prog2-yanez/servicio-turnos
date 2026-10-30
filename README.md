<!-- calidad:inicio -->
![Calidad](https://img.shields.io/badge/Calidad-sin%20c%C3%B3digo-lightgrey) ![Aprobar](https://img.shields.io/badge/Aprobar-no-red) ![Destacar](https://img.shields.io/badge/Destacar-no-lightgrey)

**Calidad (7 dimensiones):** sin código en `main` (no evaluable).
<!-- calidad:fin -->

# Servicio de turnos

Servicio backend del sistema de turnos médicos. Maneja las cuentas de usuario, arma la disponibilidad de cada profesional y lleva adelante las reservas contra el servicio central de la cátedra.

Este repositorio contiene por ahora la base técnica del servicio: arranque con Spring Boot 4, PostgreSQL, migraciones con Flyway, endpoint de salud y pruebas de integración con una base real.

## Qué hace el servicio

- **Registra usuarios e inicia sesión**, con contraseñas guardadas con Argon2id y un JWT propio para la app.
- **Arma los turnos libres** de un profesional combinando su agenda, que pide al servicio de catálogo, con las ocupaciones que informa la cátedra.
- **Reserva turnos** contra la cátedra (REST y Kafka) y guarda el estado de cada proceso a nombre del usuario que lo inició.

El resto del sistema (servicio de catálogo, app y documentación) vive en repositorios aparte.

## Tecnologías

| Tecnología | Uso |
|---|---|
| Java 21 | Lenguaje |
| Spring Boot 4.1.2 | Aplicación web, JPA, validación y Actuator |
| Maven (wrapper `mvnw`) | Compilación y dependencias |
| PostgreSQL 17 | Base de datos |
| Flyway | Migraciones del esquema |
| Docker Compose | Base de datos local |
| Testcontainers | Pruebas contra un PostgreSQL real y temporal |
| Lombok | Menos código repetitivo |

## Requisitos

- JDK 21.
- Docker con Docker Compose (para la base local y para las pruebas).
- No hace falta instalar Maven: `./mvnw` descarga la versión correcta. En Windows se usa `mvnw.cmd`.

## Configuración

Copiar el archivo de ejemplo y cambiar las contraseñas:

```bash
cp .env.example .env
```

| Variable | Para qué sirve |
|---|---|
| `POSTGRES_DB` | Nombre de la base de turnos |
| `POSTGRES_USER` | Usuario de esa base |
| `POSTGRES_PASSWORD` | Contraseña de ese usuario |
| `DB_PORT` | Puerto local donde se publica PostgreSQL (por defecto `5434`) |
| `SPRING_DATASOURCE_URL` | URL de conexión del servicio, por ejemplo `jdbc:postgresql://localhost:5434/turnos` |
| `SPRING_DATASOURCE_USERNAME` | Usuario con el que se conecta el servicio |
| `SPRING_DATASOURCE_PASSWORD` | Contraseña con la que se conecta el servicio |
| `SERVER_PORT` | Puerto del servicio (por defecto `8081`, para no chocar con el catálogo en `8080`) |
| `CATALOGO_API_URL` | URL del servicio de catálogo (por defecto `http://localhost:8080`) |
| `JWT_SECRETO` | Clave con la que turnos firma el JWT de los usuarios (HS256). Mínimo 32 caracteres |
| `JWT_DURACION` | Cuánto dura el JWT de los usuarios (por defecto `1h`) |

- El puerto de `SPRING_DATASOURCE_URL` tiene que coincidir con `DB_PORT`.
- El puerto por defecto es `5434` para no chocar con PostgreSQL instalado en la máquina (`5432`) ni con la base del catálogo (`5433`).
- La base y el usuario son solo de turnos: cada servicio tiene su propio contenedor, así que turnos no tiene acceso a la base del catálogo.
- `JWT_SECRETO` se genera una vez con `openssl rand -base64 48`. Si falta o tiene menos de 32 caracteres, el servicio no arranca. El catálogo usa la misma clave para validar los tokens.
- `.env` no se versiona. Docker Compose lo lee solo; Spring Boot no, por eso se carga a mano al ejecutar el servicio (ver más abajo).

## Cuentas de usuario

Turnos es el dueño de las cuentas de la app y emite su JWT. Las contraseñas se guardan con **Argon2id** y nunca aparecen en respuestas ni en logs.

### Registrarse

```bash
curl -X POST http://localhost:8081/api/v1/cuenta/registro \
  -H "Content-Type: application/json" \
  -d '{"login": "ana", "password": "una-clave", "firstName": "Ana", "lastName": "Gomez",
       "email": "ana@mail.com", "langKey": "es"}'
```

| Campo | Regla |
|---|---|
| `login` | Obligatorio, hasta 50 caracteres: letras, números y `_.@-` |
| `password` | Obligatoria, entre 4 y 100 caracteres |
| `email` | Obligatorio y válido, entre 5 y 254 caracteres |
| `firstName`, `lastName` | Opcionales, hasta 50 caracteres |
| `imageUrl` | Opcional, hasta 256 caracteres |
| `langKey` | Opcional, entre 2 y 10 caracteres (por defecto `es`) |

- Responde `201` con `{"id_token": "...", "venceEn": "..."}`: la cuenta queda activa y con la sesión iniciada.
- `login` y `email` se guardan en minúsculas. Si ya existen, responde `409` (`login-repetido` o `email-repetido`).
- Id, estado, roles y auditoría los decide el servicio: la cuenta nace activa y con `ROLE_USER`.

### El JWT de los usuarios

| Dato | Contenido |
|---|---|
| `sub` | login del usuario |
| `userId` | id del usuario en turnos |
| `auth` | roles separados por espacio, por ejemplo `ROLE_USER` |
| `iss` | `servicio-turnos` |
| `iat`, `exp` | emisión y vencimiento |

Es distinto del JWT técnico de la cátedra: ese solo lo usa el backend y nunca llega a la app.

## Errores

Todos los errores responden `application/problem+json` (RFC 7807) con `type`, `title`, `status`, `detail` e `instance`. El `type` es `urn:turnos:error:<codigo>`, por ejemplo `urn:turnos:error:datos-invalidos`. Ningún error muestra trazas ni datos internos.

## Integración con la cátedra

El servicio reserva turnos contra el servicio central de la cátedra por REST y Kafka. Usa la misma cuenta técnica que el servicio de catálogo: se da de alta una sola vez, a mano, y los dos servicios comparten el JWT técnico (contrato de integración, sección 2).

La respuesta del alta (`POST /api/student/register`) o de `GET /api/student/integration` trae el `id_token` y el objeto `integration`. Esos valores se copian al `.env`:

| Variable | Campo de la respuesta |
|---|---|
| `CATEDRA_API_URL` | URL base de la API (la entrega la cátedra) |
| `CATEDRA_JWT` | `id_token` |
| `CATEDRA_KAFKA_BOOTSTRAP_SERVERS` | `integration.kafkaBootstrapServers` |
| `CATEDRA_KAFKA_CONSUMER_GROUP` | `integration.kafkaConsumerGroupId` |
| `CATEDRA_KAFKA_TOPIC_ACCIONES` | `integration.kafkaAppointmentActionsTopic` (eventos que publica la cátedra sobre cada reserva) |
| `CATEDRA_KAFKA_TOPIC_TELEFONO` | `integration.kafkaAppointmentPhoneTopic` (donde turnos publica el teléfono del paciente) |

- Estos valores son credenciales: van solo en `.env` y nunca en el código, en los logs ni en este README.
- El JWT técnico lo usa solo el backend para hablar con la cátedra; nunca llega a la app.
- Si las variables están vacías el servicio arranca igual, pero no puede reservar turnos.

## Base de datos con Docker Compose

```bash
docker compose up -d
docker compose ps
```

Levanta PostgreSQL con los datos de `.env`. Cuando `docker compose ps` muestra el contenedor como `healthy`, la base ya acepta conexiones. Los datos se guardan en el volumen `turnos-postgres-data`, así que se conservan al detener o recrear el contenedor.

```bash
docker compose down        # detiene el contenedor y conserva los datos
docker compose down -v     # además borra los datos
```

Cambiar las contraseñas en `.env` no modifica las de una base ya creada: hay que borrar el volumen o cambiarlas dentro de PostgreSQL.

## Ejecutar el servicio

Con la base levantada, cargar las variables y ejecutar:

```bash
set -a
. ./.env
set +a
./mvnw spring-boot:run
```

Desde IntelliJ se pueden definir las variables `SPRING_DATASOURCE_*` en la configuración de ejecución de `ServicioTurnosApplication`.

### Comprobar que funciona

```bash
curl http://localhost:8081/actuator/health
```

La respuesta esperada es `{"status":"UP"}`: el servicio está en marcha y llega a la base. Es el único endpoint de Actuator que se expone y no muestra detalles internos.

## Pruebas

```bash
./mvnw verify
```

Compila, ejecuta las pruebas y genera el reporte de cobertura. Las pruebas levantan un PostgreSQL 17 temporal con Testcontainers, por lo que Docker tiene que estar en marcha. No usan `.env` ni la base de desarrollo.

| Prueba | Qué comprueba |
|---|---|
| `ServicioTurnosApplicationTests` | Que la aplicación arranca |
| `MigracionFlywayTests` | Que Flyway aplica las migraciones y no queda ninguna pendiente |
| `EndpointSaludTests` | Que `/actuator/health` responde `UP` sin mostrar detalles internos |
| `ManejadorErroresApiTests` | El formato ProblemDetail de cada tipo de error |
| `UsuarioTests`, `RegistrarUsuarioServiceTests` | Las reglas del registro: cuenta activa con `ROLE_USER`, login y email repetidos |
| `CifradorArgon2idTests`, `EmisorTokensJwtTests`, `JwtPropertiesTests` | Hash Argon2id, contenido y firma del JWT, clave mínima |
| `UsuarioPersistenciaTests` | Guardar y buscar usuarios contra PostgreSQL |
| `CuentaControllerTests` | `POST /api/v1/cuenta/registro`: 201, 400 y 409 |

### Cobertura

JaCoCo mide qué partes del código ejecutan las pruebas. Después de `./mvnw verify` el reporte queda en:

- `target/site/jacoco/index.html`: para verlo en el navegador.
- `target/site/jacoco/jacoco.xml`: el que lee SonarCloud en cada pull request.

SonarCloud no cuenta la clase `ServicioTurnosApplication` en la cobertura, porque solo arranca Spring y no tiene lógica propia.

## Base de datos y migraciones

- **Flyway es el único que crea o modifica el esquema.** Las migraciones están en `src/main/resources/db/migration/` y se nombran `V1__descripcion.sql`, `V2__descripcion.sql`, etc.
- Una migración ya aplicada no se edita: cada cambio es una migración nueva.
- Hibernate trabaja con `ddl-auto=validate`: solo comprueba que las entidades coincidan con el esquema y nunca lo modifica.
