package cl.duoc.pedidos360.usuario.oauth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.HashMap;
import java.util.Map;

/** Configuración externa de proveedores; los secretos siempre llegan por variables de entorno. */
@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {
    private String frontendUrl;
    private String gatewayPublicUrl;
    private Map<String, Provider> providers = new HashMap<>();

    public String getFrontendUrl() { return frontendUrl; }
    public void setFrontendUrl(String frontendUrl) { this.frontendUrl = frontendUrl; }
    public String getGatewayPublicUrl() { return gatewayPublicUrl; }
    public void setGatewayPublicUrl(String gatewayPublicUrl) { this.gatewayPublicUrl = gatewayPublicUrl; }
    public Map<String, Provider> getProviders() { return providers; }
    public void setProviders(Map<String, Provider> providers) { this.providers = providers; }

    public static class Provider {
        private String clientId;
        private String clientSecret;
        /**
         * Identificador del tenant cuando el proveedor lo requiere.
         * En Microsoft Entra External ID permite detectar configuraciones mezcladas
         * entre aplicaciones registradas en tenants distintos.
         */
        private String tenantId;
        private String authorizationUrl;
        private String tokenUrl;
        private String userInfoUrl;
        private String scope;
        private boolean pkce = true;
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getAuthorizationUrl() { return authorizationUrl; }
        public void setAuthorizationUrl(String authorizationUrl) { this.authorizationUrl = authorizationUrl; }
        public String getTokenUrl() { return tokenUrl; }
        public void setTokenUrl(String tokenUrl) { this.tokenUrl = tokenUrl; }
        public String getUserInfoUrl() { return userInfoUrl; }
        public void setUserInfoUrl(String userInfoUrl) { this.userInfoUrl = userInfoUrl; }
        public String getScope() { return scope; }
        public void setScope(String scope) { this.scope = scope; }
        public boolean isPkce() { return pkce; }
        public void setPkce(boolean pkce) { this.pkce = pkce; }
    }
}
