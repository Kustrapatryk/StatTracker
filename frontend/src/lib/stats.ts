import type { StatLine } from '@/api/client'

/** Counting stats shared by a single game's line and per-game season averages. */
export type CountingStats = Omit<StatLine, 'plusMinus'>

/** Box score columns; a stat highlights the columns it is made of. */
export type BoxColumn = 'MIN' | 'PTS' | 'REB' | 'AST' | 'FG3' | 'FG' | 'FT' | 'STL' | 'BLK' | 'TOV' | 'PF' | 'PM'

export interface StatDefinition {
  label: string
  short: string
  value: (stats: CountingStats) => number
  columns: readonly BoxColumn[]
}

export const STATS = {
  PTS: { label: 'Punkty', short: 'PTS', value: (s) => s.pts, columns: ['PTS'] },
  REB: { label: 'Zbiórki', short: 'REB', value: (s) => s.reb, columns: ['REB'] },
  AST: { label: 'Asysty', short: 'AST', value: (s) => s.ast, columns: ['AST'] },
  FG3M: { label: 'Trafione trójki', short: '3PM', value: (s) => s.fg3m, columns: ['FG3'] },
  STL: { label: 'Przechwyty', short: 'STL', value: (s) => s.stl, columns: ['STL'] },
  BLK: { label: 'Bloki', short: 'BLK', value: (s) => s.blk, columns: ['BLK'] },
  TOV: { label: 'Straty', short: 'TOV', value: (s) => s.tov, columns: ['TOV'] },
  PRA: { label: 'Pkt + zb. + as.', short: 'PRA', value: (s) => s.pts + s.reb + s.ast, columns: ['PTS', 'REB', 'AST'] },
  PR: { label: 'Pkt + zbiórki', short: 'P+R', value: (s) => s.pts + s.reb, columns: ['PTS', 'REB'] },
  PA: { label: 'Pkt + asysty', short: 'P+A', value: (s) => s.pts + s.ast, columns: ['PTS', 'AST'] },
  RA: { label: 'Zbiórki + asysty', short: 'R+A', value: (s) => s.reb + s.ast, columns: ['REB', 'AST'] },
  SB: { label: 'Przechwyty + bloki', short: 'S+B', value: (s) => s.stl + s.blk, columns: ['STL', 'BLK'] },
  FGM: { label: 'Celne rzuty z gry', short: 'FGM', value: (s) => s.fgm, columns: ['FG'] },
  FGA: { label: 'Rzuty z gry', short: 'FGA', value: (s) => s.fga, columns: ['FG'] },
  FG3A: { label: 'Rzuty za 3', short: '3PA', value: (s) => s.fg3a, columns: ['FG3'] },
  FTM: { label: 'Celne rzuty wolne', short: 'FTM', value: (s) => s.ftm, columns: ['FT'] },
  FTA: { label: 'Rzuty wolne', short: 'FTA', value: (s) => s.fta, columns: ['FT'] },
  OREB: { label: 'Zbiórki w ataku', short: 'OREB', value: (s) => s.oreb, columns: ['REB'] },
  DREB: { label: 'Zbiórki w obronie', short: 'DREB', value: (s) => s.dreb, columns: ['REB'] },
  MIN: { label: 'Minuty', short: 'MIN', value: (s) => s.minutes, columns: ['MIN'] },
} as const satisfies Record<string, StatDefinition>

export type StatKey = keyof typeof STATS

export const STAT_GROUPS: readonly { label: string; keys: readonly StatKey[] }[] = [
  { label: 'Podstawowe', keys: ['PTS', 'REB', 'AST', 'FG3M', 'STL', 'BLK', 'TOV'] },
  { label: 'Łączone', keys: ['PRA', 'PR', 'PA', 'RA', 'SB'] },
  { label: 'Rzuty', keys: ['FGM', 'FGA', 'FG3A', 'FTM', 'FTA'] },
  { label: 'Inne', keys: ['OREB', 'DREB', 'MIN'] },
]

export const isStatKey = (value: string | null): value is StatKey => value !== null && Object.hasOwn(STATS, value)

export type Outcome = 'over' | 'under' | 'push'

export const classify = (value: number, line: number): Outcome =>
  value > line ? 'over' : value < line ? 'under' : 'push'

export interface HitSummary {
  games: number
  over: number
  push: number
  average: number | null
  median: number | null
}

export function summarize(values: readonly number[], line: number): HitSummary {
  const outcomes = values.map((value) => classify(value, line))
  return {
    games: values.length,
    over: outcomes.filter((outcome) => outcome === 'over').length,
    push: outcomes.filter((outcome) => outcome === 'push').length,
    average: mean(values),
    median: median(values),
  }
}

export function mean(values: readonly number[]): number | null {
  return values.length === 0 ? null : values.reduce((sum, value) => sum + value, 0) / values.length
}

export function median(values: readonly number[]): number | null {
  if (values.length === 0) return null
  const sorted = values.toSorted((a, b) => a - b)
  const middle = Math.floor(sorted.length / 2)
  return sorted.length % 2 === 1 ? sorted[middle] : (sorted[middle - 1] + sorted[middle]) / 2
}

/** The average rounded to the nearest x.5, so the line can never be pushed. */
export function defaultLine(values: readonly number[]): number {
  const average = mean(values)
  return average === null ? 0.5 : Math.floor(average) + 0.5
}

export const MAX_LINE = 200

/** Accepts "14.5" and "14,5"; lines move in steps of 0.5. */
export function parseLine(text: string | null): number | null {
  if (text === null || text.trim() === '') return null
  const value = Number(text.trim().replace(',', '.'))
  if (!Number.isFinite(value) || value < 0 || value > MAX_LINE) return null
  return Math.round(value * 2) / 2
}
