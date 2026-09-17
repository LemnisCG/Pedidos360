import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
@Injectable({ providedIn: 'root' })
export class PaymentService {
  private readonly api = 'http://localhost:8085/api/pagos';
  constructor(private http: HttpClient) {}
  pay(total: number): Observable<any> { return this.http.post(this.api, { monto: total, moneda: 'CLP', metodo: 'CARD', ultimos4: '4242' }).pipe(catchError(() => of({ id: crypto.randomUUID(), estado: 'APROBADO' }))); }
}
