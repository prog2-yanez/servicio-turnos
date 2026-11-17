<!-- evaluacion:inicio -->
# Evaluación — prog2-yanez/servicio-turnos

> Documento **generado automáticamente** por la cátedra de Programación II (UM): RepoSense + análisis de calidad de **7 dimensiones**. **No editar a mano**: se regenera en cada evaluación.
> Rama evaluada: `main`. Análisis **heurístico** (regex + métricas) + build: es una criba para orientar la revisión, **no una nota**. La **documentación** del repositorio no se evalúa.
> Generado: 2026-11-17.

## 1. Veredicto

| Aspecto | Resultado |
|---|---|
| Índice de calidad | **76/100** |
| Tipo de repositorio | `backend` |
| Build (`mvnw verify` / `gradlew check`) | OK |
| Código (LOC) | 8,186 |
| Archivos de código | 202 |
| Clases | 220 |
| Código dentro de clases (%POO) | 72% |
| Funciones fuera de clases | 0 |
| Duplicación (puerta) | 0.4% |
| Duplicación lógica | 0.4% |
| Duplicación de boilerplate | 7.8% |
| Cobertura | 99% |
| Tope duro aplicado a D1 | `POO_BAJO` → D1 ≤ 60 |

> Este informe **describe** lo que el análisis midió con los criterios ya establecidos de la materia. No es una nota.

## 2. Semáforo de aprobación

Criterios de la cátedra (`CRITERIOS-CALIDAD.md`): **Aprobar** se juega con el perfil `estandar`; **destacar**, con el `exigente` (meta de excelencia).

| Semáforo | Resultado |
|---|---|
| Aprobar (`estandar`) | **no cumple** — 7/11 |
| Destacar (`exigente`) | **no cumple** — 6/11 |

**Para aprobar, falta cumplir:** SOLID

**Para destacar, falta cumplir:** SOLID; POO

## 3. Autoría (RepoSense)

| Autor | Commits | Código (LOC) | Tests (LOC) | Docs (LOC) | Días activos | % del código |
|---|---:|---:|---:|---:|---:|---:|
| Guadalupe Yañez | 86 | 4,548 | 5,277 | 734 | 15 | 100% |

> _El LOC de autoría lo calcula RepoSense sobre el historial y puede diferir del LOC de código del análisis (que mira el estado de `main`)._

## 4. Dimensiones evaluadas

Cada dimensión indica **qué mide**, **por qué importa** y **tu resultado**. Cuando el puntaje queda por debajo de la referencia, se agregan preguntas para **investigar la causa** (no hay recetas).

| Dimensión | Peso | Puntaje |
|:--|--:|--:|
| **D1** Diseño OO, SOLID y patrones (20%) | 20% | 3 |
| **D2** Arquitectura hexagonal — backend (15%) | 15% | 100 |
| **D3** Frontend KMP: MVVM y buenas prácticas (10%) | 10% | N/A |
| **D4** Integración y contrato (10%) | 10% | 80 |
| **D5** Seguridad y robustez (10%) | 10% | 100 |
| **D6** Pruebas y cobertura (20%) | 20% | 100 |
| **D7** Proceso y entrega (15%) | 15% | 100 |

### D1. Diseño OO, SOLID y patrones (20%)

- **Qué mide:** Métricas orientadas a objetos (clases, métodos, campos, cohesión, acoplamiento, herencia) y proxies de SOLID, más patrones de diseño.
- **Por qué importa:** El eje de la materia es el buen uso de la POO y su arquitectura: un diseño con clases claras y poco acopladas es más fácil de entender, probar y cambiar.
- **Tu resultado:** **3/100**
- **Para investigar la causa:**
    - ¿Se pensó el modelo de clases antes de codificar o se agrupó después?
    - ¿Qué clases concentran muchas responsabilidades a la vez?
    - ¿Los colaboradores llegan como parámetros o se crean por dentro?

### D2. Arquitectura hexagonal — backend (15%)

- **Qué mide:** Separación en capas (dominio, puertos de entrada/salida, adaptadores) y dirección de las dependencias del backend.
- **Por qué importa:** Aislar el dominio de la infraestructura permite cambiar la base de datos, el framework o el transporte sin tocar las reglas de negocio.
- **Tu resultado:** **100/100**

### D4. Integración y contrato (10%)

- **Qué mide:** Cumplimiento del contrato de integración de la cátedra (formato de errores, paginación, autenticación, endpoints esperados).
- **Por qué importa:** Los servicios deben poder integrarse entre sí y con la app sin sorpresas; el contrato es lo que hacen cumplir las pruebas de integración.
- **Tu resultado:** **80/100**

### D5. Seguridad y robustez (10%)

- **Qué mide:** Credenciales embebidas, consultas SQL por concatenación, y presencia de config de entorno; aislamiento de secretos.
- **Por qué importa:** Un secreto en el repositorio o una consulta construida por concatenación son riesgos concretos, no hipotéticos.
- **Tu resultado:** **100/100**

### D6. Pruebas y cobertura (20%)

- **Qué mide:** Cantidad y resultado de pruebas y cobertura de línea (JaCoCo/Kover, Surefire/Failsafe) cuando hay build.
- **Por qué importa:** Las pruebas son la red de seguridad para refactorizar e integrar; la cobertura acota las zonas sin verificar.
- **Tu resultado:** **100/100**

### D7. Proceso y entrega (15%)

- **Qué mide:** Artefactos verificables de proceso/entrega: `compose`, `.env.example`, CI (`.github/workflows/`), `CODEOWNERS` y wrapper de build (`mvnw`/`gradlew`). **No** puntúa documentación.
- **Por qué importa:** Un proyecto que compila, se levanta y se valida solo es más fácil de revisar y de integrar en equipo.
- **Tu resultado:** **100/100**

## 5. Señales detectadas

El análisis encontró las siguientes señales (archivo, sin número de línea a propósito):

**DIP/concretos** (47)
- clase 'EmisorTokensJwt' muy acoplada (CBO=14) — `src/main/java/prog2/servicioturnos/usuarios/infrastructure/seguridad/EmisorTokensJwt.java`
- clase 'CuentaController' muy acoplada (CBO=11) — `src/main/java/prog2/servicioturnos/usuarios/infrastructure/web/CuentaController.java`
- clase 'IniciarSesionService' muy acoplada (CBO=10) — `src/main/java/prog2/servicioturnos/usuarios/application/usecases/IniciarSesionService.java`
- clase 'RegistrarUsuarioService' muy acoplada (CBO=10) — `src/main/java/prog2/servicioturnos/usuarios/application/usecases/RegistrarUsuarioService.java`
- clase 'OcupacionesCatedraRestAdapter' muy acoplada (CBO=37) — `src/main/java/prog2/servicioturnos/disponibilidad/infrastructure/catedra/OcupacionesCatedraRestAdapter.java`
- clase 'OcupacionesCatedraRestAdapter' instancia 5 concretos (CBO=37) — `src/main/java/prog2/servicioturnos/disponibilidad/infrastructure/catedra/OcupacionesCatedraRestAdapter.java`
- clase 'CatalogoRestAdapter' muy acoplada (CBO=29) — `src/main/java/prog2/servicioturnos/disponibilidad/infrastructure/catalogo/CatalogoRestAdapter.java`
- clase 'CatalogoRestAdapter' instancia 7 concretos (CBO=29) — `src/main/java/prog2/servicioturnos/disponibilidad/infrastructure/catalogo/CatalogoRestAdapter.java`
- clase 'DisponibilidadController' muy acoplada (CBO=9) — `src/main/java/prog2/servicioturnos/disponibilidad/infrastructure/web/DisponibilidadController.java`
- clase 'ConsultarTurnosLibresService' muy acoplada (CBO=14) — `src/main/java/prog2/servicioturnos/disponibilidad/application/usecases/ConsultarTurnosLibresService.java`
- clase 'BuscarProfesionalesConTurnosLibresService' muy acoplada (CBO=17) — `src/main/java/prog2/servicioturnos/disponibilidad/application/usecases/BuscarProfesionalesConTurnosLibresService.java`
- clase 'CalculadoraTurnosLibres' muy acoplada (CBO=15) — `src/main/java/prog2/servicioturnos/disponibilidad/domain/service/CalculadoraTurnosLibres.java`
- clase 'CorsConfig' muy acoplada (CBO=27) — `src/main/java/prog2/servicioturnos/config/CorsConfig.java`
- clase 'CorsConfig' instancia 4 concretos (CBO=27) — `src/main/java/prog2/servicioturnos/config/CorsConfig.java`
- clase 'RechazoComoProblema' muy acoplada (CBO=12) — `src/main/java/prog2/servicioturnos/config/CorsConfig.java`
- clase 'JwtConfig' muy acoplada (CBO=14) — `src/main/java/prog2/servicioturnos/config/JwtConfig.java`
- clase 'CatedraConfig' muy acoplada (CBO=8) — `src/main/java/prog2/servicioturnos/config/CatedraConfig.java`
- clase 'SecurityConfig' muy acoplada (CBO=19) — `src/main/java/prog2/servicioturnos/config/SecurityConfig.java`
- clase 'LectorErrorCatedra' muy acoplada (CBO=10) — `src/main/java/prog2/servicioturnos/shared/catedra/LectorErrorCatedra.java`
- clase 'ManejadorErroresApi' muy acoplada (CBO=65) — `src/main/java/prog2/servicioturnos/shared/web/ManejadorErroresApi.java`
- clase 'ManejadorErroresApi' instancia 5 concretos (CBO=65) — `src/main/java/prog2/servicioturnos/shared/web/ManejadorErroresApi.java`
- clase 'HoldsCatedraRestAdapter' muy acoplada (CBO=39) — `src/main/java/prog2/servicioturnos/reservas/infrastructure/catedra/HoldsCatedraRestAdapter.java`
- clase 'HoldsCatedraRestAdapter' instancia 13 concretos (CBO=39) — `src/main/java/prog2/servicioturnos/reservas/infrastructure/catedra/HoldsCatedraRestAdapter.java`
- clase 'ReservasCatedraRestAdapter' muy acoplada (CBO=41) — `src/main/java/prog2/servicioturnos/reservas/infrastructure/catedra/ReservasCatedraRestAdapter.java`
- clase 'ReservasCatedraRestAdapter' instancia 9 concretos (CBO=41) — `src/main/java/prog2/servicioturnos/reservas/infrastructure/catedra/ReservasCatedraRestAdapter.java`
- clase 'ProcesoReservaPersistenceAdapter' muy acoplada (CBO=8) — `src/main/java/prog2/servicioturnos/reservas/infrastructure/persistence/ProcesoReservaPersistenceAdapter.java`
- clase 'JpaEventoProcesadoRepository' muy acoplada (CBO=11) — `src/main/java/prog2/servicioturnos/reservas/infrastructure/persistence/repository/JpaEventoProcesadoRepository.java`
- clase 'ProcesoReservaEntity' muy acoplada (CBO=8) — `src/main/java/prog2/servicioturnos/reservas/infrastructure/persistence/entity/ProcesoReservaEntity.java`
- clase 'AccionesCatedraListener' muy acoplada (CBO=18) — `src/main/java/prog2/servicioturnos/reservas/infrastructure/messaging/AccionesCatedraListener.java`
- clase 'KafkaConfig' muy acoplada (CBO=8) — `src/main/java/prog2/servicioturnos/reservas/infrastructure/messaging/KafkaConfig.java`
- clase 'PublicadorTelefonoKafka' muy acoplada (CBO=21) — `src/main/java/prog2/servicioturnos/reservas/infrastructure/messaging/PublicadorTelefonoKafka.java`
- clase 'PublicadorTelefonoKafka' instancia 3 concretos (CBO=21) — `src/main/java/prog2/servicioturnos/reservas/infrastructure/messaging/PublicadorTelefonoKafka.java`
- clase 'ReservaController' muy acoplada (CBO=22) — `src/main/java/prog2/servicioturnos/reservas/infrastructure/web/ReservaController.java`
- clase 'CancelarReservaService' muy acoplada (CBO=26) — `src/main/java/prog2/servicioturnos/reservas/application/usecases/CancelarReservaService.java`
- clase 'CancelarReservaService' instancia 4 concretos (CBO=26) — `src/main/java/prog2/servicioturnos/reservas/application/usecases/CancelarReservaService.java`
- clase 'RecuperarProcesosService' muy acoplada (CBO=28) — `src/main/java/prog2/servicioturnos/reservas/application/usecases/RecuperarProcesosService.java`
- clase 'ProcesarAccionesCatedraService' muy acoplada (CBO=15) — `src/main/java/prog2/servicioturnos/reservas/application/usecases/ProcesarAccionesCatedraService.java`
- clase 'ListarReservasPropiasService' muy acoplada (CBO=30) — `src/main/java/prog2/servicioturnos/reservas/application/usecases/ListarReservasPropiasService.java`
- clase 'ListarReservasPropiasService' instancia 4 concretos (CBO=30) — `src/main/java/prog2/servicioturnos/reservas/application/usecases/ListarReservasPropiasService.java`
- clase 'EnviarTelefonoService' muy acoplada (CBO=14) — `src/main/java/prog2/servicioturnos/reservas/application/usecases/EnviarTelefonoService.java`
- clase 'EnviarTelefonoService' instancia 4 concretos (CBO=14) — `src/main/java/prog2/servicioturnos/reservas/application/usecases/EnviarTelefonoService.java`
- clase 'IniciarReservaService' muy acoplada (CBO=26) — `src/main/java/prog2/servicioturnos/reservas/application/usecases/IniciarReservaService.java`
- clase 'IniciarReservaService' instancia 3 concretos (CBO=26) — `src/main/java/prog2/servicioturnos/reservas/application/usecases/IniciarReservaService.java`
- clase 'ReservasCatedra' muy acoplada (CBO=8) — `src/main/java/prog2/servicioturnos/reservas/domain/ports/out/ReservasCatedra.java`
- clase 'ProcesoReserva' muy acoplada (CBO=28) — `src/main/java/prog2/servicioturnos/reservas/domain/model/ProcesoReserva.java`
- clase 'ProcesoReserva' instancia 15 concretos (CBO=28) — `src/main/java/prog2/servicioturnos/reservas/domain/model/ProcesoReserva.java`
- clase 'EstadoProceso' muy acoplada (CBO=13) — `src/main/java/prog2/servicioturnos/reservas/domain/model/EstadoProceso.java`

**SRP/responsabilidad** (26)
- clase 'UsuarioEntity' grande (LOC=34, métodos=0, campos=14) — `src/main/java/prog2/servicioturnos/usuarios/infrastructure/persistence/entity/UsuarioEntity.java`
- clase 'OcupacionesCatedraRestAdapter' con baja cohesión (8%) — `src/main/java/prog2/servicioturnos/disponibilidad/infrastructure/catedra/OcupacionesCatedraRestAdapter.java`
- clase 'CatalogoRestAdapter' grande (LOC=101, métodos=16, campos=6) — `src/main/java/prog2/servicioturnos/disponibilidad/infrastructure/catalogo/CatalogoRestAdapter.java`
- clase 'CatalogoRestAdapter' con baja cohesión (5%) — `src/main/java/prog2/servicioturnos/disponibilidad/infrastructure/catalogo/CatalogoRestAdapter.java`
- método 'ProfesionalConTurnosLibres.ProfesionalConTurnosLibres' con 5 parámetros — `src/main/java/prog2/servicioturnos/disponibilidad/domain/model/ProfesionalConTurnosLibres.java`
- clase 'ManejadorErroresApi' grande (LOC=175, métodos=12, campos=5) — `src/main/java/prog2/servicioturnos/shared/web/ManejadorErroresApi.java`
- clase 'ManejadorErroresApi' con baja cohesión (0%) — `src/main/java/prog2/servicioturnos/shared/web/ManejadorErroresApi.java`
- clase 'HoldsCatedraRestAdapter' grande (LOC=73, métodos=10, campos=5) — `src/main/java/prog2/servicioturnos/reservas/infrastructure/catedra/HoldsCatedraRestAdapter.java`
- clase 'HoldsCatedraRestAdapter' con baja cohesión (9%) — `src/main/java/prog2/servicioturnos/reservas/infrastructure/catedra/HoldsCatedraRestAdapter.java`
- método 'HoldsCatedraRestAdapter.traducirError' con complejidad 6 — `src/main/java/prog2/servicioturnos/reservas/infrastructure/catedra/HoldsCatedraRestAdapter.java`
- clase 'ReservasCatedraRestAdapter' grande (LOC=138, métodos=18, campos=8) — `src/main/java/prog2/servicioturnos/reservas/infrastructure/catedra/ReservasCatedraRestAdapter.java`
- clase 'ReservasCatedraRestAdapter' con baja cohesión (15%) — `src/main/java/prog2/servicioturnos/reservas/infrastructure/catedra/ReservasCatedraRestAdapter.java`
- método 'ReservasCatedraRestAdapter.deUnPaciente' con complejidad 6 — `src/main/java/prog2/servicioturnos/reservas/infrastructure/catedra/ReservasCatedraRestAdapter.java`
- clase 'ProcesoReservaEntity' grande (LOC=34, métodos=0, campos=15) — `src/main/java/prog2/servicioturnos/reservas/infrastructure/persistence/entity/ProcesoReservaEntity.java`
- clase 'AccionesCatedraListener' grande (LOC=44, métodos=3, campos=9) — `src/main/java/prog2/servicioturnos/reservas/infrastructure/messaging/AccionesCatedraListener.java`
- método 'AccionesCatedraListener.recibir' con complejidad 9 — `src/main/java/prog2/servicioturnos/reservas/infrastructure/messaging/AccionesCatedraListener.java`
- método 'AccionesCatedraListener.EventoCatedra' con complejidad 7 — `src/main/java/prog2/servicioturnos/reservas/infrastructure/messaging/AccionesCatedraListener.java`
- clase 'CancelarReservaService' con baja cohesión (47%) — `src/main/java/prog2/servicioturnos/reservas/application/usecases/CancelarReservaService.java`
- clase 'RecuperarProcesosService' grande (LOC=99, métodos=10, campos=6) — `src/main/java/prog2/servicioturnos/reservas/application/usecases/RecuperarProcesosService.java`
- clase 'RecuperarProcesosService' con baja cohesión (33%) — `src/main/java/prog2/servicioturnos/reservas/application/usecases/RecuperarProcesosService.java`
- clase 'ListarReservasPropiasService' grande (LOC=75, métodos=10, campos=6) — `src/main/java/prog2/servicioturnos/reservas/application/usecases/ListarReservasPropiasService.java`
- clase 'ListarReservasPropiasService' con baja cohesión (2%) — `src/main/java/prog2/servicioturnos/reservas/application/usecases/ListarReservasPropiasService.java`
- clase 'IniciarReservaService' con baja cohesión (10%) — `src/main/java/prog2/servicioturnos/reservas/application/usecases/IniciarReservaService.java`
- método 'IniciarReservaService.iniciar' de 32 líneas — `src/main/java/prog2/servicioturnos/reservas/application/usecases/IniciarReservaService.java`
- método 'IniciarReservaService.iniciar' con complejidad 6 — `src/main/java/prog2/servicioturnos/reservas/application/usecases/IniciarReservaService.java`
- clase 'ProcesoReserva' grande (LOC=148, métodos=20, campos=2) — `src/main/java/prog2/servicioturnos/reservas/domain/model/ProcesoReserva.java`

**?** (27)
- clase 'LoginRepetidoException' con herencia profunda (DIT=3) — `src/main/java/prog2/servicioturnos/usuarios/domain/exception/LoginRepetidoException.java`
- clase 'EmailRepetidoException' con herencia profunda (DIT=3) — `src/main/java/prog2/servicioturnos/usuarios/domain/exception/EmailRepetidoException.java`
- clase 'CredencialesInvalidasException' con herencia profunda (DIT=3) — `src/main/java/prog2/servicioturnos/usuarios/domain/exception/CredencialesInvalidasException.java`
- clase 'CuentaNoEncontradaException' con herencia profunda (DIT=3) — `src/main/java/prog2/servicioturnos/usuarios/domain/exception/CuentaNoEncontradaException.java`
- clase 'ProfesionalNoEncontradoException' con herencia profunda (DIT=3) — `src/main/java/prog2/servicioturnos/disponibilidad/domain/exception/ProfesionalNoEncontradoException.java`
- clase 'CatedraNoDisponibleException' con herencia profunda (DIT=3) — `src/main/java/prog2/servicioturnos/disponibilidad/domain/exception/CatedraNoDisponibleException.java`
- clase 'CatalogoNoDisponibleException' con herencia profunda (DIT=3) — `src/main/java/prog2/servicioturnos/disponibilidad/domain/exception/CatalogoNoDisponibleException.java`
- clase 'FechaPasadaException' con herencia profunda (DIT=3) — `src/main/java/prog2/servicioturnos/disponibilidad/domain/exception/FechaPasadaException.java`
- clase 'ProfesionalDeshabilitadoException' con herencia profunda (DIT=3) — `src/main/java/prog2/servicioturnos/disponibilidad/domain/exception/ProfesionalDeshabilitadoException.java`
- clase 'ServicioNoDisponibleException' con herencia profunda (DIT=2) — `src/main/java/prog2/servicioturnos/shared/exception/ServicioNoDisponibleException.java`
- clase 'PedidoInvalidoException' con herencia profunda (DIT=2) — `src/main/java/prog2/servicioturnos/shared/exception/PedidoInvalidoException.java`
- clase 'ConflictoException' con herencia profunda (DIT=2) — `src/main/java/prog2/servicioturnos/shared/exception/ConflictoException.java`
- clase 'NoAutenticadoException' con herencia profunda (DIT=2) — `src/main/java/prog2/servicioturnos/shared/exception/NoAutenticadoException.java`
- clase 'RecursoNoEncontradoException' con herencia profunda (DIT=2) — `src/main/java/prog2/servicioturnos/shared/exception/RecursoNoEncontradoException.java`
- clase 'TelefonoNoPedidoException' con herencia profunda (DIT=3) — `src/main/java/prog2/servicioturnos/reservas/domain/exception/TelefonoNoPedidoException.java`
- clase 'DatosDeCuentaIncompletosException' con herencia profunda (DIT=3) — `src/main/java/prog2/servicioturnos/reservas/domain/exception/DatosDeCuentaIncompletosException.java`
- clase 'ProcesoNoEncontradoException' con herencia profunda (DIT=3) — `src/main/java/prog2/servicioturnos/reservas/domain/exception/ProcesoNoEncontradoException.java`
- clase 'EnvioNoConfirmadoException' con herencia profunda (DIT=3) — `src/main/java/prog2/servicioturnos/reservas/domain/exception/EnvioNoConfirmadoException.java`
- clase 'TurnoNoDisponibleException' con herencia profunda (DIT=3) — `src/main/java/prog2/servicioturnos/reservas/domain/exception/TurnoNoDisponibleException.java`
- clase 'CancelacionNoConfirmadaException' con herencia profunda (DIT=3) — `src/main/java/prog2/servicioturnos/reservas/domain/exception/CancelacionNoConfirmadaException.java`
- clase 'TurnoOcupadoException' con herencia profunda (DIT=3) — `src/main/java/prog2/servicioturnos/reservas/domain/exception/TurnoOcupadoException.java`
- clase 'TelefonoInvalidoException' con herencia profunda (DIT=3) — `src/main/java/prog2/servicioturnos/reservas/domain/exception/TelefonoInvalidoException.java`
- clase 'SinProcesoEnCursoException' con herencia profunda (DIT=3) — `src/main/java/prog2/servicioturnos/reservas/domain/exception/SinProcesoEnCursoException.java`
- clase 'ProcesoVencidoException' con herencia profunda (DIT=3) — `src/main/java/prog2/servicioturnos/reservas/domain/exception/ProcesoVencidoException.java`
- clase 'ReservaNoCancelableException' con herencia profunda (DIT=3) — `src/main/java/prog2/servicioturnos/reservas/domain/exception/ReservaNoCancelableException.java`
- clase 'ReservaRechazadaException' con herencia profunda (DIT=3) — `src/main/java/prog2/servicioturnos/reservas/domain/exception/ReservaRechazadaException.java`
- clase 'ProcesoAbiertoException' con herencia profunda (DIT=3) — `src/main/java/prog2/servicioturnos/reservas/domain/exception/ProcesoAbiertoException.java`

**OCP/type-switch** (2)
- clase 'CatalogoRestAdapter' con 1 type-checks — `src/main/java/prog2/servicioturnos/disponibilidad/infrastructure/catalogo/CatalogoRestAdapter.java`
- clase 'ManejadorErroresApi' con 3 type-checks — `src/main/java/prog2/servicioturnos/shared/web/ManejadorErroresApi.java`

## 6. Evolución por commit

Métricas calculadas sobre el estado de `main` en **cada commit** (6 commits con trabajo), del primero al último. La calidad usa el perfil `exigente`; el aporte de código lo mide RepoSense. El índice puede **bajar** al crecer el proyecto: se acumulan señales.

```mermaid
xychart-beta
    title "Índice de calidad a lo largo de los commits"
    x-axis ["8a14fc8", "b7c5fc3", "e698f47", "1a3b657", "69acdba", "b1e80b2"]
    y-axis "Índice" 0 --> 100
    line [0, 15, 46, 48, 65, 65]
```

| # | Fecha | Commit | Autor | LOC | Índice | %POO | Duplic. | Sueltas |
|---|---|---|---|---:|---:|---:|---:|---:|
| 1 | 2026-09-10 | `8a14fc8` | Ing. Daniel Quinteros | 0 | 0 | 0% | 0.0% | 0 |
| 2 | 2026-09-10 | `b7c5fc3` | Ing. Daniel Quinteros | 0 | 15 | 0% | 0.0% | 0 |
| 3 | 2026-10-12 | `e698f47` | Guadalupe Yañez | 0 | 46 | 0% | 0.0% | 0 |
| 4 | 2026-10-29 | `1a3b657` | Guadalupe Yañez | 89 | 48 | 56% | 0.0% | 0 |
| 5 | 2026-11-07 | `69acdba` | Guadalupe Yañez | 8,136 | 65 | 72% | 0.4% | 0 |
| 6 | 2026-11-10 | `b1e80b2` | Guadalupe Yañez | 8,186 | 65 | 72% | 0.4% | 0 |

---
_Informe generado por la cátedra. Las dimensiones y umbrales corresponden a `calidad.py` (perfil `exigente`); el semáforo, a `CRITERIOS-CALIDAD.md`. La documentación del repositorio no se evalúa. Ante dudas, revisá el archivo señalado._
<!-- evaluacion:fin -->
