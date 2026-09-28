import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { API_BASE_URL } from '../config/api';
import { Order } from '../models/order.model';
import { CartLine } from '../models/cart.model';

@Injectable({ providedIn: 'root' })
export class OrderService {
  readonly orders = signal<Order[]>([]);
  constructor(private http: HttpClient) {}
  async checkout(direccion: string, numeroTarjeta: string, expiracion: string, codigoSeguridad: string, lines: CartLine[]) {
    const items = lines.map(line => ({ productoId: line.producto.id, cantidad: line.cantidad }));
    return firstValueFrom(this.http.post<Order>(`${API_BASE_URL}/api/pagos/checkout`, { direccion, numeroTarjeta, expiracion, codigoSeguridad, items }));
  }
  async loadMine() { const data = await firstValueFrom(this.http.get<Order[]>(`${API_BASE_URL}/api/pagos/mis-pedidos`)); this.orders.set(data); return data; }
  async get(id: string) { return firstValueFrom(this.http.get<Order>(`${API_BASE_URL}/api/pagos/mis-pedidos/${id}`)); }
}
