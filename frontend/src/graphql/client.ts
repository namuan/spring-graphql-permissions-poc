import { accessToken } from '../auth/keycloak'

export type GraphQLResult = {
  status: number
  body: unknown
}

export async function graphqlRequest(
  query: string,
  variables: Record<string, unknown> = {},
  operationName?: string
): Promise<GraphQLResult> {
  const response = await fetch('/graphql', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(accessToken() ? { Authorization: `Bearer ${accessToken()}` } : {})
    },
    body: JSON.stringify({ query, variables, operationName })
  })
  return { status: response.status, body: await response.json() }
}
