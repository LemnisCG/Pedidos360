import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { API_BASE_URL } from '../config/api';
import { CatalogBanner, CatalogResponse, Product } from '../models/product.model';

@Injectable({ providedIn: 'root' })
export class ProductService {
  readonly products = signal<Product[]>([]);
  readonly banner = signal<CatalogBanner>({ etiqueta: 'CATÁLOGO DIGITAL', titulo: 'TU PRÓXIMA AVENTURA COMIENZA AQUÍ', subtitulo: 'Clásicos inolvidables y nuevos mundos, reunidos en un catálogo preparado para jugar.' });
  readonly loading = signal(false);
  constructor(private http: HttpClient) {}
  async load() {
    this.loading.set(true);
    try { const data = await firstValueFrom(this.http.get<CatalogResponse>(`${API_BASE_URL}/api/catalogo`)); this.products.set(data.productos || []); if (data.banner) this.banner.set(data.banner); }
    finally { this.loading.set(false); }
  }
  async get(id: number) { return firstValueFrom(this.http.get<Product>(`${API_BASE_URL}/api/productos/${id}`)); }
}
