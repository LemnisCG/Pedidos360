import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Product } from '../models/product.model';
import { CartItem } from '../models/cart.model';

@Injectable({ providedIn: 'root' })
export class CartService {
  // Se usa una nueva clave para evitar recuperar productos antiguos sin portada desde localStorage.
  private readonly key = 'pedidos360_cart_clp_v3';
  readonly items$ = new BehaviorSubject<CartItem[]>(this.load());
  private load(): CartItem[] { try { return JSON.parse(localStorage.getItem(this.key) || '[]'); } catch { return []; } }
  private save(items: CartItem[]) { localStorage.setItem(this.key, JSON.stringify(items)); this.items$.next(items); }
  add(product: Product) { const items = [...this.items$.value]; const found = items.find(i => i.product.id === product.id); found ? found.quantity++ : items.push({ product, quantity: 1 }); this.save(items); }
  update(id: number, quantity: number) { this.save(this.items$.value.map(i => i.product.id === id ? {...i, quantity} : i)); }
  remove(id: number) { this.save(this.items$.value.filter(i => i.product.id !== id)); }
  clear() { this.save([]); }
  count() { return this.items$.value.reduce((a, i) => a + i.quantity, 0); }
  total() { return this.items$.value.reduce((a, i) => a + i.product.precio * i.quantity, 0); }
}
