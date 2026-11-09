import {
  Bar,
  BarChart,
  type BarShapeProps,
  CartesianGrid,
  LabelList,
  Rectangle,
  ReferenceLine,
  ResponsiveContainer,
  Tooltip,
  type TooltipPayloadEntry,
  XAxis,
  type XAxisTickContentProps,
  YAxis,
} from 'recharts'
import type { PlayerGame } from '@/api/client'
import { formatLine, formatPlusMinus, formatShortDate, formatValue, opponentLabel, SEASON_TYPE_LABELS, SEASON_TYPE_MARKERS } from '@/lib/format'
import { classify, type Outcome, STATS, type StatKey } from '@/lib/stats'
import { OUTCOME_COLORS, OUTCOME_LABELS } from './outcomeColors'

interface Props {
  /** Oldest first. */
  games: readonly PlayerGame[]
  stat: StatKey
  line: number
  activeGameId: string | undefined
  onHover: (gameId: string | undefined) => void
  onSelect: (gameId: string) => void
}

interface Datum {
  gameId: string
  value: number
  outcome: Outcome
  game: PlayerGame
}

const HEIGHT = 340
/** Above this many games the axis labels are rotated to fit. */
const COMPACT_FROM = 16

export function PropChart({ games, stat, line, activeGameId, onHover, onSelect }: Props) {
  const data: Datum[] = games.map((game) => {
    const value = STATS[stat].value(game.stats)
    return { gameId: game.gameId, value, outcome: classify(value, line), game }
  })
  const byId = new Map(data.map((datum) => [datum.gameId, datum]))
  const seasonStarts = data.filter((datum, index) => index > 0 && datum.game.seasonId !== data[index - 1].game.seasonId)
  const compact = data.length >= COMPACT_FROM
  const gameAt = (index: unknown) => (index === undefined || index === null ? undefined : data[Number(index)]?.gameId)

  return (
    <ResponsiveContainer width="100%" height={HEIGHT}>
      <BarChart
        data={data}
        margin={{ top: 24, right: 44, bottom: 4, left: 0 }}
        barCategoryGap="16%"
        onMouseMove={(state) => onHover(gameAt(state.activeTooltipIndex))}
        onMouseLeave={() => onHover(undefined)}
        onClick={(state) => {
          const gameId = gameAt(state.activeTooltipIndex)
          if (gameId) onSelect(gameId)
        }}
        className="cursor-pointer select-none"
      >
        <CartesianGrid vertical={false} strokeDasharray="3 3" stroke="var(--color-border)" />
        <XAxis
          dataKey="gameId"
          interval={0}
          tickLine={false}
          height={compact ? 70 : 52}
          tick={(props: XAxisTickContentProps) => (
            <GameTick
              x={Number(props.x)}
              y={Number(props.y)}
              datum={byId.get(String(props.payload.value))}
              compact={compact}
            />
          )}
        />
        <YAxis allowDecimals={false} width={36} tickLine={false} axisLine={false} fontSize={12} />
        <Tooltip
          cursor={{ fill: 'var(--color-muted)', opacity: 0.7 }}
          isAnimationActive={false}
          content={({ active, payload }) => <GameTooltip active={active} payload={payload} stat={stat} line={line} />}
        />
        {seasonStarts.map((datum) => (
          <ReferenceLine
            key={datum.gameId}
            x={datum.gameId}
            position="start"
            stroke="var(--color-muted-foreground)"
            strokeDasharray="4 4"
            label={{ value: datum.game.seasonId, position: 'insideTopLeft', fontSize: 11, fill: 'var(--color-muted-foreground)' }}
          />
        ))}
        <ReferenceLine
          y={line}
          stroke="var(--color-foreground)"
          strokeDasharray="6 4"
          strokeWidth={1.5}
          ifOverflow="extendDomain"
          label={{ value: formatLine(line), position: 'right', fontSize: 12, fontWeight: 600, fill: 'var(--color-foreground)' }}
        />
        <Bar
          dataKey="value"
          minPointSize={3}
          isAnimationActive={false}
          shape={(props: BarShapeProps) => {
            const datum = props.payload as Datum
            const active = datum.gameId === activeGameId
            return (
              <Rectangle
                {...props}
                fill={OUTCOME_COLORS[datum.outcome]}
                radius={[3, 3, 0, 0]}
                stroke={active ? 'var(--color-foreground)' : 'none'}
                strokeWidth={active ? 2 : 0}
              />
            )
          }}
        >
          <LabelList dataKey="value" position="top" fontSize={11} formatter={(value) => formatValue(Number(value))} />
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  )
}

function GameTick({ x, y, datum, compact }: { x: number; y: number; datum: Datum | undefined; compact: boolean }) {
  if (!datum) return null
  const { game } = datum
  const marker = SEASON_TYPE_MARKERS[game.seasonType]
  if (compact) {
    return (
      <g transform={`translate(${x},${y + 6})`}>
        <text transform="rotate(-50)" textAnchor="end" fontSize={10} fill="var(--color-muted-foreground)">
          {marker && (
            <tspan fontWeight={700} fill="#b45309">
              {marker}{' '}
            </tspan>
          )}
          {opponentLabel(game)} {formatShortDate(game.gameDate)}
        </text>
      </g>
    )
  }
  return (
    <g transform={`translate(${x},${y})`}>
      <text textAnchor="middle" fontSize={11} fill="var(--color-foreground)">
        <tspan x={0} dy={12}>
          {opponentLabel(game)}
        </tspan>
        <tspan x={0} dy={13} fill="var(--color-muted-foreground)">
          {formatShortDate(game.gameDate)}
        </tspan>
        {marker && (
          <tspan x={0} dy={13} fontWeight={700} fill="#b45309">
            {marker}
          </tspan>
        )}
      </text>
    </g>
  )
}

interface GameTooltipProps {
  active: boolean | undefined
  payload: readonly TooltipPayloadEntry[] | undefined
  stat: StatKey
  line: number
}

function GameTooltip({ active, payload, stat, line }: GameTooltipProps) {
  const datum = payload?.[0]?.payload as Datum | undefined
  if (!active || !datum) return null
  const { game, value, outcome } = datum
  const s = game.stats
  return (
    <div className="min-w-56 rounded-lg border bg-popover px-3 py-2 text-xs text-popover-foreground shadow-lg">
      <p className="font-medium">
        {formatShortDate(game.gameDate)} {opponentLabel(game)} · {game.win ? 'W' : 'L'} {game.teamScore}–{game.opponentScore}
      </p>
      <p className="text-muted-foreground">
        {SEASON_TYPE_LABELS[game.seasonType]} {game.seasonId}
        {game.neutralSite && ' · neutralny parkiet'}
      </p>
      <p className="mt-1.5 text-sm font-semibold" style={{ color: OUTCOME_COLORS[outcome] }}>
        {STATS[stat].short} {formatValue(value)} · {OUTCOME_LABELS[outcome]} {formatLine(line)}
      </p>
      <dl className="mt-1.5 grid grid-cols-[auto_1fr] gap-x-3 gap-y-0.5 tabular-nums">
        <dt className="text-muted-foreground">Linijka</dt>
        <dd>
          {formatValue(s.minutes)} MIN · {s.pts} PTS · {s.reb} REB · {s.ast} AST
        </dd>
        <dt className="text-muted-foreground">Rzuty</dt>
        <dd>
          FG {s.fgm}/{s.fga} · 3P {s.fg3m}/{s.fg3a} · FT {s.ftm}/{s.fta}
        </dd>
        <dt className="text-muted-foreground">Inne</dt>
        <dd>
          {s.stl} STL · {s.blk} BLK · {s.tov} TOV · {formatPlusMinus(s.plusMinus)}
        </dd>
      </dl>
    </div>
  )
}
