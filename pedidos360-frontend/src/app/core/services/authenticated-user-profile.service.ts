import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { API_URL } from '../config/api';

/** Pide a msvc-usuario que cree o actualice el perfil local del usuario que inició sesión en Entra. */
@Injectable({ providedIn: 'root' })
export class AuthenticatedUserProfileService {
  constructor(private http: HttpClient) {}

  async synchronizeProfileWithBackend() {
    try { await firstValueFrom(this.http.get(`${API_URL}/usuarios/me`)); }
    catch (error) { console.error('No se pudo sincronizar el perfil del usuario', error); }
  }
}
