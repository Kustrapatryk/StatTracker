import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'
import { boxScore, dataStatus, games, player, seasonAverages } from './fixtures'

export const handlers = [
  http.get('*/api/players/:playerId', ({ params }) =>
    params.playerId === String(player.id)
      ? HttpResponse.json(player)
      : HttpResponse.json({ status: 404, detail: `Player ${params.playerId} not found` }, { status: 404 }),
  ),
  http.get('*/api/players/:playerId/games', () => HttpResponse.json(games)),
  http.get('*/api/players/:playerId/season-averages', () => HttpResponse.json(seasonAverages)),
  http.get('*/api/games/:gameId/boxscore', () => HttpResponse.json(boxScore)),
  http.get('*/api/data-status', () => HttpResponse.json(dataStatus)),
]

export const server = setupServer(...handlers)
