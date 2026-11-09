import type { PlayerGame, SeasonType } from '@/api/client'

export const GAME_COUNTS = [5, 10, 15, 20, 30] as const
export type GameCount = (typeof GAME_COUNTS)[number]

export const SEASON_TYPES = ['REGULAR', 'PLAY_IN', 'PLAYOFFS'] as const satisfies readonly SeasonType[]

export interface GameFilter {
  count: number
  phases: readonly SeasonType[]
  /** Only the season of the player's most recent game. */
  latestSeasonOnly: boolean
}

/**
 * @param games most recent first, as the API returns them
 * @returns the last `count` matching games, oldest first as on the chart
 */
export function selectGames(games: readonly PlayerGame[], filter: GameFilter): PlayerGame[] {
  const latestSeason = games[0]?.seasonId
  return games
    .filter((game) => filter.phases.includes(game.seasonType))
    .filter((game) => !filter.latestSeasonOnly || game.seasonId === latestSeason)
    .slice(0, filter.count)
    .reverse()
}
