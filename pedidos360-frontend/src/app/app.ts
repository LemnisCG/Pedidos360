import { Component, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from './shared/components/header/header.component';
import { FooterComponent } from './shared/components/footer/footer.component';
import { AuthModalComponent } from './shared/components/auth-modal/auth-modal.component';
import { ProductService } from './core/services/product.service';
import { CartService } from './core/services/cart.service';
import { AuthService } from './core/auth/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, HeaderComponent, FooterComponent, AuthModalComponent],
  template: `
    <div class="app-shell">
      <app-header></app-header>
      <main class="page-shell"><router-outlet></router-outlet></main>
      <app-footer></app-footer>
      <app-auth-modal></app-auth-modal>
    </div>
  `,
})
export class App implements OnInit {
  constructor(
    private readonly products: ProductService,
    private readonly cart: CartService,
    private readonly auth: AuthService,
  ) {}

  async ngOnInit() {
    // Si un proveedor OAuth falla, el backend regresa al frontend con un mensaje seguro.
    const params = new URLSearchParams(window.location.search);
    const oauthError = params.get('oauth_error');
    if (oauthError) {
      this.auth.openWithError(oauthError);
      window.history.replaceState({}, document.title, window.location.pathname);
    }

    // Facebook puede añadir #_=_ a redirects antiguos. Si llega directo a :4200,
    // significa que el callback configurado en Meta no está pasando por el Gateway.
    if (window.location.hash === '#_=_') {
      this.auth.openWithError(
        'Facebook regresó directamente al frontend. Verifica que la Redirect URI de Meta sea exactamente http://localhost:8080/api/auth/oauth2/callback/facebook.'
      );
      window.history.replaceState({}, document.title, window.location.pathname);
    }

    // El catálogo es público; el carrito se consulta solo cuando ya existe una sesión válida.
    await this.products.load();
    if (this.auth.isLoggedIn()) await this.cart.load();
  }
}
