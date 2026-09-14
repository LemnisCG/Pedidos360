// src/app/app.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { MsalService } from '@azure/msal-angular';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div style="padding: 2rem; font-family: sans-serif;">
      <h1>Pedidos360 - Frontend</h1>

      <button *ngIf="!isLoggedIn" (click)="login()">Iniciar Sesión / Registrarse</button>
      
      <div *ngIf="isLoggedIn">
        <p>Sesión activa</p>
        <button (click)="llamarBff()">Sincronizar con BFF</button>
        <button (click)="logout()">Cerrar Sesión</button>
      </div>

      <pre *ngIf="backendResponse">{{ backendResponse | json }}</pre>
    </div>
  `
})
export class AppComponent implements OnInit {
  isLoggedIn = false;
  backendResponse: any = null;

  constructor(private authService: MsalService, private http: HttpClient) {}

  async ngOnInit() {
    await this.authService.instance.initialize();
    await this.authService.instance.handleRedirectPromise();
    this.checkAccount();
  }

  login() {
    this.authService.loginRedirect({
      scopes: ['api://6314fdda-9417-40f2-8ebe-9cc6ef7cd4ed/BFF.Access']
    });
  }

  logout() {
    this.authService.logoutRedirect();
  }

  checkAccount() {
    this.isLoggedIn = this.authService.instance.getAllAccounts().length > 0;
  }

  llamarBff() {
    // El MsalInterceptor inyectará el Bearer Token automáticamente
    this.http.get('http://localhost:8080/api/auth/login').subscribe({
      next: (res) => this.backendResponse = res,
      error: (err) => console.error('Error al conectar con BFF:', err)
    });
  }
}