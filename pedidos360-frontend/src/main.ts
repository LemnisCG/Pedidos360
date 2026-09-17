import 'zone.js';
import { registerLocaleData } from '@angular/common';
import localeEsCL from '@angular/common/locales/es-CL';
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

// Registramos el locale de Chile para mostrar los precios como $59.990, $29.990, etc.
registerLocaleData(localeEsCL);

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
