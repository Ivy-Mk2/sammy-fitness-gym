import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { TemaService } from '../../core/services/tema.service';
import { ToastService } from '../../core/services/toast.service';
import { mensajeError } from '../../core/utils';

@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <div class="fondo">
      <button
        mat-icon-button
        class="tema"
        (click)="tema.alternar()"
        [attr.aria-label]="tema.tema() === 'oscuro' ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'"
      >
        <mat-icon>{{ tema.tema() === 'oscuro' ? 'light_mode' : 'dark_mode' }}</mat-icon>
      </button>

      <mat-card class="tarjeta" appearance="outlined">
        <div class="cabecera">
          <mat-icon class="logo" aria-hidden="true">fitness_center</mat-icon>
          <h1>Sammy Fitness Gym</h1>
          <p>Reserva tus clases y gestiona tu membresía</p>
        </div>

        <form [formGroup]="form" (ngSubmit)="ingresar()" novalidate>
          <mat-form-field appearance="outline">
            <mat-label>Correo electrónico</mat-label>
            <input matInput type="email" formControlName="correo" autocomplete="username" />
            <mat-icon matPrefix>mail</mat-icon>
            @if (form.controls.correo.hasError('required')) {
              <mat-error>El correo es obligatorio</mat-error>
            } @else if (form.controls.correo.hasError('email')) {
              <mat-error>El correo no tiene un formato válido</mat-error>
            }
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Contraseña</mat-label>
            <input
              matInput
              [type]="verPassword() ? 'text' : 'password'"
              formControlName="password"
              autocomplete="current-password"
            />
            <mat-icon matPrefix>lock</mat-icon>
            <button
              mat-icon-button
              matSuffix
              type="button"
              (click)="verPassword.set(!verPassword())"
              [attr.aria-label]="verPassword() ? 'Ocultar contraseña' : 'Mostrar contraseña'"
            >
              <mat-icon>{{ verPassword() ? 'visibility_off' : 'visibility' }}</mat-icon>
            </button>
            @if (form.controls.password.hasError('required')) {
              <mat-error>La contraseña es obligatoria</mat-error>
            }
          </mat-form-field>

          @if (error()) {
            <p class="error" role="alert"><mat-icon>error</mat-icon>{{ error() }}</p>
          }

          <button mat-flat-button type="submit" class="ingresar" [disabled]="enviando()">
            @if (enviando()) {
              <mat-spinner diameter="20" />
            } @else {
              Ingresar
            }
          </button>
        </form>
      </mat-card>
    </div>
  `,
  styles: `
    .fondo {
      min-height: 100vh;
      display: grid;
      place-items: center;
      padding: 16px;
      box-sizing: border-box;
      background:
        radial-gradient(circle at 20% 10%, color-mix(in srgb, var(--mat-sys-primary) 22%, transparent), transparent 45%),
        radial-gradient(circle at 85% 90%, color-mix(in srgb, var(--mat-sys-tertiary) 20%, transparent), transparent 45%),
        var(--mat-sys-surface);
      position: relative;
    }
    .tema { position: absolute; top: 12px; right: 12px; }
    .tarjeta { width: min(420px, 100%); padding: 28px 24px 20px; box-sizing: border-box; }
    .cabecera { text-align: center; margin-bottom: 20px; }
    .logo { font-size: 44px; width: 44px; height: 44px; color: var(--mat-sys-primary); }
    h1 { font: var(--mat-sys-headline-small); margin: 8px 0 4px; }
    .cabecera p { margin: 0; color: var(--mat-sys-on-surface-variant); }
    form { display: flex; flex-direction: column; gap: 4px; }
    .ingresar { height: 48px; margin-top: 8px; }
    .ingresar mat-spinner { margin: 0 auto; }
    .error {
      display: flex; align-items: center; gap: 8px; margin: 0 0 4px;
      color: var(--mat-sys-error); font: var(--mat-sys-body-medium);
    }
  `,
})
export class LoginComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly toast = inject(ToastService);
  protected readonly tema = inject(TemaService);

  protected readonly enviando = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly verPassword = signal(false);

  // Mismas reglas que LoginRequest del backend
  protected readonly form = this.fb.nonNullable.group({
    correo: ['', [Validators.required, Validators.email, Validators.maxLength(120)]],
    password: ['', [Validators.required, Validators.maxLength(100)]],
  });

  ngOnInit(): void {
    if (this.auth.estaAutenticado()) {
      this.router.navigateByUrl(this.auth.rutaInicio());
    }
  }

  protected ingresar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.error.set(null);
    const { correo, password } = this.form.getRawValue();
    this.auth.login(correo.trim(), password).subscribe({
      next: (usuario) => {
        this.enviando.set(false);
        this.toast.exito(`¡Bienvenido(a), ${usuario.nombre}!`);
        const volver = this.route.snapshot.queryParamMap.get('volver');
        this.router.navigateByUrl(volver && volver.startsWith('/') ? volver : this.auth.rutaInicio(usuario.rol));
      },
      error: (e) => {
        this.enviando.set(false);
        this.error.set(mensajeError(e));
      },
    });
  }
}
