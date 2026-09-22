import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { switchMap } from 'rxjs';
import { CartService } from '../../core/services/cart.service';
import { PaymentService } from '../../core/services/payment.service';
import { OrderService } from '../../core/services/order.service';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.css'
})
export class CheckoutComponent {
  processing = false;
  errorMessage = '';

  card = '4242 4242 4242 4242';
  exp = '12 / 29';
  cvc = '123';
  country = 'Chile';
  zip = '8320000';
  direccionEnvio = '';

  constructor(
    public cart: CartService,
    public auth: AuthService,
    private payments: PaymentService,
    private orders: OrderService,
    private router: Router
  ) {}

  place() {
    if (!this.cart.count() || this.processing) return;

    const email = this.auth.email;
    const direccion = this.direccionEnvio.trim();

    if (!email) {
      this.errorMessage = 'Debes iniciar sesión para asociar el pedido a tu correo.';
      return;
    }

    if (!direccion) {
      this.errorMessage = 'Ingresa la dirección de envío antes de realizar el pago.';
      return;
    }

    this.errorMessage = '';
    this.processing = true;
    const items = this.cart.items$.value;

    // 1) msvc-pago registra la aprobación demo.
    // 2) msvc-orden crea la orden y envía el correo real si SMTP está configurado.
    this.payments.pay(this.cart.total()).pipe(
      switchMap(payment => this.orders.create(items, payment.id, email, direccion))
    ).subscribe({
      next: order => {
        this.cart.clear();
        this.router.navigate(['/order-created', order.id]);
      },
      error: () => {
        this.processing = false;
        this.errorMessage = 'No fue posible completar el proceso. Revisa los microservicios e inténtalo nuevamente.';
      }
    });
  }
}
