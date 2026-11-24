# Sammy Fitness Gym – Sistema Web de Gestión de Clases y Membresías

Aplicación web para que los socios de **Sammy Fitness Gym** reserven clases grupales y para que el personal
gestione clases, socios, membresías y asistencia.

| Capa | Tecnología |
|---|---|
| Backend | Java 21 · Spring Boot 3.5 (Web, Security, Data JPA, Validation) · JWT (jjwt) · BCrypt · springdoc-openapi · MySQL 8 |
| Frontend | Angular 21 (componentes standalone) · TypeScript · RxJS · Angular Router · HttpClient · Angular Material |
| Correo | SendGrid (sin API key, los correos solo se registran en el log) |
| Pruebas | JUnit 5 · Mockito · MockMvc · H2 (backend) · Vitest (frontend) · colección Postman |

```
sammy-fitness-gym/
├── backend/            API REST Spring Boot (paquete com.sammyfitness.gym)
├── frontend/           Aplicación Angular
├── docs/endpoints.md   Tabla de endpoints, roles, códigos y RF cubiertos
└── postman/            Colección con los casos de prueba PE-01 a PE-10
```

---

## 1. Requisitos

| Herramienta | Versión | Comprobar con |
|---|---|---|
| JDK | 21 o superior | `java -version` |
| Node.js | 20.19+, 22.12+ o 24+ | `node -v` |
| npm | 10 o superior | `npm -v` |
| MySQL 8 | **o** Docker Desktop (recomendado) | `docker -v` |
| Git | cualquiera reciente | `git --version` |

No hace falta instalar Maven: el backend incluye el **Maven Wrapper** (`mvnw` / `mvnw.cmd`).

> **Importante (Windows):** clone el proyecto en una ruta **sin paréntesis**, por ejemplo
> `C:\proyectos\sammy-fitness-gym`. Vitest interpreta los paréntesis como un patrón y
> `ng test` no encuentra las pruebas (compilar y ejecutar la app no se ve afectado).

---

## 2. Variables de entorno

Las credenciales **nunca** están en el código. El backend lee el archivo `backend/.env` (o variables de entorno del sistema).

```bash
cd backend
cp .env.example .env        # en Windows: copy .env.example .env
```

| Variable | Obligatoria | Descripción | Ejemplo / valor por defecto |
|---|---|---|---|
| `DB_URL` | No | URL JDBC de MySQL | `jdbc:mysql://localhost:3306/sammy_gym?createDatabaseIfNotExist=true&serverTimezone=America/Lima` |
| `DB_USERNAME` | **Sí** | Usuario de MySQL | `root` |
| `DB_PASSWORD` | **Sí** | Contraseña de MySQL | — |
| `JWT_SECRET` | **Sí** | Clave para firmar los JWT (mín. 32 caracteres aleatorios) | — |
| `JWT_EXPIRATION_MS` | No | Duración del token en ms | `28800000` (8 h) |
| `FRONTEND_URL` | No | Origen permitido por CORS | `http://localhost:4200` |
| `SEED_PASSWORD` | Para datos de prueba | Contraseña de los usuarios ficticios | — |
| `SENDGRID_API_KEY` | No | API key de SendGrid; vacía = correos solo en el log | vacío |
| `MAIL_FROM` / `MAIL_FROM_NAME` | No | Remitente verificado en SendGrid | `no-reply@…` / `Sammy Fitness Gym` |
| `MEMBRESIAS_CRON` | No | Hora del proceso diario de vencimientos (RN-06) | `0 0 6 * * *` |
| `APP_ZONA_HORARIA` | No | Zona horaria del gimnasio | `America/Lima` |
| `SPRING_PROFILES_ACTIVE` | No | `dev` carga los datos ficticios | `dev` |
| `SERVER_PORT` | No | Puerto del backend | `8080` |

Generar un `JWT_SECRET` aleatorio:

```powershell
# PowerShell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
```
```bash
# Linux / macOS / Git Bash
openssl rand -base64 48
```

El frontend solo necesita la URL de la API, definida en `frontend/src/environments/environment.ts`
(por defecto `http://localhost:8080/api`).

---

## 3. Crear la base de datos

### Opción A – Docker (recomendada)

`backend/docker-compose.yml` levanta MySQL 8 con la contraseña de `DB_PASSWORD` y crea la base `sammy_gym`:

```bash
cd backend
docker compose up -d          # iniciar
docker compose ps             # debe aparecer "healthy"
docker compose down           # detener (conserva los datos)
docker compose down -v        # detener y BORRAR los datos (regenera los de prueba al reiniciar)
```

### Opción B – MySQL instalado localmente

```sql
CREATE DATABASE sammy_gym CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Ajuste `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` en `backend/.env`.
En ambos casos **las tablas se crean solas** al arrancar el backend (`JPA_DDL_AUTO=update`).

---

## 4. Ejecutar el backend

```bash
cd backend
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

- API: <http://localhost:8080/api>
- **Swagger UI: <http://localhost:8080/swagger-ui.html>** (ejecute `POST /api/auth/login`, copie el `token` y pulse **Authorize**)
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

Con el perfil `dev` y la base vacía, el `DataSeeder` carga los datos ficticios (ver sección 6).

Pruebas del backend (usan H2 en memoria, no tocan MySQL):

```bash
./mvnw verify                 # Windows: mvnw.cmd verify
```

---

## 5. Ejecutar el frontend

```bash
cd frontend
npm install                   # solo la primera vez
npm start                     # = ng serve
```

Abra <http://localhost:4200>. Otros comandos:

```bash
npm run build                 # compilación de producción en frontend/dist
npm test -- --watch=false     # pruebas unitarias (Vitest)
```

---

## 6. Usuarios de prueba

Datos **ficticios** creados por el `DataSeeder` (perfil `dev`, solo si la base está vacía).
**Todos usan como contraseña el valor de `SEED_PASSWORD`** de su `backend/.env`.

| Rol | Correo | Notas |
|---|---|---|
| Administrador | `admin@sammyfitness.test` | Acceso a clases, socios e inscritos |
| Recepcionista | `recepcion@sammyfitness.test` | Socios, membresías e inscritos |
| Entrenador | `carlos.ramos@sammyfitness.test` | También `lucia.torres@`, `miguel.vargas@`, `andrea.flores@sammyfitness.test` |
| Cliente | `cliente01@correo.test` … `cliente08@correo.test` | Membresía **activa** |
| Cliente | `cliente09@correo.test` | Membresía que vence en 2 días (aviso RN-06) |
| Cliente | `cliente10@correo.test` | Membresía **vencida** (no puede reservar) |

También se crean 2 planes (Mensual 30 días S/ 120, Trimestral 90 días S/ 320), 8 clases en los próximos días
y algunas reservas (la clase *Halterofilia Técnica* queda llena).

Los dominios `.test` están reservados: ningún correo de prueba llega a una persona real.
Las fechas son relativas al día en que se cargó la base; para regenerarlas use `docker compose down -v`.

---

## 7. Funcionalidades por rol

| Rol | Puede |
|---|---|
| Cliente | Ver el catálogo con filtros y cupos, reservar en un clic, ver y cancelar sus reservas, ver su membresía |
| Entrenador | Crear/editar/eliminar **sus** clases, ver sus inscritos y registrar asistencia |
| Recepcionista | Registrar y editar socios, asignar membresías, cambiar su estado, ver inscritos |
| Administrador | Todo lo anterior sobre cualquier clase |

Reglas de negocio principales: RN-01 membresía activa en la fecha de la clase · RN-02 aforo con bloqueo optimista
(`@Version`, 409 si hay concurrencia) · RN-03 sin reservas duplicadas ni cruces de horario · RN-04 cancelar libera
el cupo (no en clases ya realizadas) · RN-05 vencimiento = inicio + duración del plan · RN-06 proceso diario de
vencimientos y avisos por correo.

Detalle de cada endpoint, roles y códigos de respuesta: **[docs/endpoints.md](docs/endpoints.md)**.

---

## 8. Pruebas con Postman

1. Importe `postman/gym.postman_collection.json` en Postman.
2. En la pestaña **Variables** de la colección, ponga en `password` el valor de `SEED_PASSWORD`
   (y ajuste `baseUrl` si el backend no está en `http://localhost:8080`).
3. Ejecute la colección completa con **Run collection**.

Incluye los casos PE-01 a PE-10. `{{token}}` se llena automáticamente en cada login y el flujo es repetible:
crea su propia clase, reserva sobre ella y la elimina al final.

---

## 9. Solución de problemas

| Problema | Causa / solución |
|---|---|
| El backend no arranca: `JWT_SECRET debe tener al menos 32 caracteres` | Defina `JWT_SECRET` en `backend/.env` |
| `Communications link failure` al arrancar | MySQL no está encendido: `docker compose up -d` |
| `http://localhost:8080/` responde 401 | Normal: la API no tiene página raíz y todo requiere token. Use Swagger o el frontend |
| Login 401 con los usuarios de prueba | La contraseña es `SEED_PASSWORD`. Si la cambió después de cargar los datos, use `docker compose down -v` y reinicie |
| El frontend muestra "No se pudo conectar con el servidor" | El backend no está en `http://localhost:8080` o `FRONTEND_URL` no coincide con el origen del frontend |
| `ng test` dice "No test files found" | La ruta del proyecto tiene paréntesis; muévalo a una ruta sin ellos |
