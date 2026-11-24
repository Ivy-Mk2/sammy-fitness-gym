# API REST – Sammy Fitness Gym

Base URL local: `http://localhost:8080` · Documentación interactiva: `http://localhost:8080/swagger-ui.html`

- Autenticación: cabecera `Authorization: Bearer <token>` (el token se obtiene en `POST /api/auth/login`).
- Formato de fechas `yyyy-MM-dd`, horas `HH:mm`. Cuerpos en JSON.
- Todos los errores devuelven el mismo cuerpo:

```json
{ "fecha": "2026-11-24T10:15:30", "codigo": 409, "mensaje": "La clase no tiene cupos disponibles (aforo completo)", "ruta": "/api/reservas" }
```

| Código | Significado |
|---|---|
| 400 | Datos inválidos (Bean Validation, JSON mal formado, valor de enum/fecha/número incorrecto) |
| 401 | No autenticado (sin token, token inválido/expirado) o credenciales incorrectas en el login |
| 403 | Autenticado pero sin permiso (rol no autorizado o recurso de otro usuario) |
| 404 | El recurso o la ruta no existen |
| 409 | Conflicto de negocio (aforo completo, reserva duplicada, DNI/correo repetido, membresía no activa, modificación concurrente) |
| 500 | Error inesperado (el detalle solo queda en el log del servidor) |

## Requisitos funcionales (RF) referenciados

> Numeración usada en esta tabla. Si el documento de requisitos del curso usa otra, ajuste solo esta leyenda y la última columna.

| RF | Descripción |
|---|---|
| RF-01 | Iniciar sesión con correo y contraseña; acceso según rol |
| RF-02 | Consultar el catálogo de clases con filtros (disciplina, día, horario, entrenador) y cupos |
| RF-03 | Gestionar clases (crear, editar, eliminar) |
| RF-04 | Consultar la lista de inscritos de una clase |
| RF-05 | Reservar una clase (RN-01, RN-02, RN-03) con correo de confirmación |
| RF-06 | Consultar mis reservas |
| RF-07 | Cancelar una reserva propia (RN-04) |
| RF-08 | Registrar la asistencia de un cliente |
| RF-09 | Consultar mi membresía |
| RF-10 | Gestionar socios (registrar, editar, consultar) |
| RF-11 | Gestionar membresías (asignar con RN-05, cambiar estado; vencimiento y avisos RN-06) |
| RF-12 | Consultar planes de membresía |

## Endpoints

| Método | Ruta | Rol | Parámetros / Body | Respuesta y códigos | RF cubierto |
|---|---|---|---|---|---|
| POST | `/api/auth/login` | Público | Body: `{ correo*, password* }` | **200** `{ token, rol, nombre }` · 400 datos inválidos · 401 "Credenciales incorrectas" | RF-01 |
| GET | `/api/clases` | Todos los autenticados | Query (opcionales): `disciplina` (SPINNING, CROSSFIT, YOGA, FUNCIONAL, ZUMBA, HALTEROFILIA) · `dia` (yyyy-MM-dd; sin él, desde hoy) · `horario` (MANANA, TARDE, NOCHE) · `entrenadorId` | **200** `[ { id, nombre, disciplina, entrenadorId, entrenadorNombre, fecha, horaInicio, horaFin, aforoMaximo, cuposOcupados, cuposDisponibles, descripcion } ]` · 400 filtro inválido · 401 | RF-02 |
| GET | `/api/clases/{id}` | Todos los autenticados | Path: `id` | **200** clase · 401 · 404 | RF-02 |
| POST | `/api/clases` | ADMINISTRADOR, ENTRENADOR | Body: `{ nombre* (3-60), disciplina*, entrenadorId (obligatorio para ADMINISTRADOR; el ENTRENADOR crea para sí mismo), fecha* (no pasada), horaInicio*, horaFin* (> inicio), aforoMaximo* (1-50), descripcion (máx. 200) }` | **201** clase + cabecera `Location` · 400 · 401 · 403 (rol o entrenador creando para otro) · 409 el entrenador ya tiene clase en ese horario | RF-03 |
| PUT | `/api/clases/{id}` | ADMINISTRADOR, ENTRENADOR (solo sus clases) | Path: `id` · Body: igual que POST | **200** clase · 400 · 401 · 403 · 404 · 409 (cruce de horario, aforo menor a reservas confirmadas, clase ya iniciada, modificación concurrente) | RF-03 |
| DELETE | `/api/clases/{id}` | ADMINISTRADOR, ENTRENADOR (solo sus clases) | Path: `id` | **204** · 401 · 403 · 404 · 409 la clase tiene reservas confirmadas | RF-03 |
| GET | `/api/clases/{id}/inscritos` | ADMINISTRADOR, RECEPCIONISTA, ENTRENADOR (solo sus clases) | Path: `id` | **200** `[ { reservaId, clienteId, nombreCompleto, dni, telefono, correo, estado, fechaReserva } ]` · 401 · 403 · 404 | RF-04 |
| POST | `/api/reservas` | CLIENTE | Body: `{ claseId* }` | **201** `{ id, claseId, claseNombre, disciplina, entrenadorNombre, fecha, horaInicio, horaFin, estado, fechaReserva, cancelable }` · 400 · 401 · 403 · 404 clase inexistente · 409 membresía no activa (RN-01), aforo completo o reserva simultánea (RN-02), reserva duplicada o cruce de horario (RN-03), clase ya realizada | RF-05 |
| GET | `/api/reservas/mis-reservas` | CLIENTE | — | **200** lista de reservas del cliente (todas los estados) · 401 · 403 | RF-06 |
| DELETE | `/api/reservas/{id}` | CLIENTE (solo las propias) | Path: `id` | **200** reserva con estado `CANCELADA` (cupo liberado) · 401 · 403 reserva de otro cliente · 404 · 409 no está confirmada o la clase ya se realizó/está en curso (RN-04) | RF-07 |
| PATCH | `/api/reservas/{id}/asistencia` | ADMINISTRADOR, ENTRENADOR (solo sus clases) | Path: `id` · Body: `{ asistio* (true/false) }` | **200** reserva `ASISTIDA` (o `CONFIRMADA` si `false`) · 400 · 401 · 403 · 404 · 409 reserva cancelada o clase aún no realizada | RF-08 |
| GET | `/api/membresias/mi-membresia` | CLIENTE | — | **200** `{ id, socioId, planId, planNombre, precio, fechaInicio, fechaVencimiento, estado, diasRestantes }` (la vigente o la más reciente) · 401 · 403 · 404 sin membresías | RF-09 |
| GET | `/api/socios` | ADMINISTRADOR, RECEPCIONISTA | Query: `q` (opcional; busca en nombres, apellidos, DNI, correo) | **200** `[ { id, nombres, apellidos, dni, correo, telefono, activo, membresiaActual } ]` · 401 · 403 | RF-10 |
| GET | `/api/socios/{id}` | ADMINISTRADOR, RECEPCIONISTA | Path: `id` | **200** socio · 401 · 403 · 404 | RF-10 |
| POST | `/api/socios` | ADMINISTRADOR, RECEPCIONISTA | Body: `{ nombres* (máx. 80), apellidos* (máx. 80), dni* (8 dígitos), correo* (email), telefono* (9 dígitos), password* (8-100), activo }` | **201** socio + `Location` · 400 · 401 · 403 · 409 DNI o correo ya registrados | RF-10 |
| PUT | `/api/socios/{id}` | ADMINISTRADOR, RECEPCIONISTA | Path: `id` · Body: igual que POST; `password` vacía = no cambia; `activo=false` desactiva el acceso | **200** socio · 400 · 401 · 403 · 404 · 409 DNI o correo de otro usuario | RF-10 |
| POST | `/api/socios/{id}/membresias` | ADMINISTRADOR, RECEPCIONISTA | Path: `id` · Body: `{ planId*, fechaInicio (por defecto hoy) }` | **201** membresía con `fechaVencimiento = fechaInicio + duración del plan` (RN-05) · 400 · 401 · 403 · 404 socio o plan inexistente · 409 ya tiene una membresía activa en ese periodo | RF-11 |
| PATCH | `/api/membresias/{id}/estado` | ADMINISTRADOR, RECEPCIONISTA | Path: `id` · Body: `{ estado* (ACTIVA, VENCIDA, CONGELADA) }` | **200** membresía · 400 · 401 · 403 · 404 · 409 no se puede activar (vencida o se cruza con otra activa) | RF-11 |
| GET | `/api/planes` | Todos los autenticados | — | **200** `[ { id, nombre, duracionDias, precio } ]` · 401 | RF-12 |

`*` = obligatorio.

## Procesos automáticos (sin endpoint)

| Proceso | Cuándo | Qué hace | RF |
|---|---|---|---|
| Vencimiento de membresías (RN-06) | Diario, 06:00 hora de Lima (`MEMBRESIAS_CRON`) | Marca `VENCIDA` las membresías `ACTIVA` con `fechaVencimiento` pasada y envía un correo a las que vencen en 3 días o menos | RF-11 |
| Correo de confirmación de reserva | Tras confirmar cada reserva (`@Async`, después del commit) | Envía los datos de la clase al cliente. Si falla, la reserva no se ve afectada | RF-05 |

Sin `SENDGRID_API_KEY` los correos no se envían: se registran en el log del backend.

## Casos de prueba (Postman)

Colección: [`postman/gym.postman_collection.json`](../postman/gym.postman_collection.json). Importe la colección en Postman, ponga en la variable `password` el valor de `SEED_PASSWORD` y ejecútela completa con *Run collection*.

| Caso | Petición | Esperado |
|---|---|---|
| PE-01 | `POST /api/auth/login` (cliente03) | 200 y se guarda `{{token}}` |
| PE-02 | `POST /api/auth/login` con contraseña incorrecta | 401 "Credenciales incorrectas" |
| PE-03 | `GET /api/clases` sin token | 401 |
| PE-04 | `POST /api/clases` con token de cliente | 403 |
| PE-05 | `POST /api/clases` como entrenador (aforo 1) | 201 |
| PE-06 | `POST /api/reservas` (cliente03 en la clase de PE-05) | 201 |
| PE-07 | `POST /api/reservas` (cliente04 en la misma clase, ya llena) | 409 |
| PE-08 | `POST /api/reservas` (cliente10, membresía vencida) | 409 |
| PE-09 | `DELETE /api/reservas/{id}` de la reserva de PE-06 | 200 |
| PE-10 | `GET /api/clases/9999` | 404 |

> Las membresías de los datos ficticios se crean relativas a la fecha en que se cargó la BD (las de cliente01–07 vencen a los 25 días). Si pasa ese tiempo, borre la BD (`docker compose down -v` y `docker compose up -d`) para regenerarlos.
