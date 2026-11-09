import createClient from 'openapi-fetch'
import type { components, paths } from './schema'

type Schemas = components['schemas']
export type PlayerSearchResult = Schemas['PlayerSearchResult']
export type PlayerProfile = Schemas['PlayerProfile']
export type PlayerGame = Schemas['PlayerGame']
export type StatLine = Schemas['StatLine']
export type SeasonAverage = Schemas['SeasonAverage']
export type AverageLine = Schemas['AverageLine']
export type BoxScore = Schemas['BoxScore']
export type TeamBoxScore = Schemas['TeamBoxScore']
export type PlayerLine = Schemas['PlayerLine']
export type DataStatus = Schemas['DataStatus']
export type SeasonType = PlayerGame['seasonType']

export const api = createClient<paths>({
  baseUrl: window.location.origin,
  // looked up on every request, so a fetch mock installed after this module loads still applies
  fetch: (request) => fetch(request),
})

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

interface ApiResult<T> {
  data?: T
  error?: unknown
  response: Response
}

/** openapi-fetch returns errors as values, TanStack Query expects them thrown. */
export async function unwrap<T>(request: Promise<ApiResult<T>>): Promise<T> {
  const { data, error, response } = await request
  if (!response.ok || data === undefined) {
    throw new ApiError(response.status, problemDetail(error) ?? `HTTP ${response.status}`)
  }
  return data
}

function problemDetail(error: unknown): string | undefined {
  if (typeof error === 'object' && error !== null && 'detail' in error && typeof error.detail === 'string') {
    return error.detail
  }
  return undefined
}
