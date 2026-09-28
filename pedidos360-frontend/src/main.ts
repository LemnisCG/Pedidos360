import 'zone.js';
import { registerLocaleData } from '@angular/common';
import localeEsCL from '@angular/common/locales/es-CL';
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

// Locale chileno para precios CLP y fechas del catálogo.
registerLocaleData(localeEsCL);
bootstrapApplication(App, appConfig).catch(console.error);
