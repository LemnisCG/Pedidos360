import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../core/services/product.service';
import { BackButtonComponent } from '../../shared/components/back-button/back-button.component';

/**
 * Pantalla exclusiva del App Role "Administrador": edita el banner principal de la tienda.
 * El API Gateway vuelve a exigir el rol, así que un usuario sin él recibe 403 aunque llegue hasta aquí.
 */
@Component({
  selector: 'app-catalog-banner-administration',
  standalone: true,
  imports: [FormsModule, BackButtonComponent],
  templateUrl: './catalog-banner-administration.component.html',
  styleUrl: './catalog-banner-administration.component.css',
})
export class CatalogBannerAdministrationComponent {
  etiqueta: string;
  titulo: string;
  subtitulo: string;
  busy = signal(false);
  error = signal('');
  savedSuccessfully = signal(false);

  constructor(private products: ProductService) {
    const currentBanner = this.products.banner();
    this.etiqueta = currentBanner.etiqueta;
    this.titulo = currentBanner.titulo;
    this.subtitulo = currentBanner.subtitulo;
  }

  async saveCatalogBanner() {
    this.savedSuccessfully.set(false);
    if (!this.etiqueta.trim() || !this.titulo.trim() || !this.subtitulo.trim()) {
      this.error.set('Completa la etiqueta, el título y el subtítulo.');
      return;
    }

    this.busy.set(true);
    this.error.set('');

    try {
      await this.products.updateCatalogBanner({ etiqueta: this.etiqueta, titulo: this.titulo, subtitulo: this.subtitulo });
      this.savedSuccessfully.set(true);
    } catch (error: any) {
      this.error.set(
        error?.status === 403
          ? 'Tu cuenta no tiene el rol Administrador.'
          : error?.error?.error || 'No fue posible guardar el banner.',
      );
    } finally {
      this.busy.set(false);
    }
  }
}
