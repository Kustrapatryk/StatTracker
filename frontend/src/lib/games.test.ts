import { describe, expect, it } from 'vitest'
import { games } from '@/test/fixtures'
import { SEASON_TYPES, selectGames } from './games'

const ids = (selected: { gameId: string }[]) => selected.map((game) => game.gameId)

describe('selectGames', () => {
  it('takes the most recent games across seasons and orders them oldest first', () => {
    const selected = selectGames(games, { count: 3, phases: SEASON_TYPES, latestSeasonOnly: false })

    expect(ids(selected)).toEqual(['0042500317', '0022600101', '0022600120'])
  })

  it('leaves out excluded phases', () => {
    const selected = selectGames(games, { count: 5, phases: ['REGULAR'], latestSeasonOnly: false })

    expect(ids(selected)).toEqual(['0022501230', '0022500933', '0022600101', '0022600120'])
  })

  it('can stay within the season of the most recent game', () => {
    const selected = selectGames(games, { count: 30, phases: SEASON_TYPES, latestSeasonOnly: true })

    expect(ids(selected)).toEqual(['0022600101', '0022600120'])
  })

  it('returns nothing for a player without games', () => {
    expect(selectGames([], { count: 20, phases: SEASON_TYPES, latestSeasonOnly: true })).toEqual([])
  })
})
