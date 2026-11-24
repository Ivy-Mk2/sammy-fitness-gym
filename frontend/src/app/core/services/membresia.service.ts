import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { EstadoMembresia, Membresia, Plan, Socio, SocioRequest } from '../models';

/** Membresías, planes y socios (equivalente a ClienteMembresiaService del backend). */
@Injectable({ providedIn: 'root' })
export class MembresiaService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  miMembresia(): Observable<Membresia> {
    return this.http.get<Membresia>(`${this.api}/membresias/mi-membresia`);
  }

  planes(): Observable<Plan[]> {
    return this.http.get<Plan[]>(`${this.api}/planes`);
  }

  cambiarEstado(membresiaId: number, estado: EstadoMembresia): Observable<Membresia> {
    return this.http.patch<Membresia>(`${this.api}/membresias/${membresiaId}/estado`, { estado });
  }

  // ---------------------------------------------------------------- socios

  socios(texto?: string): Observable<Socio[]> {
    const params = texto ? new HttpParams().set('q', texto) : undefined;
    return this.http.get<Socio[]>(`${this.api}/socios`, { params });
  }

  crearSocio(socio: SocioRequest): Observable<Socio> {
    return this.http.post<Socio>(`${this.api}/socios`, socio);
  }

  actualizarSocio(id: number, socio: SocioRequest): Observable<Socio> {
    return this.http.put<Socio>(`${this.api}/socios/${id}`, socio);
  }

  asignarMembresia(socioId: number, planId: number, fechaInicio: string | null): Observable<Membresia> {
    return this.http.post<Membresia>(`${this.api}/socios/${socioId}/membresias`, { planId, fechaInicio });
  }
}
