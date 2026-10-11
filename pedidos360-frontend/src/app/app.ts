import { Component, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from './shared/components/header/header.component';
import { FooterComponent } from './shared/components/footer/footer.component';
import { ProductService } from './core/services/product.service';
import { CartService } from './core/services/cart.service';
import { AuthService } from './core/auth/auth.service';
import { AuthenticatedUserProfileService } from './core/services/authenticated-user-profile.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, HeaderComponent, FooterComponent],
  template: `
    <div class="app-shell">
      <app-header></app-header>
      <main class="page-shell"><router-outlet></router-outlet></main>
      <app-footer></app-footer>
    </div>
  `,
})
export class App implements OnInit {
  constructor(
    private readonly products: ProductService,
    private readonly cart: CartService,
    private readonly auth: AuthService,
    private readonly authenticatedUserProfile: AuthenticatedUserProfileService,
  ) {}

  async ngOnInit() {
    // El catálogo es público; el carrito se consulta solo cuando ya existe una sesión válida.
    await this.products.load();
    if (this.auth.isLoggedIn()) {
      await Promise.all([this.cart.load(), this.authenticatedUserProfile.synchronizeProfileWithBackend()]);
    }
  }
}
