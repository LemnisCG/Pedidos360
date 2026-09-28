import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { Router } from '@angular/router';
import { API_BASE_URL } from '../config/api';

export type AuthMode = 'login' | 'register';
export type AuthProvider = 'local' | 'google' | 'facebook' | 'discord' | 'microsoft';
export interface AuthUser { id: string; nombre: string; apellido: string; email: string; provider: AuthProvider; }
interface AuthResponse { token: string; usuario: AuthUser; }

/** Estado de autenticación único para formulario local, OAuth y JWT interno. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenKey = 'pedidos360.jwt';
  readonly user = signal<AuthUser | null>(this.userFromToken(localStorage.getItem(this.tokenKey)));
  readonly modal = signal<AuthMode | null>(null);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly isLoggedIn = computed(() => !!this.user());

  constructor(private http: HttpClient, private router: Router) {}

  open(mode: AuthMode = 'login') { this.error.set(''); this.modal.set(mode); }
  openWithError(message: string) { this.modal.set('login'); this.error.set(message); }
  close() { if (!this.busy()) this.modal.set(null); }
  switchMode(mode: AuthMode) { this.error.set(''); this.modal.set(mode); }
  get token() { return localStorage.getItem(this.tokenKey); }

  async login(email: string, password: string) {
    this.busy.set(true); this.error.set('');
    try {
      const response = await firstValueFrom(this.http.post<AuthResponse>(`${API_BASE_URL}/api/auth/login`, { email, password }));
      this.accept(response.token, response.usuario);
      this.modal.set(null); await this.router.navigateByUrl('/');
    } catch (e: any) { this.error.set(this.authErrorMessage(e, 'login')); throw e; }
    finally { this.busy.set(false); }
  }

  async register(nombre: string, apellido: string, email: string, password: string) {
    this.busy.set(true); this.error.set('');
    try {
      const response = await firstValueFrom(this.http.post<AuthResponse>(`${API_BASE_URL}/api/auth/register`, { nombre, apellido, email, password }));
      this.accept(response.token, response.usuario);
      this.modal.set(null); await this.router.navigateByUrl('/');
    } catch (e: any) { this.error.set(this.authErrorMessage(e, 'register')); throw e; }
    finally { this.busy.set(false); }
  }

  oauth(provider: Exclude<AuthProvider, 'local'>) {
    // El API Gateway redirige al proveedor; el callback vuelve con un JWT interno de Pedidos360.
    window.location.href = `${API_BASE_URL}/api/auth/oauth2/${provider}/authorize`;
  }

  async acceptOAuthFragment(fragment: string | null) {
    const params = new URLSearchParams(fragment || '');
    const token = params.get('token');
    if (!token) throw new Error('El proveedor no devolvió un token válido.');
    this.accept(token, this.userFromToken(token));
    await this.router.navigateByUrl('/');
  }

  logout() { localStorage.removeItem(this.tokenKey); this.user.set(null); this.router.navigateByUrl('/'); }

  /** Convierte respuestas HTTP del backend en mensajes útiles para el usuario. */
  private authErrorMessage(error: any, action: 'login' | 'register'): string {
    const status = Number(error?.status ?? 0);
    const backendMessage = this.extractBackendMessage(error?.error);

    // 409 se trata primero para no terminar mostrando solamente "Conflict".
    if (status === 409) {
      if (backendMessage && backendMessage.toLowerCase() !== 'conflict') return backendMessage;
      return 'Ya existe una cuenta con este correo. Inicia sesión o utiliza otro correo electrónico.';
    }

    if (backendMessage && !['bad request', 'unauthorized'].includes(backendMessage.toLowerCase())) {
      return backendMessage;
    }

    if (status === 401) {
      return 'Correo electrónico o contraseña incorrectos.';
    }
    if (status === 400) {
      return 'Revisa los datos ingresados e inténtalo nuevamente.';
    }
    if (status === 0) {
      return 'No se pudo conectar con el backend de Pedidos360.';
    }

    return action === 'register'
      ? 'No fue posible crear la cuenta.'
      : 'No fue posible iniciar sesión.';
  }

  /** Soporta errores JSON y respuestas de texto del Gateway/servidor. */
  private extractBackendMessage(body: any): string {
    if (!body) return '';
    if (typeof body?.message === 'string' && body.message.trim()) return body.message.trim();

    if (typeof body === 'string') {
      const text = body.trim();
      if (!text) return '';
      try {
        const parsed = JSON.parse(text);
        if (typeof parsed?.message === 'string') return parsed.message.trim();
      } catch {
        return text;
      }
    }

    return '';
  }

  private accept(token: string, explicit: AuthUser | null) {
    localStorage.setItem(this.tokenKey, token);
    this.user.set(explicit ?? this.userFromToken(token));
  }

  private userFromToken(token: string | null): AuthUser | null {
    if (!token) return null;
    try {
      const raw = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      const padded = raw + '='.repeat((4 - raw.length % 4) % 4);
      const binary = atob(padded);
      const bytes = Uint8Array.from(binary, char => char.charCodeAt(0));
      const payload = JSON.parse(new TextDecoder().decode(bytes));
      if (payload.exp && payload.exp * 1000 < Date.now()) { localStorage.removeItem(this.tokenKey); return null; }
      const [nombre, ...rest] = String(payload.name || 'Usuario').split(' ');
      return { id: String(payload.sub), nombre, apellido: rest.join(' '), email: String(payload.email || ''), provider: (payload.provider || 'local') as AuthProvider };
    } catch { return null; }
  }
}
