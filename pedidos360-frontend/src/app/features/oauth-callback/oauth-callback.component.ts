import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { CartService } from '../../core/services/cart.service';

/** Pantalla temporal que recibe el JWT interno después de OAuth. */
@Component({
  selector: 'app-oauth-callback',
  standalone: true,
  template: `
    <section class="oauth-loading">
      <span></span>
      <h1>Completando inicio de sesión...</h1>
      <p>Estamos validando tu identidad y preparando Pedidos360.</p>
    </section>
  `,
  styles: [`
    .oauth-loading{min-height:70vh;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;color:#526071}
    .oauth-loading span{width:48px;height:48px;border:4px solid #dbeafe;border-top-color:#0d6efd;border-radius:50%;animation:s .8s linear infinite}
    @keyframes s{to{transform:rotate(360deg)}}
  `],
})
export class OAuthCallbackComponent implements OnInit {
  constructor(
    private readonly auth: AuthService,
    private readonly cart: CartService,
    private readonly router: Router,
  ) {}

  async ngOnInit() {
    try {
      await this.auth.acceptOAuthFragment(location.hash.slice(1));
      await this.cart.load();
    } catch (error: any) {
      await this.router.navigateByUrl('/');
      this.auth.openWithError(error?.message || 'No se pudo completar el inicio de sesión externo.');
    }
  }
}
