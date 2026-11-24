import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { AuthService } from './core/services/auth.service';

export const routes: Routes = [
  {
    path: 'login',
    title: 'Iniciar sesión · Sammy Fitness',
    loadComponent: () => import('./components/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'clases',
    title: 'Clases · Sammy Fitness',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./components/catalogo-clases/catalogo-clases.component').then((m) => m.CatalogoClasesComponent),
  },
  {
    path: 'mis-reservas',
    title: 'Mis reservas · Sammy Fitness',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['CLIENTE'] },
    loadComponent: () =>
      import('./components/mis-reservas/mis-reservas.component').then((m) => m.MisReservasComponent),
  },
  {
    path: 'admin',
    title: 'Panel · Sammy Fitness',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['ADMINISTRADOR', 'ENTRENADOR', 'RECEPCIONISTA'] },
    loadComponent: () => import('./components/panel-admin/panel-admin.component').then((m) => m.PanelAdminComponent),
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: () => (inject(AuthService).tieneRol('RECEPCIONISTA') ? '/admin/socios' : '/admin/clases'),
      },
      {
        path: 'clases',
        title: 'Gestión de clases · Sammy Fitness',
        canActivate: [roleGuard],
        data: { roles: ['ADMINISTRADOR', 'ENTRENADOR'] },
        loadComponent: () =>
          import('./components/panel-admin/admin-clases.component').then((m) => m.AdminClasesComponent),
      },
      {
        path: 'socios',
        title: 'Socios · Sammy Fitness',
        canActivate: [roleGuard],
        data: { roles: ['ADMINISTRADOR', 'RECEPCIONISTA'] },
        loadComponent: () =>
          import('./components/panel-admin/admin-socios.component').then((m) => m.AdminSociosComponent),
      },
      {
        path: 'inscritos',
        title: 'Inscritos · Sammy Fitness',
        loadComponent: () =>
          import('./components/panel-admin/admin-inscritos.component').then((m) => m.AdminInscritosComponent),
      },
    ],
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: () => {
      const auth = inject(AuthService);
      return auth.estaAutenticado() ? auth.rutaInicio() : '/login';
    },
  },
  { path: '**', redirectTo: '' },
];
