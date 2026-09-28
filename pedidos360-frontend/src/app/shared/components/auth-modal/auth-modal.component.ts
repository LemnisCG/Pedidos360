import { Component, effect, signal } from '@angular/core';
import { AuthMode, AuthProvider, AuthService } from '../../../core/auth/auth.service';
import { CartService } from '../../../core/services/cart.service';

/**
 * Modal de autenticación de Pedidos360.
 * Usa controles HTML nativos para que el foco y la escritura no dependan
 * de un motor de formularios ni se reinicien durante el change detection.
 */
@Component({
  selector: 'app-auth-modal',
  standalone: true,
  templateUrl: './auth-modal.component.html',
  styleUrl: './auth-modal.component.css',
})
export class AuthModalComponent {
  readonly showPassword = signal(false);
  readonly showRepeat = signal(false);

  constructor(
    public readonly auth: AuthService,
    private readonly cart: CartService,
  ) {
    // Al abrir/cambiar el modal solo reiniciamos los estados visuales.
    // El mensaje de OAuth NO se limpia aquí: openWithError() lo establece
    // después de regresar de Facebook/Microsoft y debe quedar visible.
    effect(() => {
      if (!this.auth.modal()) return;
      this.showPassword.set(false);
      this.showRepeat.set(false);
    });
  }

  /**
   * Lee el formulario mediante FormData y ejecuta login o registro local.
   * Los inputs siguen siendo HTML nativo, por lo que siempre aceptan teclado,
   * autocompletado y gestores de contraseñas del navegador.
   */
  async submit(mode: AuthMode, form: HTMLFormElement, event: Event) {
    event.preventDefault();

    if (!form.checkValidity()) {
      form.reportValidity();
      this.auth.error.set('Completa correctamente los campos obligatorios.');
      return;
    }

    const data = new FormData(form);
    const nombre = String(data.get('nombre') ?? '').trim();
    const apellido = String(data.get('apellido') ?? '').trim();
    const email = String(data.get('email') ?? '').trim();
    const password = String(data.get('password') ?? '');
    const repeat = String(data.get('repeat') ?? '');

    if (mode === 'register' && password !== repeat) {
      this.auth.error.set('Las contraseñas no coinciden.');
      return;
    }

    try {
      if (mode === 'register') {
        await this.auth.register(nombre, apellido, email, password);
      } else {
        await this.auth.login(email, password);
      }
      await this.cart.load();
    } catch {
      // AuthService deja el mensaje de error listo para mostrarse en el modal.
    }
  }

  /** Alterna entre los formularios sin recargar la aplicación. */
  changeMode(mode: AuthMode) {
    this.auth.switchMode(mode);
  }

  /** Redirige al flujo OAuth 2.0 del proveedor elegido. */
  oauth(provider: Exclude<AuthProvider, 'local'>) {
    this.auth.oauth(provider);
  }
}
