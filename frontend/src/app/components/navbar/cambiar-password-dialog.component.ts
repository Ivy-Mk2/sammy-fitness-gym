import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-cambiar-password-dialog',
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
  ],
  template: `
    <h2 mat-dialog-title>Cambiar contraseña</h2>
    <form [formGroup]="form" (ngSubmit)="guardar()">
      <mat-dialog-content class="contenido">
        <mat-form-field appearance="outline">
          <mat-label>Contraseña actual</mat-label>
          <input matInput [type]="verActual() ? 'text' : 'password'" formControlName="passwordActual" />
          <button mat-icon-button matSuffix type="button" (click)="verActual.set(!verActual())">
            <mat-icon>{{ verActual() ? 'visibility_off' : 'visibility' }}</mat-icon>
          </button>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Nueva contraseña</mat-label>
          <input matInput [type]="verNueva() ? 'text' : 'password'" formControlName="passwordNueva" />
          <button mat-icon-button matSuffix type="button" (click)="verNueva.set(!verNueva())">
            <mat-icon>{{ verNueva() ? 'visibility_off' : 'visibility' }}</mat-icon>
          </button>
          <mat-hint>Mínimo 8 caracteres</mat-hint>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Confirmar nueva contraseña</mat-label>
          <input matInput [type]="verNueva() ? 'text' : 'password'" formControlName="confirmar" />
        </mat-form-field>

        @if (error()) {
          <p class="error">{{ error() }}</p>
        }
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close [disabled]="cargando()">Cancelar</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="form.invalid || cargando()">
          {{ cargando() ? 'Guardando...' : 'Actualizar contraseña' }}
        </button>
      </mat-dialog-actions>
    </form>
  `,
  styles: `
    .contenido { display: flex; flex-direction: column; gap: 8px; padding-top: 8px; min-width: 300px; }
    .error { color: var(--mat-sys-error); font: var(--mat-sys-body-small); margin: 0; }
  `,
})
export class CambiarPasswordDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly dialogRef = inject(MatDialogRef<CambiarPasswordDialogComponent>);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly verActual = signal(false);
  protected readonly verNueva = signal(false);
  protected readonly cargando = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    passwordActual: ['', [Validators.required]],
    passwordNueva: ['', [Validators.required, Validators.minLength(8)]],
    confirmar: ['', [Validators.required]],
  });

  protected guardar(): void {
    const { passwordActual, passwordNueva, confirmar } = this.form.getRawValue();
    if (passwordNueva !== confirmar) {
      this.error.set('Las contraseñas nuevas no coinciden');
      return;
    }

    this.cargando.set(true);
    this.error.set(null);

    this.auth.cambiarPassword(passwordActual, passwordNueva).subscribe({
      next: (resp) => {
        this.cargando.set(false);
        this.snackBar.open(resp.mensaje, 'Cerrar', { duration: 4000 });
        this.dialogRef.close(true);
      },
      error: (err) => {
        this.cargando.set(false);
        this.error.set(err?.error?.mensaje ?? 'No se pudo cambiar la contraseña');
      },
    });
  }
}