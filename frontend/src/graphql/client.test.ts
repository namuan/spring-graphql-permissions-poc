import { describe, expect, it, vi } from 'vitest'
import { graphqlRequest } from './client'

vi.mock('../auth/keycloak', () => ({ accessToken: () => 'test-token' }))

describe('graphqlRequest', () => {
  it('sends the bearer token and returns the HTTP status', async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      status: 200,
      json: async () => ({ data: { myOrders: [] } })
    })
    vi.stubGlobal('fetch', fetchMock)

    const result = await graphqlRequest('query GetMyOrders { myOrders { id } }', {}, 'GetMyOrders')

    expect(result.status).toBe(200)
    expect(result.body).toEqual({ data: { myOrders: [] } })
    const [, options] = fetchMock.mock.calls[0]
    expect(options.headers.Authorization).toBe('Bearer test-token')
    expect(JSON.parse(options.body).operationName).toBe('GetMyOrders')
  })
})
