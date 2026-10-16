/** Configuración de desarrollo (ng serve): el frontend llama directo al BFF local, sin API Gateway ni versión. */
export const environment = {
  apiBaseUrl: 'http://localhost:8080',
  apiPathPrefix: '/api',
};
