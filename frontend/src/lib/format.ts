import type { PlayerGame, SeasonType } from '@/api/client'

export const SEASON_TYPE_LABELS: Record<SeasonType, string> = {
  REGULAR: 'Sezon zasadniczy',
  PLAY_IN: 'Play-in',
  PLAYOFFS: 'Playoffy',
}

/** Marker shown on the chart next to postseason games. */
export const SEASON_TYPE_MARKERS: Record<SeasonType, string | null> = {
  REGULAR: null,
  PLAY_IN: 'PI',
  PLAYOFFS: 'PO',
}

/** 2026-11-04 → 11/04, month first as on NBA.com. */
export const formatShortDate = (isoDate: string) => `${isoDate.slice(5, 7)}/${isoDate.slice(8, 10)}`

const longDate = new Intl.DateTimeFormat('pl-PL', { day: 'numeric', month: 'long', year: 'numeric' })
const dateTime = new Intl.DateTimeFormat('pl-PL', { dateStyle: 'medium', timeStyle: 'short' })

export function formatLongDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-').map(Number)
  return longDate.format(new Date(year, month - 1, day))
}

export const formatDateTime = (isoInstant: string) => dateTime.format(new Date(isoInstant))

/** "vs HOU" at home, "@LAC" away; NBA lists both teams at a neutral site as visitors, so neither gets "@" there. */
export function opponentLabel(game: Pick<PlayerGame, 'home' | 'neutralSite' | 'opponentAbbreviation'>): string {
  return game.home || game.neutralSite ? `vs ${game.opponentAbbreviation}` : `@${game.opponentAbbreviation}`
}

export const formatDecimal = (value: number | null) => (value === null ? '–' : value.toFixed(1))

export const formatLine = (line: number) => line.toFixed(1)

/** Whole numbers stay whole: 31, but 31.5. */
export const formatValue = (value: number) => (Number.isInteger(value) ? String(value) : value.toFixed(1))

export const formatPct = (value: number | null) => (value === null ? '–' : `${(value * 100).toFixed(1)}%`)

export const formatShare = (part: number, total: number) => (total === 0 ? '–' : `${Math.round((part / total) * 100)}%`)

export function formatPlusMinus(value: number | null): string {
  if (value === null) return '–'
  return value > 0 ? `+${value}` : String(value)
}

/** NBA.com gives "6-6" (feet-inches). */
export function formatHeight(height: string | null): string | null {
  const match = height?.match(/^(\d+)-(\d+)$/)
  if (!match) return height
  return `${Math.round((Number(match[1]) * 12 + Number(match[2])) * 2.54)} cm`
}

export const formatWeight = (pounds: number | null) => (pounds === null ? null : `${Math.round(pounds * 0.4536)} kg`)
