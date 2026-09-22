import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from './shared/components/header/header.component';
import { FooterComponent } from './shared/components/footer/footer.component';
import { AuthService } from './core/auth/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, HeaderComponent, FooterComponent],
  template: `
    <div class="app-shell">
      <app-header></app-header>
      <main class="page-shell"><router-outlet></router-outlet></main>
      <app-footer></app-footer>
    </div>
  `
})
export class App implements OnInit {
  constructor(public auth: AuthService, private cdr: ChangeDetectorRef) {}

  async ngOnInit() {
    // Inicializa Microsoft Entra External ID una sola vez al arrancar la aplicación.
    await this.auth.initialize();
    this.cdr.detectChanges();
  }
}
