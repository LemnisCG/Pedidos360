import { ApplicationConfig, inject, provideAppInitializer, provideZoneChangeDetection } from '@angular/core';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import {
  MSAL_GUARD_CONFIG,
  MSAL_INSTANCE,
  MSAL_INTERCEPTOR_CONFIG,
  MsalBroadcastService,
  MsalGuard,
  MsalInterceptor,
  MsalService,
} from '@azure/msal-angular';
import { routes } from './app.routes';
import { AuthService } from './core/auth/auth.service';
import {
  createEntraExternalIdPublicClientApplication,
  createMsalGuardConfigurationForPrivateRoutes,
  createMsalInterceptorConfigurationForApiGateway,
} from './core/auth/entra-external-id.config';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    // MsalInterceptor es un interceptor basado en clase, por eso se registra mediante DI.
    provideHttpClient(withInterceptorsFromDi()),
    { provide: HTTP_INTERCEPTORS, useClass: MsalInterceptor, multi: true },
    { provide: MSAL_INSTANCE, useFactory: createEntraExternalIdPublicClientApplication },
    { provide: MSAL_GUARD_CONFIG, useFactory: createMsalGuardConfigurationForPrivateRoutes },
    { provide: MSAL_INTERCEPTOR_CONFIG, useFactory: createMsalInterceptorConfigurationForApiGateway },
    MsalService,
    MsalGuard,
    MsalBroadcastService,
    provideAppInitializer(() => inject(AuthService).completeSignInRedirectAndSelectActiveAccount()),
  ]
};
