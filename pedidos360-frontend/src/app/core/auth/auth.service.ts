import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { BehaviorSubject } from 'rxjs';
import { PublicClientApplication, BrowserCacheLocation, ProtocolMode, AccountInfo } from '@azure/msal-browser';

const SCOPES = ['api://6314fdda-9417-40f2-8ebe-9cc6ef7cd4ed/BFF.Access'];
const msalInstance = new PublicClientApplication({
  auth: {
    clientId: '6314fdda-9417-40f2-8ebe-9cc6ef7cd4ed',
    authority: 'https://pedidos360auth.ciamlogin.com/92aabe28-d724-4ac3-a20c-45088143bf29/v2.0',
    redirectUri: 'http://localhost:4200',
    knownAuthorities: ['pedidos360auth.ciamlogin.com']
  },
  cache: { cacheLocation: BrowserCacheLocation.LocalStorage },
  system: { protocolMode: ProtocolMode.OIDC }
});

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly account$ = new BehaviorSubject<AccountInfo | null>(null);
  readonly loading$ = new BehaviorSubject(true);
  constructor(private http: HttpClient) {}

  async initialize() {
    try {
      await msalInstance.initialize();
      const response = await msalInstance.handleRedirectPromise();
      if (response?.account) msalInstance.setActiveAccount(response.account);
      const account = msalInstance.getActiveAccount() ?? msalInstance.getAllAccounts()[0] ?? null;
      if (account) msalInstance.setActiveAccount(account);
      this.account$.next(account);
    } finally { this.loading$.next(false); }
  }
  get isLoggedIn() { return !!this.account$.value; }

  // Nombre y correo obtenidos desde la identidad autenticada en Microsoft Entra.
  get displayName(): string {
    const account = this.account$.value;
    return account?.name || account?.username || 'Usuario';
  }

  get email(): string {
    const account = this.account$.value;
    if (!account) return '';

    const claims = (account.idTokenClaims ?? {}) as Record<string, unknown>;
    const emailClaim = claims['email'];
    const emailsClaim = claims['emails'];

    if (typeof emailClaim === 'string' && emailClaim.trim()) return emailClaim.trim();
    if (Array.isArray(emailsClaim) && typeof emailsClaim[0] === 'string') return emailsClaim[0].trim();
    return account.username?.trim() ?? '';
  }
  login() { return msalInstance.loginRedirect({ scopes: SCOPES }); }
  register() { return msalInstance.loginRedirect({ scopes: SCOPES, prompt: 'create' }); }
  logout() { return msalInstance.logoutRedirect({ postLogoutRedirectUri: 'http://localhost:4200' }); }

  async syncProfile() {
    const account = this.account$.value;
    if (!account) throw new Error('No hay una sesión activa.');
    const token = await msalInstance.acquireTokenSilent({ scopes: SCOPES, account });
    const headers = new HttpHeaders({ Authorization: `Bearer ${token.accessToken}` });
    return this.http.get('http://localhost:8080/api/v1/auth/login', { headers }).toPromise();
  }
}
