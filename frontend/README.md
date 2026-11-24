# Frontend – Sammy Fitness Gym

Aplicación Angular 21 (componentes standalone + Angular Material). Instrucciones completas en el
[README principal](../README.md).

```bash
npm install                 # primera vez
npm start                   # http://localhost:4200 (requiere el backend en http://localhost:8080)
npm run build               # compilación de producción en dist/
npm test -- --watch=false   # pruebas unitarias con Vitest
```

| Carpeta | Contenido |
|---|---|
| `src/app/core/services` | AuthService, ClaseService, ReservaService, MembresiaService, ToastService, TemaService |
| `src/app/core/interceptors` | Interceptor que agrega `Authorization: Bearer` y redirige a `/login` ante un 401 |
| `src/app/core/guards` | AuthGuard y RoleGuard |
| `src/app/components` | Navbar, Login, CatalogoClases, ClaseCard, Filtros, MisReservas, PanelAdmin (clases, socios, inscritos), Toast |
| `src/environments/environment.ts` | URL de la API |
