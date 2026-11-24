import { Component, DestroyRef, OnInit, inject, input, output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { debounceTime } from 'rxjs';
import { DISCIPLINAS, Disciplina, Entrenador, FiltrosClase, HORARIOS, Horario } from '../../core/models';

/** Filtros del catálogo: disciplina, día, horario y entrenador. Emite cada cambio. */
@Component({
  selector: 'app-filtros',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatSelectModule, MatInputModule, MatButtonModule, MatIconModule],
  template: `
    <form [formGroup]="form" class="filtros" aria-label="Filtros de clases">
      <mat-form-field appearance="outline" subscriptSizing="dynamic">
        <mat-label>Disciplina</mat-label>
        <mat-select formControlName="disciplina">
          <mat-option [value]="null">Todas</mat-option>
          @for (d of disciplinas; track d.valor) {
            <mat-option [value]="d.valor">{{ d.etiqueta }}</mat-option>
          }
        </mat-select>
      </mat-form-field>

      <mat-form-field appearance="outline" subscriptSizing="dynamic">
        <mat-label>Día</mat-label>
        <input matInput type="date" formControlName="dia" />
      </mat-form-field>

      <mat-form-field appearance="outline" subscriptSizing="dynamic">
        <mat-label>Horario</mat-label>
        <mat-select formControlName="horario">
          <mat-option [value]="null">Cualquiera</mat-option>
          @for (h of horarios; track h.valor) {
            <mat-option [value]="h.valor">{{ h.etiqueta }}</mat-option>
          }
        </mat-select>
      </mat-form-field>

      @if (mostrarEntrenador()) {
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Entrenador</mat-label>
          <mat-select formControlName="entrenadorId">
            <mat-option [value]="null">Todos</mat-option>
            @for (e of entrenadores(); track e.id) {
              <mat-option [value]="e.id">{{ e.nombre }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
      }

      <button mat-stroked-button type="button" (click)="limpiar()" class="limpiar">
        <mat-icon>filter_alt_off</mat-icon>Limpiar
      </button>
    </form>
  `,
  styles: `
    .filtros {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 12px;
      align-items: center;
    }
    .limpiar { height: 56px; }
  `,
})
export class FiltrosComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly entrenadores = input<Entrenador[]>([]);
  readonly mostrarEntrenador = input(true);
  readonly filtrosChange = output<FiltrosClase>();

  protected readonly disciplinas = DISCIPLINAS;
  protected readonly horarios = HORARIOS;

  protected readonly form = this.fb.group({
    disciplina: this.fb.control<Disciplina | null>(null),
    dia: this.fb.control<string | null>(null),
    horario: this.fb.control<Horario | null>(null),
    entrenadorId: this.fb.control<number | null>(null),
  });

  ngOnInit(): void {
    this.form.valueChanges
      .pipe(debounceTime(250), takeUntilDestroyed(this.destroyRef))
      .subscribe((v) => this.filtrosChange.emit({ ...v, dia: v.dia || null }));
  }

  protected limpiar(): void {
    this.form.reset({ disciplina: null, dia: null, horario: null, entrenadorId: null });
  }
}
