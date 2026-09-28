import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { API_BASE_URL } from '../config/api';
import { AuthService } from '../auth/auth.service';
import { ProductService } from './product.service';
import { CartApiItem, CartLine } from '../models/cart.model';
import { Product } from '../models/product.model';

@Injectable({ providedIn: 'root' })
export class CartService {
  readonly lines = signal<CartLine[]>([]);
  readonly count = computed(() => this.lines().reduce((sum, line) => sum + line.cantidad, 0));
  readonly total = computed(() => this.lines().reduce((sum, line) => sum + line.producto.precio * line.cantidad, 0));
  constructor(private http: HttpClient, private auth: AuthService, private products: ProductService) {}

  async load() {
    if (!this.auth.isLoggedIn()) { this.lines.set([]); return; }
    const api = await firstValueFrom(this.http.get<CartApiItem[]>(`${API_BASE_URL}/api/carrito`));
    const byId = new Map(this.products.products().map(p => [p.id, p]));
    this.lines.set(api.map(i => ({ producto: byId.get(i.productoId), cantidad: i.cantidad })).filter(x => !!x.producto) as CartLine[]);
  }
  async add(producto: Product, cantidad = 1) { await firstValueFrom(this.http.post(`${API_BASE_URL}/api/carrito`, { productoId: producto.id, cantidad })); await this.load(); }
  async update(productoId: number, cantidad: number) { await firstValueFrom(this.http.put(`${API_BASE_URL}/api/carrito/${productoId}`, { productoId, cantidad })); await this.load(); }
  async remove(productoId: number) { await firstValueFrom(this.http.delete(`${API_BASE_URL}/api/carrito/${productoId}`)); await this.load(); }
  clearLocal() { this.lines.set([]); }
}
