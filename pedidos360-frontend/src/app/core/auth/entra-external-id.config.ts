import {
  BrowserCacheLocation,
  InteractionType,
  IPublicClientApplication,
  PublicClientApplication,
} from '@azure/msal-browser';
import { MsalGuardConfiguration, MsalInterceptorConfiguration } from '@azure/msal-angular';
import { API_BASE_URL } from '../config/api';

/**
 * Datos públicos del registro de aplicación "pedidos360-frontend" en Microsoft Entra External ID.
 * Ninguno es secreto: una SPA con MSAL usa Authorization Code + PKCE y no necesita Client Secret.
 */
export const ENTRA_EXTERNAL_ID_TENANT_SUBDOMAIN = 'pedidos360auth';
export const ENTRA_EXTERNAL_ID_APPLICATION_CLIENT_ID = '6314fdda-9417-40f2-8ebe-9cc6ef7cd4ed';
export const ENTRA_EXTERNAL_ID_TENANT_ID = '92aabe28-d724-4ac3-a20c-45088143bf29';
export const ENTRA_EXTERNAL_ID_AUTHORITY_HOST_BY_SUBDOMAIN = `${ENTRA_EXTERNAL_ID_TENANT_SUBDOMAIN}.ciamlogin.com`;
export const ENTRA_EXTERNAL_ID_AUTHORITY_URL = `https://${ENTRA_EXTERNAL_ID_AUTHORITY_HOST_BY_SUBDOMAIN}/`;

/**
 * El tenant firma sus tokens con un emisor cuyo host es el ID del tenant, no el subdominio.
 * MSAL rechaza el login (endpoints_resolution_error) si ese host no está en knownAuthorities.
 */
export const ENTRA_EXTERNAL_ID_ISSUER_HOST_BY_TENANT_ID = `${ENTRA_EXTERNAL_ID_TENANT_ID}.ciamlogin.com`;

/** Scope expuesto en "Expose an API": identifica al API Gateway (BFF) como destinatario del access token. */
export const API_GATEWAY_ACCESS_TOKEN_SCOPE = `api://${ENTRA_EXTERNAL_ID_APPLICATION_CLIENT_ID}/BFF.Access`;

/** Valores de los App Roles definidos en Entra; llegan en el claim "roles" del token. */
export const ENTRA_APP_ROLE_ADMINISTRADOR = 'Administrador';
export const ENTRA_APP_ROLE_CLIENTE = 'Cliente';

/** Único endpoint del gateway que se puede consultar sin iniciar sesión. */
const PUBLIC_CATALOG_ENDPOINT_URL = `${API_BASE_URL}/api/catalogo`;
const PROTECTED_API_GATEWAY_ENDPOINTS_PATTERN = `${API_BASE_URL}/api/*`;

export function createEntraExternalIdPublicClientApplication(): IPublicClientApplication {
  return new PublicClientApplication({
    auth: {
      clientId: ENTRA_EXTERNAL_ID_APPLICATION_CLIENT_ID,
      authority: ENTRA_EXTERNAL_ID_AUTHORITY_URL,
      knownAuthorities: [ENTRA_EXTERNAL_ID_AUTHORITY_HOST_BY_SUBDOMAIN, ENTRA_EXTERNAL_ID_ISSUER_HOST_BY_TENANT_ID],
      redirectUri: window.location.origin,
      postLogoutRedirectUri: window.location.origin,
    },
    cache: {
      // sessionStorage: la sesión termina al cerrar la pestaña y los tokens no quedan persistidos en disco.
      cacheLocation: BrowserCacheLocation.SessionStorage,
    },
  });
}

/** MsalGuard: una ruta privada sin sesión redirige al inicio de sesión de Entra. */
export function createMsalGuardConfigurationForPrivateRoutes(): MsalGuardConfiguration {
  return {
    interactionType: InteractionType.Redirect,
    authRequest: { scopes: [API_GATEWAY_ACCESS_TOKEN_SCOPE] },
  };
}

/**
 * MsalInterceptor: adjunta el access token a las llamadas al gateway.
 * El orden importa: gana la primera entrada que coincide, por eso el catálogo público (null) va antes.
 */
export function createMsalInterceptorConfigurationForApiGateway(): MsalInterceptorConfiguration {
  const protectedResourceMap = new Map<string, Array<string> | null>([
    [PUBLIC_CATALOG_ENDPOINT_URL, null],
    [PROTECTED_API_GATEWAY_ENDPOINTS_PATTERN, [API_GATEWAY_ACCESS_TOKEN_SCOPE]],
  ]);

  return { interactionType: InteractionType.Redirect, protectedResourceMap };
}
