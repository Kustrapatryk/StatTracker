import { PinIcon, PinOffIcon } from 'lucide-react'
import { Link } from 'react-router'
import type { PlayerLine, StatLine, TeamBoxScore } from '@/api/client'
import { useBoxScore } from '@/api/queries'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableFooter, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { formatLongDate, formatPlusMinus, formatValue, SEASON_TYPE_LABELS } from '@/lib/format'
import { type BoxColumn, STATS, type StatKey } from '@/lib/stats'
import { cn } from '@/lib/utils'

interface Props {
  gameId: string | undefined
  playerId: number
  stat: StatKey
  pinned: boolean
  onUnpin: () => void
}

export function BoxScorePanel({ gameId, playerId, stat, pinned, onUnpin }: Props) {
  const boxScore = useBoxScore(gameId)

  if (!gameId) {
    return (
      <Card>
        <CardContent className="py-10 text-center text-sm text-muted-foreground">Brak meczu do pokazania</CardContent>
      </Card>
    )
  }
  if (boxScore.isError) {
    return (
      <Alert variant="destructive">
        <AlertDescription>Nie udało się pobrać box score: {boxScore.error.message}</AlertDescription>
      </Alert>
    )
  }
  if (!boxScore.data) {
    return <Skeleton className="h-[34rem] w-full rounded-xl" />
  }

  const { away, home, gameDate, seasonId, seasonType, neutralSite } = boxScore.data
  const highlighted = new Set<BoxColumn>(STATS[stat].columns)
  return (
    <Card size="sm" className={cn('transition-opacity', boxScore.isPlaceholderData && 'opacity-60')}>
      <CardHeader>
        <CardTitle className="flex items-baseline gap-2 text-base tabular-nums">
          <TeamScore team={away} />
          <span className="text-muted-foreground">:</span>
          <TeamScore team={home} reverse />
        </CardTitle>
        <CardDescription className="flex flex-wrap items-center gap-x-2 gap-y-1">
          {formatLongDate(gameDate)}
          <Badge variant="secondary">
            {SEASON_TYPE_LABELS[seasonType]} {seasonId}
          </Badge>
          {neutralSite && <Badge variant="outline">neutralny parkiet</Badge>}
        </CardDescription>
        <CardAction>
          {pinned ? (
            <Button variant="outline" size="sm" onClick={onUnpin}>
              <PinOffIcon /> Odepnij
            </Button>
          ) : (
            <span className="flex items-center gap-1 text-xs text-muted-foreground">
              <PinIcon className="size-3" /> kliknij słupek, by przypiąć
            </span>
          )}
        </CardAction>
      </CardHeader>
      <CardContent className="flex flex-col gap-4 px-2">
        <TeamTable team={away} playerId={playerId} stat={stat} highlighted={highlighted} />
        <TeamTable team={home} playerId={playerId} stat={stat} highlighted={highlighted} />
      </CardContent>
    </Card>
  )
}

function TeamScore({ team, reverse }: { team: TeamBoxScore; reverse?: boolean }) {
  return (
    <span className={cn('flex items-baseline gap-2', reverse && 'flex-row-reverse', !team.win && 'text-muted-foreground')}>
      <span>{team.abbreviation}</span>
      <span className="text-xl font-semibold">{team.score}</span>
    </span>
  )
}

const COLUMNS: readonly { key: BoxColumn; label: string; value: (s: StatLine) => string }[] = [
  { key: 'MIN', label: 'MIN', value: (s) => formatValue(s.minutes) },
  { key: 'PTS', label: 'PTS', value: (s) => String(s.pts) },
  { key: 'REB', label: 'REB', value: (s) => String(s.reb) },
  { key: 'AST', label: 'AST', value: (s) => String(s.ast) },
  { key: 'FG3', label: '3PT', value: (s) => `${s.fg3m}-${s.fg3a}` },
  { key: 'FG', label: 'FG', value: (s) => `${s.fgm}-${s.fga}` },
  { key: 'FT', label: 'FT', value: (s) => `${s.ftm}-${s.fta}` },
  { key: 'STL', label: 'STL', value: (s) => String(s.stl) },
  { key: 'BLK', label: 'BLK', value: (s) => String(s.blk) },
  { key: 'TOV', label: 'TOV', value: (s) => String(s.tov) },
  { key: 'PF', label: 'PF', value: (s) => String(s.pf) },
  { key: 'PM', label: '+/-', value: (s) => formatPlusMinus(s.plusMinus) },
]

interface TeamTableProps {
  team: TeamBoxScore
  playerId: number
  stat: StatKey
  highlighted: ReadonlySet<BoxColumn>
}

function TeamTable({ team, playerId, stat, highlighted }: TeamTableProps) {
  const columnClass = (key: BoxColumn) => cn('px-1.5 text-right', highlighted.has(key) && 'bg-muted font-semibold')
  return (
    <Table className="text-xs tabular-nums">
      <TableHeader>
        <TableRow>
          <TableHead className="h-8 px-2 text-foreground">{team.name}</TableHead>
          {COLUMNS.map((column) => (
            <TableHead key={column.key} className={cn('h-8', columnClass(column.key))}>
              {column.label}
            </TableHead>
          ))}
        </TableRow>
      </TableHeader>
      <TableBody>
        {team.players.map((player) => (
          <PlayerRow
            key={player.playerId}
            player={player}
            stat={stat}
            selected={player.playerId === playerId}
            columnClass={columnClass}
          />
        ))}
      </TableBody>
      <TableFooter>
        <TableRow>
          <TableCell className="px-2">Razem</TableCell>
          {COLUMNS.map((column) => (
            <TableCell key={column.key} className={columnClass(column.key)}>
              {column.value(team.totals)}
            </TableCell>
          ))}
        </TableRow>
      </TableFooter>
    </Table>
  )
}

interface PlayerRowProps {
  player: PlayerLine
  stat: StatKey
  selected: boolean
  columnClass: (key: BoxColumn) => string
}

function PlayerRow({ player, stat, selected, columnClass }: PlayerRowProps) {
  return (
    <TableRow className={cn(selected && 'bg-amber-100/70 hover:bg-amber-100 dark:bg-amber-500/20')}>
      <TableCell className="max-w-40 truncate px-2 py-1.5">
        <Link
          to={`/players/${player.playerId}?stat=${stat}`}
          className={cn('hover:underline', selected && 'font-semibold')}
          title={player.name}
        >
          {player.name}
        </Link>
        {player.position && <span className="ml-1 text-muted-foreground">{player.position}</span>}
      </TableCell>
      {COLUMNS.map((column) => (
        <TableCell key={column.key} className={cn('py-1.5', columnClass(column.key))}>
          {column.value(player.stats)}
        </TableCell>
      ))}
    </TableRow>
  )
}
