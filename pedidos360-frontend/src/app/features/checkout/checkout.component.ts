import { CurrencyPipe } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CartService } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import { ProductService } from '../../core/services/product.service';
import { BackButtonComponent } from '../../shared/components/back-button/back-button.component';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [FormsModule, CurrencyPipe, BackButtonComponent],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.css',
})
export class CheckoutComponent {
  numeroTarjeta = '4242 4242 4242 4242';
  expiracion = '12 / 29';
  codigo = '123';
  direccion = '';
  busy = signal(false);
  error = signal('');

  constructor(
    public cart: CartService,
    private orders: OrderService,
    private products: ProductService,
    private router: Router,
  ) {}

  /**
   * Ejecuta el checkout y vuelve a consultar el catálogo para que el stock
   * que ve el usuario coincida inmediatamente con PostgreSQL.
   */
  async pay() {
    if (!this.direccion.trim()) {
      this.error.set('Ingresa una dirección de envío.');
      return;
    }

    this.busy.set(true);
    this.error.set('');

    try {
      const order = await this.orders.checkout(
        this.direccion,
        this.numeroTarjeta,
        this.expiracion,
        this.codigo,
        this.cart.lines(),
      );

      this.cart.clearLocal();
      await this.products.load();
      await this.router.navigate(['/orders', order.id, 'created']);
    } catch (error: any) {
      this.error.set(
        error?.error?.error ||
          error?.error?.message ||
          'No fue posible completar el pedido.',
      );
    } finally {
      this.busy.set(false);
    }
  }
}
