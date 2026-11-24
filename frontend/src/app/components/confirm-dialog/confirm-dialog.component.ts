import { Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialog, MatDialogModule } from '@angular/material/dialog';
import { Observable } from 'rxjs';

export interface DatosConfirmacion {
  titulo: string;
  mensaje: string;
  confirmar?: string;
  cancelar?: string;
  peligro?: boolean;
}

/** Diálogo de confirmación antes de cancelar o eliminar. */
@Component({
  selector: 'app-confirm-dialog',
  imports: [MatDialogModule, MatButtonModule],
  template: `
    <h2 mat-dialog-title>{{ datos.titulo }}</h2>
    <mat-dialog-content>
      <p>{{ datos.mensaje }}</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="false">{{ datos.cancelar ?? 'Volver' }}</button>
      <button mat-flat-button [class.peligro]="datos.peligro" [mat-dialog-close]="true" cdkFocusInitial>
        {{ datos.confirmar ?? 'Confirmar' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .peligro {
      --mat-button-filled-container-color: var(--mat-sys-error);
      --mat-button-filled-label-text-color: var(--mat-sys-on-error);
    }
  `,
})
export class ConfirmDialogComponent {
  protected readonly datos = inject<DatosConfirmacion>(MAT_DIALOG_DATA);
}

/** Abre el diálogo y emite true si el usuario confirma. */
export function confirmar(dialog: MatDialog, datos: DatosConfirmacion): Observable<boolean | undefined> {
  return dialog.open(ConfirmDialogComponent, { data: datos, width: '420px', maxWidth: '92vw' }).afterClosed();
}
