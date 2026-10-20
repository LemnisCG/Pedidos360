/**
 * Configuración de producción (ng build). El frontend desplegado en AWS consume el API Manager
 * (Amazon API Gateway), que expone la versión v1 de la API y reenvía cada llamada al BFF.
 */
export const environment = {
  apiBaseUrl: 'https://13ldsuv6ac.execute-api.us-east-1.amazonaws.com',
  apiPathPrefix: '/api/v1',
};
