import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Reserva } from '../models';

@Injectable({ providedIn: 'root' })
export class ReservaService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/reservas`;

  reservar(claseId: number): Observable<Reserva> {
    return this.http.post<Reserva>(this.url, { claseId });
  }

  misReservas(): Observable<Reserva[]> {
    return this.http.get<Reserva[]>(`${this.url}/mis-reservas`);
  }

  cancelar(id: number): Observable<Reserva> {
    return this.http.delete<Reserva>(`${this.url}/${id}`);
  }

  marcarAsistencia(id: number, asistio: boolean): Observable<Reserva> {
    return this.http.patch<Reserva>(`${this.url}/${id}/asistencia`, { asistio });
  }
}
