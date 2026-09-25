import Keycloak from 'keycloak-js'

export const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:8081',
  realm: import.meta.env.VITE_KEYCLOAK_REALM ?? 'security-poc',
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'poc-frontend'
})

export function accessToken(): string | undefined {
  return keycloak.token
}

export function identity(): {
  username: string
  tenant: string
  roles: string[]
} {
  const claims = keycloak.tokenParsed ?? {}
  const realmAccess = claims.realm_access as { roles?: string[] } | undefined
  return {
    username: claims.preferred_username ?? claims.sub ?? 'unknown',
    tenant: claims.tenant_id ?? 'unknown',
    roles: realmAccess?.roles ?? []
  }
}
