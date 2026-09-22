import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, tap } from 'rxjs';
import { Order } from '../models/order.model';
import { CartItem } from '../models/cart.model';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly api = 'http://localhost:8084/api/ordenes';

  constructor(private http: HttpClient) {}

  create(items: CartItem[], paymentId: string, clienteEmail: string, direccionEnvio: string): Observable<Order> {
    const payload = {
      paymentId,
      clienteEmail,
      direccionEnvio,
      items: items.map(i => ({
        productoId: i.product.id,
        nombre: i.product.nombre,
        precioUnitario: i.product.precio,
        cantidad: i.quantity,
        imagenUrl: i.product.imagenUrl
      }))
    };

    /*
     * Importante: aquí NO usamos una orden ficticia como fallback.
     * Una compra solo se considera completada si msvc-orden responde correctamente,
     * porque ese servicio también descuenta el stock real en msvc-producto.
     */
    return this.http.post<Order>(this.api, payload).pipe(
      tap(o => localStorage.setItem(`order_${o.id}`, JSON.stringify(o)))
    );
  }

  get(id: string): Observable<Order | null> {
    return this.http.get<Order>(`${this.api}/${id}`).pipe(
      catchError(() => of(JSON.parse(localStorage.getItem(`order_${id}`) || 'null')))
    );
  }
}
