import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Clase, ClaseRequest, Entrenador, FiltrosClase, Inscrito } from '../models';

@Injectable({ providedIn: 'root' })
export class ClaseService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/clases`;

  listar(filtros: FiltrosClase = {}): Observable<Clase[]> {
    let params = new HttpParams();
    for (const [clave, valor] of Object.entries(filtros)) {
      if (valor !== null && valor !== undefined && valor !== '') {
        params = params.set(clave, String(valor));
      }
    }
    return this.http.get<Clase[]>(this.url, { params });
  }

  obtener(id: number): Observable<Clase> {
    return this.http.get<Clase>(`${this.url}/${id}`);
  }

  crear(clase: ClaseRequest): Observable<Clase> {
    return this.http.post<Clase>(this.url, clase);
  }

  actualizar(id: number, clase: ClaseRequest): Observable<Clase> {
    return this.http.put<Clase>(`${this.url}/${id}`, clase);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  inscritos(id: number): Observable<Inscrito[]> {
    return this.http.get<Inscrito[]>(`${this.url}/${id}/inscritos`);
  }

  entrenadores(): Observable<Entrenador[]> {
    return this.http.get<Entrenador[]>(`${environment.apiUrl}/entrenadores`);
  }
}
