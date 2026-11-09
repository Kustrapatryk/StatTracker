import type { AverageLine, BoxScore, DataStatus, PlayerGame, PlayerProfile, SeasonAverage, StatLine } from '@/api/client'

export function statLine(stats: Partial<StatLine> = {}): StatLine {
  return {
    minutes: 34,
    pts: 0,
    reb: 0,
    ast: 0,
    fgm: 0,
    fga: 0,
    fg3m: 0,
    fg3a: 0,
    ftm: 0,
    fta: 0,
    oreb: 0,
    dreb: 0,
    stl: 0,
    blk: 0,
    tov: 0,
    pf: 0,
    plusMinus: 0,
    ...stats,
  }
}

export const player: PlayerProfile = {
  id: 1628983,
  fullName: 'Shai Gilgeous-Alexander',
  firstName: 'Shai',
  lastName: 'Gilgeous-Alexander',
  team: { id: 1610612760, abbreviation: 'OKC', city: 'Oklahoma City', name: 'Thunder' },
  position: 'G',
  jerseyNumber: '2',
  height: '6-6',
  weightLbs: 195,
  country: 'Canada',
  draftYear: 2018,
  active: true,
  fromYear: 2018,
  toYear: 2026,
}

function game(
  gameId: string,
  gameDate: string,
  opponentAbbreviation: string,
  stats: Partial<StatLine>,
  extra: Partial<PlayerGame> = {},
): PlayerGame {
  return {
    gameId,
    gameDate,
    seasonId: '2026-27',
    seasonType: 'REGULAR',
    teamId: 1610612760,
    teamAbbreviation: 'OKC',
    opponentTeamId: 1610612743,
    opponentAbbreviation,
    home: true,
    neutralSite: false,
    win: true,
    teamScore: 120,
    opponentScore: 110,
    stats: statLine(stats),
    ...extra,
  }
}

/** Most recent first, as the API returns them: two games of 2026-27, then the end of 2025-26. */
export const games: PlayerGame[] = [
  game('0022600120', '2026-11-04', 'LAC', { pts: 34, reb: 5, ast: 7, fg3m: 2 }, { home: false }),
  game('0022600101', '2026-11-02', 'HOU', { pts: 28, reb: 4, ast: 9, fg3m: 1 }),
  game('0042500317', '2026-06-01', 'SAS', { pts: 35, reb: 6, ast: 5, fg3m: 3 }, { seasonId: '2025-26', seasonType: 'PLAYOFFS' }),
  game('0042500316', '2026-05-30', 'SAS', { pts: 22, reb: 3, ast: 8, fg3m: 0 }, { seasonId: '2025-26', seasonType: 'PLAYOFFS', win: false }),
  game('0022500933', '2026-03-09', 'DEN', { pts: 14, reb: 7, ast: 4, fg3m: 0 }, { seasonId: '2025-26' }),
  game('0022501230', '2025-12-13', 'SAS', { pts: 30, reb: 2, ast: 6, fg3m: 1 }, { seasonId: '2025-26', home: false, neutralSite: true, win: false }),
]

function averageLine(stats: Partial<AverageLine>): AverageLine {
  return { ...statLine(), gamesPlayed: 0, plusMinus: 0, fgPct: 0.5, fg3Pct: 0.35, ftPct: 0.9, ...stats }
}

export const seasonAverages: SeasonAverage[] = [
  { seasonId: '2026-27', seasonType: 'REGULAR', teams: ['OKC'], stats: averageLine({ gamesPlayed: 2, pts: 31, reb: 4.5, ast: 8 }), byTeam: [] },
  { seasonId: '2025-26', seasonType: 'REGULAR', teams: ['OKC'], stats: averageLine({ gamesPlayed: 68, pts: 31.1, reb: 4.3, ast: 6.6 }), byTeam: [] },
  { seasonId: '2025-26', seasonType: 'PLAYOFFS', teams: ['OKC'], stats: averageLine({ gamesPlayed: 15, pts: 29.8, reb: 5.1, ast: 7 }), byTeam: [] },
]

export const boxScore: BoxScore = {
  gameId: '0022600120',
  gameDate: '2026-11-04',
  seasonId: '2026-27',
  seasonType: 'REGULAR',
  neutralSite: false,
  home: {
    teamId: 1610612746,
    abbreviation: 'LAC',
    name: 'LA Clippers',
    score: 110,
    win: false,
    totals: statLine({ minutes: 240, pts: 110 }),
    players: [{ playerId: 202695, name: 'Kawhi Leonard', position: 'F', starter: null, stats: statLine({ pts: 27 }) }],
  },
  away: {
    teamId: 1610612760,
    abbreviation: 'OKC',
    name: 'Oklahoma City Thunder',
    score: 120,
    win: true,
    totals: statLine({ minutes: 240, pts: 120 }),
    players: [{ playerId: 1628983, name: 'Shai Gilgeous-Alexander', position: 'G', starter: null, stats: statLine({ pts: 34, reb: 5, ast: 7 }) }],
  },
}

export const dataStatus: DataStatus = {
  latestSeason: '2026-27',
  lastGameDate: '2026-11-05',
  games: 1300,
  importRunning: false,
  lastSuccessAt: '2026-11-06T08:00:00Z',
  lastRun: null,
}
