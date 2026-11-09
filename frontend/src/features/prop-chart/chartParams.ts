import { useCallback, useMemo } from 'react'
import { useSearchParams } from 'react-router'
import type { SeasonType } from '@/api/client'
import { GAME_COUNTS, type GameCount, SEASON_TYPES } from '@/lib/games'
import { isStatKey, parseLine, type StatKey } from '@/lib/stats'

/** Chart settings, kept in the URL so the view survives a reload and can be shared. */
export interface ChartParams {
  stat: StatKey
  /** `null` until the user sets one; the page then derives it from the season average. */
  line: number | null
  count: GameCount
  latestSeasonOnly: boolean
  phases: SeasonType[]
}

const DEFAULTS: ChartParams = {
  stat: 'PTS',
  line: null,
  count: 20,
  latestSeasonOnly: false,
  phases: [...SEASON_TYPES],
}

export function parseChartParams(search: URLSearchParams): ChartParams {
  const stat = search.get('stat')
  const count = Number(search.get('n'))
  const phases = (search.get('phases') ?? '')
    .split(',')
    .filter((phase): phase is SeasonType => (SEASON_TYPES as readonly string[]).includes(phase))
  return {
    stat: isStatKey(stat) ? stat : DEFAULTS.stat,
    line: parseLine(search.get('line')),
    count: GAME_COUNTS.find((allowed) => allowed === count) ?? DEFAULTS.count,
    latestSeasonOnly: search.get('season') === 'latest',
    phases: phases.length > 0 ? SEASON_TYPES.filter((phase) => phases.includes(phase)) : DEFAULTS.phases,
  }
}

export function serializeChartParams(params: ChartParams): URLSearchParams {
  const search = new URLSearchParams()
  if (params.stat !== DEFAULTS.stat) search.set('stat', params.stat)
  if (params.line !== null) search.set('line', String(params.line))
  if (params.count !== DEFAULTS.count) search.set('n', String(params.count))
  if (params.latestSeasonOnly) search.set('season', 'latest')
  if (params.phases.length < SEASON_TYPES.length) search.set('phases', params.phases.join(','))
  return search
}

export function useChartParams(): [ChartParams, (changes: Partial<ChartParams>) => void] {
  const [search, setSearch] = useSearchParams()
  const params = useMemo(() => parseChartParams(search), [search])
  const update = useCallback(
    (changes: Partial<ChartParams>) =>
      setSearch(
        (current) => {
          const next = { ...parseChartParams(current), ...changes }
          // a points line means nothing for assists
          if (changes.stat !== undefined && changes.line === undefined) next.line = null
          return serializeChartParams(next)
        },
        { replace: true },
      ),
    [setSearch],
  )
  return [params, update]
}
