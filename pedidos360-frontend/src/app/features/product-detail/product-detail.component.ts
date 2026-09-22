import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ProductService } from '../../core/services/product.service';
import { CartService } from '../../core/services/cart.service';
import { Product } from '../../core/models/product.model';

@Component({
  selector: 'app-product-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './product-detail.component.html',
  styleUrl: './product-detail.component.css'
})
export class ProductDetailComponent {
  readonly product = signal<Product | null>(null);
  readonly loading = signal(true);

  constructor(
    route: ActivatedRoute,
    service: ProductService,
    private readonly cart: CartService,
    private readonly router: Router
  ) {
    const id = Number(route.snapshot.paramMap.get('id'));

    // Signal para que el detalle se muestre apenas termina la petición HTTP.
    service.get(id).subscribe({
      next: (product) => {
        this.product.set(product ?? null);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  add() {
    const product = this.product();
    if (!product) return;
    this.cart.add(product);
    this.router.navigate(['/cart']);
  }
}
