import { CurrencyPipe } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { CartService } from '../../core/services/cart.service';
import { OrderService } from '../../core/services/order.service';
import { ProductService } from '../../core/services/product.service';
import { BackButtonComponent } from '../../shared/components/back-button/back-button.component';

type DeliveryMethod = 'domicilio' | 'retiro' | 'punto';

@Component({
  selector: 'app-checkout',
  standalone: true,
  imports: [FormsModule, CurrencyPipe, BackButtonComponent],
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.css'
})
export class CheckoutComponent {
  readonly busy = signal(false);
  readonly error = signal('');
  readonly errors = signal<Record<string, string>>({});
  readonly email: string;
  readonly nombre: string;
  readonly apellido: string;

  telefono = '';
  novedades = false;
  entrega: DeliveryMethod = 'domicilio';

  region = 'Región Metropolitana';
  comuna = '';
  ciudad = '';
  calle = '';
  complemento = '';
  referencia = '';

  mismaFacturacion = true;
  rut = '';
  factura = false;

  numeroTarjeta = '';
  expiracion = '';
  codigoSeguridad = '';
  pais = 'Chile';
  codigoPostal = '8320000';

  readonly regiones = [
    'Arica y Parinacota',
    'Tarapacá',
    'Antofagasta',
    'Atacama',
    'Coquimbo',
    'Valparaíso',
    'Región Metropolitana',
    'O’Higgins',
    'Maule',
    'Ñuble',
    'Biobío',
    'La Araucanía',
    'Los Ríos',
    'Los Lagos',
    'Aysén',
    'Magallanes'
  ];

  readonly comunasPorRegion: Record<string, string[]> = {
    'Arica y Parinacota': ['Arica', 'Camarones', 'Putre'],
    'Tarapacá': ['Iquique', 'Alto Hospicio', 'Pozo Almonte'],
    'Antofagasta': ['Antofagasta', 'Calama', 'Mejillones'],
    'Atacama': ['Copiapó', 'Caldera', 'Vallenar'],
    'Coquimbo': ['La Serena', 'Coquimbo', 'Ovalle'],
    'Valparaíso': ['Valparaíso', 'Viña del Mar', 'Concón', 'Quilpué', 'Villa Alemana', 'San Antonio'],
    'Región Metropolitana': [
      'Santiago',
      'Santiago Centro',
      'Estación Central',
      'Huechuraba',
      'Providencia',
      'Las Condes',
      'Ñuñoa',
      'La Florida',
      'Maipú',
      'Puente Alto',
      'San Bernardo',
      'Pudahuel',
      'Quilicura',
      'Recoleta',
      'Peñalolén',
      'Macul'
    ],
    'O’Higgins': ['Rancagua', 'Machalí', 'San Fernando'],
    'Maule': ['Talca', 'Curicó', 'Linares'],
    'Ñuble': ['Chillán', 'San Carlos', 'Bulnes'],
    'Biobío': ['Concepción', 'Talcahuano', 'Los Ángeles', 'Chiguayante'],
    'La Araucanía': ['Temuco', 'Padre Las Casas', 'Villarrica'],
    'Los Ríos': ['Valdivia', 'La Unión', 'Panguipulli'],
    'Los Lagos': ['Puerto Montt', 'Osorno', 'Castro'],
    'Aysén': ['Coyhaique', 'Aysén', 'Chile Chico'],
    'Magallanes': ['Punta Arenas', 'Puerto Natales', 'Porvenir']
  };

  constructor(
    public cart: CartService,
    auth: AuthService,
    private orders: OrderService,
    private products: ProductService,
    private router: Router
  ) {
    const user = auth.user();
    this.email = user?.email ?? '';
    this.nombre = user?.nombre ?? '';
    this.apellido = user?.apellido ?? '';
  }

  shippingCost() {
    return 0;
  }

  get comunasDisponibles(): string[] {
    return this.comunasPorRegion[this.region] ?? [];
  }

  onRegionChange(): void {
    if (!this.comunasDisponibles.includes(this.comuna)) {
      this.comuna = '';
    }
  }

  onPhoneInput(value: string): void {
    let digits = value.replace(/\D/g, '');
    if (digits.startsWith('56')) {
      digits = digits.slice(2);
    }
    digits = digits.slice(0, 9);

    if (digits.length <= 1) {
      this.telefono = digits;
      return;
    }

    const first = digits.slice(0, 1);
    const middle = digits.slice(1, 5);
    const last = digits.slice(5, 9);
    this.telefono = [first, middle, last].filter(Boolean).join(' ');
  }

  onCardNumberInput(value: string): void {
    const digits = value.replace(/\D/g, '').slice(0, 19);
    this.numeroTarjeta = digits.replace(/(.{4})/g, '$1 ').trim();
  }

  onExpiryInput(value: string): void {
    const digits = value.replace(/\D/g, '').slice(0, 4);
    if (digits.length <= 2) {
      this.expiracion = digits;
      return;
    }
    this.expiracion = `${digits.slice(0, 2)}/${digits.slice(2)}`;
  }

  onSecurityCodeInput(value: string): void {
    this.codigoSeguridad = value.replace(/\D/g, '').slice(0, 4);
  }

  onPostalCodeInput(value: string): void {
    this.codigoPostal = value.replace(/\D/g, '').slice(0, 7);
  }

  private validar(): boolean {
    const e: Record<string, string> = {};

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email)) {
      e['email'] = 'Inicia sesión con un correo válido.';
    }

    const tel = this.telefono.replace(/\D/g, '');
    if (!/^9\d{8}$/.test(tel)) {
      e['telefono'] = 'Ingresa un celular chileno válido. Ejemplo: 9 1234 5678.';
    }

    if (!this.nombre.trim() || !this.apellido.trim()) {
      e['nombre'] = 'Completa tu nombre en tu perfil.';
    }

    if (!this.region.trim()) {
      e['region'] = 'Selecciona una región.';
    }

    if (!this.comuna.trim()) {
      e['comuna'] = 'Selecciona una comuna.';
    }

    if (!this.ciudad.trim()) {
      e['ciudad'] = 'Ingresa la ciudad.';
    }

    if (!this.calle.trim() || this.calle.trim().length < 5) {
      e['calle'] = 'Ingresa una dirección válida.';
    }

    if (!this.pais.trim()) {
      e['pais'] = 'Selecciona un país.';
    }

    if (!/^\d{7}$/.test(this.codigoPostal.trim())) {
      e['codigoPostal'] = 'Ingresa un código postal válido de 7 dígitos.';
    }

    const cardDigits = this.numeroTarjeta.replace(/\D/g, '');
    if (cardDigits.length < 12 || cardDigits.length > 19) {
      e['numeroTarjeta'] = 'Ingresa entre 12 y 19 dígitos ficticios.';
    }

    if (!this.isExpiryValid(this.expiracion)) {
      e['expiracion'] = 'Utiliza un mes entre 01 y 12 y el formato MM/AA.';
    }

    if (!/^\d{3,4}$/.test(this.codigoSeguridad.trim())) {
      e['codigoSeguridad'] = 'Ingresa un código de seguridad válido.';
    }

    if (this.factura && !/^\d{1,2}\.?\d{3}\.?\d{3}-[0-9kK]$/.test(this.rut.trim())) {
      e['rut'] = 'Ingresa un RUT con formato válido.';
    }

    if (!this.cart.lines().length) {
      e['carrito'] = 'Tu carrito está vacío.';
    }

    this.errors.set(e);
    this.error.set(Object.keys(e).length ? 'Revisa los campos marcados antes de confirmar el pedido.' : '');
    return Object.keys(e).length === 0;
  }

  private isExpiryValid(value: string): boolean {
    if (!/^(0[1-9]|1[0-2])\/(\d{2})$/.test(value)) {
      return false;
    }

    // Solo validamos el formato y el mes: este checkout no procesa pagos reales.
    return true;
  }

  private direccionCompleta(): string {
    const partes = [this.calle, this.complemento, this.comuna, this.ciudad, this.region, 'Chile'];
    const detalles = [
      `Entrega: ${this.entrega === 'domicilio' ? 'Envío a domicilio' : this.entrega === 'retiro' ? 'Retiro en tienda' : 'Retiro en punto de entrega'}`,
      `Teléfono: ${this.telefono.trim()}`,
      this.referencia.trim() ? `Indicaciones: ${this.referencia.trim()}` : '',
      this.factura ? `Factura solicitada: ${this.rut.trim()}` : ''
    ];

    return [...partes, ...detalles]
      .filter((value) => !!value.trim())
      .join(' · ')
      .slice(0, 600);
  }

  async pay(): Promise<void> {
    if (this.busy() || !this.validar()) {
      return;
    }

    this.busy.set(true);

    try {
      const order = await this.orders.checkout(
        this.direccionCompleta(),
        '4242424242424242', // Identificador fijo de prueba; nunca enviamos lo escrito en el formulario.
        '12/29',
        '123',
        this.cart.lines()
      );
      this.cart.clearLocal();

      try {
        await this.products.load();
      } catch {
        // El pedido ya quedó confirmado; si falla la recarga, no rompemos el flujo.
      }

      await this.router.navigate(['/orders', order.id, 'created']);
    } catch (err: any) {
      this.error.set(err?.error?.message || err?.error?.error || 'No fue posible completar el pedido.');
    } finally {
      this.busy.set(false);
    }
  }
}
