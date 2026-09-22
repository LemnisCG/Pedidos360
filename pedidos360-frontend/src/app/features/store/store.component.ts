import { Component, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProductService } from '../../core/services/product.service';
import { Product } from '../../core/models/product.model';

@Component({
  selector: 'app-store',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './store.component.html',
  styleUrl: './store.component.css'
})
export class StoreComponent {
  // Signals: al llegar la respuesta HTTP Angular vuelve a pintar la vista inmediatamente.
  // Esto corrige el problema donde los productos aparecían recién al hacer clic en el buscador.
  readonly products = signal<Product[]>([]);
  readonly search = signal('');
  readonly loading = signal(true);

  readonly filtered = computed(() => {
    const q = this.search().toLowerCase().trim();
    const items = this.products();
    if (!q) return items;
    return items.filter((p) => `${p.nombre} ${p.categoria}`.toLowerCase().includes(q));
  });

  constructor(private readonly service: ProductService) {
    this.service.list().subscribe({
      next: (products) => {
        this.products.set(products);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  updateSearch(event: Event) {
    this.search.set((event.target as HTMLInputElement).value);
  }

  applySearch() {
    this.search.set(this.search().trim());
  }
}
