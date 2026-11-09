import { useState } from 'react'
import { useParams } from 'react-router'
import { ApiError, type PlayerGame } from '@/api/client'
import { usePlayer, usePlayerGames } from '@/api/queries'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { BoxScorePanel } from '@/features/box-score/BoxScorePanel'
import { ImportNotice } from '@/features/data-status/ImportNotice'
import { PlayerHeader } from '@/features/player/PlayerHeader'
import { useChartParams } from '@/features/prop-chart/chartParams'
import { HitSummary } from '@/features/prop-chart/HitSummary'
import { PropChart } from '@/features/prop-chart/PropChart'
import { PropControls } from '@/features/prop-chart/PropControls'
import { SeasonAverages } from '@/features/season-averages/SeasonAverages'
import { selectGames } from '@/lib/games'
import { defaultLine, STATS } from '@/lib/stats'
import { useDebouncedValue } from '@/lib/useDebouncedValue'
import { NotFoundPage } from './NotFoundPage'

/** Enough for the last 30 games across two seasons; postseason-only views need to look further back. */
const GAMES_LIMIT = 100
const POSTSEASON_GAMES_LIMIT = 500

export function PlayerPage() {
  const playerId = Number(useParams().playerId)
  if (!Number.isSafeInteger(playerId) || playerId <= 0) return <NotFoundPage />
  return <PlayerView key={playerId} playerId={playerId} />
}

function PlayerView({ playerId }: { playerId: number }) {
  const player = usePlayer(playerId)
  const [params, updateParams] = useChartParams()
  const games = usePlayerGames(playerId, params.phases.includes('REGULAR') ? GAMES_LIMIT : POSTSEASON_GAMES_LIMIT)
  const [hoveredGameId, setHoveredGameId] = useState<string>()
  const [pinnedGameId, setPinnedGameId] = useState<string>()
  // sweeping the mouse across the chart should not request every box score on the way
  const previewGameId = useDebouncedValue(hoveredGameId, 100)

  if (player.isError) {
    if (player.error instanceof ApiError && player.error.status === 404) {
      return <NotFoundPage message="Nie ma takiego zawodnika w bazie." />
    }
    return <LoadError message={player.error.message} />
  }

  const allGames = games.data ?? []
  const latestSeason = allGames[0]?.seasonId
  const stat = STATS[params.stat]
  const line = params.line ?? defaultLine(seasonAverageGames(allGames, latestSeason).map((game) => stat.value(game.stats)))
  const chartGames = selectGames(allGames, {
    count: params.count,
    phases: params.phases,
    latestSeasonOnly: params.latestSeasonOnly,
  })
  const boxScoreGameId = pinnedGameId ?? previewGameId ?? chartGames.at(-1)?.gameId

  return (
    <div className="flex flex-col gap-6">
      {player.data ? <PlayerHeader player={player.data} /> : <Skeleton className="h-16 w-96" />}
      <ImportNotice />

      <div className="grid gap-6 2xl:grid-cols-[minmax(0,1fr)_minmax(0,40rem)]">
        <Card className="min-w-0">
          <CardHeader>
            <PropControls params={params} line={line} latestSeason={latestSeason} onChange={updateParams} />
          </CardHeader>
          <CardContent className="flex flex-col gap-5">
            {games.isPending && <Skeleton className="h-[340px] w-full" />}
            {games.isError && <LoadError message={games.error.message} />}
            {games.isSuccess && chartGames.length === 0 && (
              <p className="flex h-[340px] items-center justify-center text-sm text-muted-foreground">
                Brak meczów w wybranym zakresie
              </p>
            )}
            {chartGames.length > 0 && (
              <>
                <PropChart
                  games={chartGames}
                  stat={params.stat}
                  line={line}
                  activeGameId={boxScoreGameId}
                  onHover={setHoveredGameId}
                  onSelect={(gameId) => setPinnedGameId((pinned) => (pinned === gameId ? undefined : gameId))}
                />
                <HitSummary values={chartGames.map((game) => stat.value(game.stats))} line={line} />
              </>
            )}
          </CardContent>
        </Card>

        <BoxScorePanel
          gameId={boxScoreGameId}
          playerId={playerId}
          stat={params.stat}
          pinned={pinnedGameId !== undefined}
          onUnpin={() => setPinnedGameId(undefined)}
        />
      </div>

      <SeasonAverages playerId={playerId} stat={params.stat} />
    </div>
  )
}

/** The games behind "the season average": the regular season, unless the season has none yet. */
function seasonAverageGames(games: readonly PlayerGame[], season: string | undefined): PlayerGame[] {
  const seasonGames = games.filter((game) => game.seasonId === season)
  const regularSeason = seasonGames.filter((game) => game.seasonType === 'REGULAR')
  return regularSeason.length > 0 ? regularSeason : seasonGames
}

function LoadError({ message }: { message: string }) {
  return (
    <Alert variant="destructive">
      <AlertTitle>Nie udało się pobrać danych</AlertTitle>
      <AlertDescription>{message}</AlertDescription>
    </Alert>
  )
}
