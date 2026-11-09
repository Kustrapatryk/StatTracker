import { keepPreviousData, skipToken, useQuery } from '@tanstack/react-query'
import { api, unwrap } from './client'

const MINUTE = 60_000

export function usePlayerSearch(query: string) {
  return useQuery({
    queryKey: ['players', 'search', query],
    queryFn:
      query.length >= 2
        ? ({ signal }) => unwrap(api.GET('/api/players', { params: { query: { query, limit: 10 } }, signal }))
        : skipToken,
    staleTime: 5 * MINUTE,
    placeholderData: keepPreviousData,
  })
}

export function usePlayer(playerId: number) {
  return useQuery({
    queryKey: ['players', playerId],
    queryFn: ({ signal }) => unwrap(api.GET('/api/players/{playerId}', { params: { path: { playerId } }, signal })),
    staleTime: 10 * MINUTE,
  })
}

/** Most recent first. */
export function usePlayerGames(playerId: number, limit: number) {
  return useQuery({
    queryKey: ['players', playerId, 'games', limit],
    queryFn: ({ signal }) =>
      unwrap(api.GET('/api/players/{playerId}/games', { params: { path: { playerId }, query: { limit } }, signal })),
    staleTime: 5 * MINUTE,
  })
}

export function useSeasonAverages(playerId: number) {
  return useQuery({
    queryKey: ['players', playerId, 'season-averages'],
    queryFn: ({ signal }) =>
      unwrap(api.GET('/api/players/{playerId}/season-averages', { params: { path: { playerId } }, signal })),
    staleTime: 5 * MINUTE,
  })
}

export function useBoxScore(gameId: string | undefined) {
  return useQuery({
    queryKey: ['games', gameId, 'boxscore'],
    queryFn: gameId
      ? ({ signal }) => unwrap(api.GET('/api/games/{gameId}/boxscore', { params: { path: { gameId } }, signal }))
      : skipToken,
    // only finished games are imported, their box scores do not change
    staleTime: Infinity,
    placeholderData: keepPreviousData,
  })
}

export function useDataStatus() {
  return useQuery({
    queryKey: ['data-status'],
    queryFn: ({ signal }) => unwrap(api.GET('/api/data-status', { signal })),
    refetchInterval: (query) => (query.state.data?.importRunning ? 5_000 : MINUTE),
  })
}
