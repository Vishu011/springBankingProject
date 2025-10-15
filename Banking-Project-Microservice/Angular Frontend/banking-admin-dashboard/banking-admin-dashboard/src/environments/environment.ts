export const environment = {
  production: false,
  apiUrl: 'http://localhost:9010', // API Gateway URL
  orchestratorUrl: 'http://localhost:9101', // AI Orchestrator base URL
  keycloak: {
    issuer: 'http://localhost:8080/realms/bank-realm', // Keycloak Realm URL
    clientId: 'bank-admin-frontend', // Client ID for Admin Angular app in Keycloak
    redirectUri: 'http://localhost:4300', // Admin Angular app base URL
    scope: 'openid profile email', // Scopes requested
    responseType: 'code', // Authorization Code Flow
    strictDiscoveryDocumentValidation: true,
    clearHashAfterLogin: true,
    nonceStateSeparator: 'auth',
  },
};
