import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { CartService } from '../../../core/services/cart.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css',
})
export class HeaderComponent {
  constructor(
    public auth: AuthService,
    public cart: CartService,
  ) {}

  /** Limpia también el carrito visual para no mezclar sesiones de usuarios distintos. */
  logout() {
    this.cart.clearLocal();
    this.auth.logout();
  }
}
