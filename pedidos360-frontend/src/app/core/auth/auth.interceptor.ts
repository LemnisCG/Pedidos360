import { HttpInterceptorFn } from '@angular/common/http';
import { API_BASE_URL } from '../config/api';

/**
 * Adjunta el JWT interno solamente a llamadas dirigidas al API Gateway.
 * Se lee directamente de localStorage para evitar una dependencia circular HttpClient -> AuthService -> HttpClient.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = localStorage.getItem('pedidos360.jwt');
  if (token && req.url.startsWith(API_BASE_URL)) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }
  return next(req);
};
