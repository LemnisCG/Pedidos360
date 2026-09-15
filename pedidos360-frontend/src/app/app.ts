import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { PublicClientApplication, BrowserCacheLocation, ProtocolMode } from '@azure/msal-browser';

const msalInstance = new PublicClientApplication({
  auth: {
    clientId: '6314fdda-9417-40f2-8ebe-9cc6ef7cd4ed',
    authority: 'https://pedidos360auth.ciamlogin.com/92aabe28-d724-4ac3-a20c-45088143bf29/v2.0',
    redirectUri: 'http://localhost:4200',
    knownAuthorities: ['pedidos360auth.ciamlogin.com']
  },
  cache: {
    cacheLocation: BrowserCacheLocation.LocalStorage
  },
  system: {
    protocolMode: ProtocolMode.OIDC
  }
});

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div style="padding: 2.5rem; font-family: system-ui, sans-serif;">
      <h1>Pedidos360 - Frontend</h1>
      <hr style="margin-bottom: 1.5rem;" />

      <!-- Estado mientras carga -->
      <p *ngIf="cargando"><strong>Estado:</strong> Procesando autenticación...</p>

      <!-- Vista sin autenticar -->
      <div *ngIf="!cargando && !isLoggedIn" style="display: flex; gap: 10px;">
        <p>No has iniciado sesión.</p>
        <button (click)="login()" style="padding: 8px 16px; cursor: pointer;">
          Iniciar Sesión
        </button>
        <button (click)="registrar()" style="padding: 8px 16px; cursor: pointer; background: #0078d4; color: white; border: none; border-radius: 4px;">
          Crear Cuenta
        </button>
      </div>

      <!-- Vista autenticado -->
      <div *ngIf="!cargando && isLoggedIn">
        <p><strong>Estado:</strong> Sesión Activa ✅</p>
        <p><strong>Usuario:</strong> {{ cuentaActual?.name }} ({{ cuentaActual?.username }})</p>
        
        <div style="margin: 1rem 0; display: flex; gap: 10px;">
          <button (click)="sincronizarConBackend()" style="padding: 8px 16px; cursor: pointer;">
            Guardar / Sincronizar con Perfil
          </button>
          <button (click)="logout()" style="padding: 8px 16px; cursor: pointer;">
            Cerrar Sesión
          </button>
        </div>
      </div>

      <!-- Respuesta del BFF / ms-perfil -->
      <div *ngIf="backendResponse" style="margin-top: 2rem; background: #f4f4f4; padding: 1rem; border-radius: 6px;">
        <h3>Datos guardados en PostgreSQL:</h3>
        <pre>{{ backendResponse | json }}</pre>
      </div>
    </div>
  `
})
export class App implements OnInit {
  isLoggedIn = false;
  cargando = true;
  cuentaActual: any = null;
  backendResponse: any = null;

  constructor(
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) { }

  async ngOnInit() {
    try {
      await msalInstance.initialize();
      // Procesa el código o token que viene de Azure en la URL de retorno
      const response = await msalInstance.handleRedirectPromise();

      if (response?.account) {
        msalInstance.setActiveAccount(response.account);
      }

      this.evaluarSesion();
    } catch (error) {
      console.error('Error durante la inicialización de MSAL:', error);
    } finally {
      this.cargando = false;
      this.cdr.detectChanges(); // Fuerza a Angular a refrescar la vista
    }
  }

  evaluarSesion() {
    const cuentas = msalInstance.getAllAccounts();
    if (cuentas.length > 0) {
      this.isLoggedIn = true;
      this.cuentaActual = cuentas[0];
      msalInstance.setActiveAccount(cuentas[0]);
    } else {
      this.isLoggedIn = false;
      this.cuentaActual = null;
    }
  }

  login() {
    msalInstance.loginRedirect({
      scopes: ['api://6314fdda-9417-40f2-8ebe-9cc6ef7cd4ed/BFF.Access']
    });
  }

  registrar() {
    msalInstance.loginRedirect({
      scopes: ['api://6314fdda-9417-40f2-8ebe-9cc6ef7cd4ed/BFF.Access'],
      prompt: 'create' // Le indica a Azure CIAM que abra directamente la pantalla de crear cuenta
    });
  }

  logout() {
    msalInstance.logoutRedirect({
      postLogoutRedirectUri: 'http://localhost:4200'
    });
  }

  async sincronizarConBackend() {
    try {
      // Obtener el token de acceso silenciosamente
      const authResult = await msalInstance.acquireTokenSilent({
        scopes: ['api://6314fdda-9417-40f2-8ebe-9cc6ef7cd4ed/BFF.Access'],
        account: this.cuentaActual
      });

      const headers = new HttpHeaders({
        'Authorization': `Bearer ${authResult.accessToken}`
      });

      console.log('Token JWT enviado al backend:', authResult.accessToken);

      // El endpoint correcto configurado en tu Spring Boot (AuthController) es /api/v1/auth/login
      this.http.get('http://localhost:8080/api/v1/auth/login', { headers }).subscribe({
        next: (data) => {
          this.backendResponse = data;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('Error al comunicar con BFF:', err);
        }
      });
    } catch (error) {
      console.error('Error obteniendo token:', error);
      // Si falla silenciosamente (e.g., token expirado), se debería llamar a msalInstance.loginRedirect() o similar
    }
  }
}