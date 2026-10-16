import { environment } from '../../../environments/environment';

/** Raíz de todas las llamadas al backend: API Gateway con /api/v1 en producción, el BFF local con /api en desarrollo. */
export const API_URL = `${environment.apiBaseUrl}${environment.apiPathPrefix}`;
