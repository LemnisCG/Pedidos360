import { Injectable, computed, signal } from '@angular/core';
import { MsalBroadcastService, MsalService } from '@azure/msal-angular';
import { AccountInfo, AuthenticationResult, EventMessage, EventType } from '@azure/msal-browser';
import { firstValueFrom } from 'rxjs';
import { API_GATEWAY_ACCESS_TOKEN_SCOPE, ENTRA_APP_ROLE_ADMINISTRADOR } from './entra-external-id.config';

export interface AuthUser { id: string; nombre: string; apellido: string; email: string; roles: string[]; }

/** Estado de autenticación de Pedidos360, respaldado por MSAL y Microsoft Entra External ID. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly user = signal<AuthUser | null>(null);
  readonly isLoggedIn = computed(() => !!this.user());

  /** Roles leídos del claim "roles" del token. Un cliente auto-registrado no trae ninguno. */
  readonly rolesFromTokenClaims = computed(() => this.user()?.roles ?? []);
  readonly hasAdministradorRole = computed(() => this.rolesFromTokenClaims().includes(ENTRA_APP_ROLE_ADMINISTRADOR));

  /** Scopes concedidos en el último access token obtenido para el API Gateway (claim "scp"). */
  readonly scopesGrantedInAccessToken = signal<string[]>([]);

  constructor(private msalService: MsalService, msalBroadcastService: MsalBroadcastService) {
    this.refreshUserFromActiveMsalAccount();
    msalBroadcastService.msalSubject$.subscribe(event => this.reactToMsalEvent(event));
  }

  /**
   * Se ejecuta antes de que arranque la aplicación: procesa la respuesta de Entra cuando
   * se vuelve del login y deja seleccionada la cuenta activa que usarán guard e interceptor.
   */
  async completeSignInRedirectAndSelectActiveAccount() {
    try {
      const redirectResult = await firstValueFrom(this.msalService.handleRedirectObservable());
      const msalInstance = this.msalService.instance;
      const account = redirectResult?.account ?? msalInstance.getActiveAccount() ?? msalInstance.getAllAccounts()[0] ?? null;
      msalInstance.setActiveAccount(account);
      if (redirectResult) this.rememberScopesGrantedInAccessToken(redirectResult);
    } catch (error) {
      // Un login cancelado o fallido no debe impedir que la tienda pública cargue.
      console.error('No se pudo completar el inicio de sesión con Entra External ID', error);
    }
    this.refreshUserFromActiveMsalAccount();
  }

  signInWithEntraExternalId() {
    this.msalService.loginRedirect({ scopes: [API_GATEWAY_ACCESS_TOKEN_SCOPE] });
  }

  /**
   * Abre la misma página de Entra que el inicio de sesión: el registro se inicia ahí con el
   * enlace "¿No tiene una cuenta? Cree una", que es el camino estándar del user flow.
   */
  signUpWithEntraExternalId() {
    this.msalService.loginRedirect({ scopes: [API_GATEWAY_ACCESS_TOKEN_SCOPE] });
  }

  logout() {
    this.msalService.logoutRedirect({ account: this.msalService.instance.getActiveAccount() });
  }

  private reactToMsalEvent(event: EventMessage) {
    if (event.eventType === EventType.LOGIN_SUCCESS || event.eventType === EventType.ACQUIRE_TOKEN_SUCCESS) {
      const result = event.payload as AuthenticationResult;
      if (result?.account) this.msalService.instance.setActiveAccount(result.account);
      if (result) this.rememberScopesGrantedInAccessToken(result);
      // Un token renovado puede traer roles distintos si se cambiaron en Entra.
      this.refreshUserFromActiveMsalAccount();
    }
    if (event.eventType === EventType.LOGOUT_SUCCESS) {
      this.scopesGrantedInAccessToken.set([]);
      this.user.set(null);
    }
  }

  private rememberScopesGrantedInAccessToken(result: AuthenticationResult) {
    if (result.accessToken && result.scopes?.length) this.scopesGrantedInAccessToken.set(result.scopes);
  }

  private refreshUserFromActiveMsalAccount() {
    this.user.set(this.userFromMsalAccount(this.msalService.instance.getActiveAccount()));
  }

  private userFromMsalAccount(account: AccountInfo | null): AuthUser | null {
    if (!account) return null;
    const claims = account.idTokenClaims ?? {};
    const email = String(claims['email'] ?? claims.preferred_username ?? account.username ?? '');
    const [nombre, ...rest] = String(claims.name || account.name || email.split('@')[0] || 'Usuario').split(' ');
    return { id: String(claims.oid ?? account.localAccountId), nombre, apellido: rest.join(' '), email, roles: claims.roles ?? [] };
  }
}
