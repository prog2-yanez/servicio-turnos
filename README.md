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

- El puerto de `SPRING_DATASOURCE_URL` tiene que coincidir con `DB_PORT`.
- El puerto por defecto es `5434` para no chocar con PostgreSQL instalado en la máquina (`5432`) ni con la base del catálogo (`5433`).
- La base y el usuario son solo de turnos: cada servicio tiene su propio contenedor, así que turnos no tiene acceso a la base del catálogo.
- `.env` no se versiona. Docker Compose lo lee solo; Spring Boot no, por eso se carga a mano al ejecutar el servicio (ver más abajo).

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
curl http://localhost:8080/actuator/health
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

### Cobertura

JaCoCo mide qué partes del código ejecutan las pruebas. Después de `./mvnw verify` el reporte queda en:

- `target/site/jacoco/index.html`: para verlo en el navegador.
- `target/site/jacoco/jacoco.xml`: el que lee SonarCloud en cada pull request.

SonarCloud no cuenta la clase `ServicioTurnosApplication` en la cobertura, porque solo arranca Spring y no tiene lógica propia.

## Base de datos y migraciones

- **Flyway es el único que crea o modifica el esquema.** Las migraciones están en `src/main/resources/db/migration/` y se nombran `V1__descripcion.sql`, `V2__descripcion.sql`, etc.
- Una migración ya aplicada no se edita: cada cambio es una migración nueva.
- Hibernate trabaja con `ddl-auto=validate`: solo comprueba que las entidades coincidan con el esquema y nunca lo modifica.
