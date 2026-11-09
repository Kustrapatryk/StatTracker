import { useState } from 'react'
import type { AverageLine, SeasonAverage, SeasonType } from '@/api/client'
import { useSeasonAverages } from '@/api/queries'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Card, CardAction, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { formatDecimal, formatPct, SEASON_TYPE_LABELS } from '@/lib/format'
import { SEASON_TYPES } from '@/lib/games'
import { STATS, type StatKey } from '@/lib/stats'
import { cn } from '@/lib/utils'

interface Column {
  label: string
  /** The stat whose column this is, highlighted while it is selected. */
  stat?: StatKey
  value: (stats: AverageLine) => string
}

const COLUMNS: readonly Column[] = [
  { label: 'M', value: (s) => String(s.gamesPlayed) },
  { label: 'MIN', stat: 'MIN', value: (s) => formatDecimal(s.minutes) },
  { label: 'PTS', stat: 'PTS', value: (s) => formatDecimal(s.pts) },
  { label: 'REB', stat: 'REB', value: (s) => formatDecimal(s.reb) },
  { label: 'AST', stat: 'AST', value: (s) => formatDecimal(s.ast) },
  { label: '3PM', stat: 'FG3M', value: (s) => formatDecimal(s.fg3m) },
  { label: 'STL', stat: 'STL', value: (s) => formatDecimal(s.stl) },
  { label: 'BLK', stat: 'BLK', value: (s) => formatDecimal(s.blk) },
  { label: 'TOV', stat: 'TOV', value: (s) => formatDecimal(s.tov) },
  { label: 'FG%', value: (s) => formatPct(s.fgPct) },
  { label: '3P%', value: (s) => formatPct(s.fg3Pct) },
  { label: 'FT%', value: (s) => formatPct(s.ftPct) },
]

export function SeasonAverages({ playerId, stat }: { playerId: number; stat: StatKey }) {
  const averages = useSeasonAverages(playerId)
  const [selectedType, setSelectedType] = useState<SeasonType>('REGULAR')

  const available = SEASON_TYPES.filter((type) => averages.data?.some((average) => average.seasonType === type))
  const type = available.includes(selectedType) ? selectedType : (available[0] ?? 'REGULAR')
  const rows = averages.data?.filter((average) => average.seasonType === type) ?? []
  // combined and less common stats get their own column next to the standard ones
  const columns = COLUMNS.some((column) => column.stat === stat)
    ? COLUMNS
    : [...COLUMNS, { label: STATS[stat].short, stat, value: (s: AverageLine) => formatDecimal(STATS[stat].value(s)) }]

  return (
    <Card>
      <CardHeader>
        <CardTitle>Średnie sezonowe</CardTitle>
        {available.length > 1 && (
          <CardAction>
            <Tabs value={type} onValueChange={(value) => setSelectedType(value as SeasonType)}>
              <TabsList>
                {available.map((option) => (
                  <TabsTrigger key={option} value={option}>
                    {SEASON_TYPE_LABELS[option]}
                  </TabsTrigger>
                ))}
              </TabsList>
            </Tabs>
          </CardAction>
        )}
      </CardHeader>
      <CardContent>
        {averages.isPending && <Skeleton className="h-40 w-full" />}
        {averages.isError && (
          <Alert variant="destructive">
            <AlertDescription>Nie udało się pobrać średnich: {averages.error.message}</AlertDescription>
          </Alert>
        )}
        {averages.isSuccess && rows.length === 0 && (
          <p className="py-6 text-center text-sm text-muted-foreground">Brak rozegranych meczów w bazie</p>
        )}
        {rows.length > 0 && (
          <Table className="tabular-nums">
            <TableHeader>
              <TableRow>
                <TableHead>Sezon</TableHead>
                <TableHead>Drużyna</TableHead>
                {columns.map((column) => (
                  <TableHead key={column.label} className={cn('text-right', column.stat === stat && 'bg-muted text-foreground')}>
                    {column.label}
                  </TableHead>
                ))}
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((average) => (
                <SeasonRows key={average.seasonId} average={average} columns={columns} stat={stat} />
              ))}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  )
}

function SeasonRows({ average, columns, stat }: { average: SeasonAverage; columns: readonly Column[]; stat: StatKey }) {
  const cellClass = (column: Column) => cn('text-right', column.stat === stat && 'bg-muted font-semibold')
  return (
    <>
      <TableRow>
        <TableCell className="font-medium">{average.seasonId}</TableCell>
        <TableCell>{average.teams.join(' / ')}</TableCell>
        {columns.map((column) => (
          <TableCell key={column.label} className={cellClass(column)}>
            {column.value(average.stats)}
          </TableCell>
        ))}
      </TableRow>
      {average.byTeam.map((team) => (
        <TableRow key={team.teamId} className="text-muted-foreground">
          <TableCell />
          <TableCell className="pl-5">{team.teamAbbreviation}</TableCell>
          {columns.map((column) => (
            <TableCell key={column.label} className={cellClass(column)}>
              {column.value(team.stats)}
            </TableCell>
          ))}
        </TableRow>
      ))}
    </>
  )
}
