import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { describe, expect, it } from 'vitest'
import { PlayerPage } from './PlayerPage'

function renderAt(url: string) {
  const router = createMemoryRouter([{ path: '/players/:playerId', element: <PlayerPage /> }], {
    initialEntries: [url],
  })
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
  return router
}

describe('PlayerPage', () => {
  it('shows how often the player beat the line in the last games', async () => {
    renderAt('/players/1628983?line=29.5&n=5')

    expect(await screen.findByRole('heading', { name: 'Shai Gilgeous-Alexander' })).toBeInTheDocument()
    // the last five: 14, 22, 35, 28, 34 points
    expect(await screen.findByText('2/5')).toBeInTheDocument()
    expect(screen.getByText('Powyżej 29.5')).toBeInTheDocument()
  })

  it('starts from the latest season average rounded to x.5', async () => {
    renderAt('/players/1628983')

    // 2026-27: 34 and 28 points
    expect(await screen.findByDisplayValue('31.5')).toBeInTheDocument()
  })

  it('moves the line in steps of 0.5 and keeps it in the URL', async () => {
    const router = renderAt('/players/1628983?line=29.5')

    await userEvent.click(await screen.findByRole('button', { name: 'Podnieś linię' }))

    expect(screen.getByDisplayValue('30.0')).toBeInTheDocument()
    expect(router.state.location.search).toBe('?line=30')
  })

  it('can stay within the latest season', async () => {
    const router = renderAt('/players/1628983?line=29.5')

    await userEvent.click(await screen.findByLabelText('Tylko sezon 2026-27'))

    expect(await screen.findByText('1/2')).toBeInTheDocument()
    expect(router.state.location.search).toBe('?line=29.5&season=latest')
  })

  it('shows the box score of the most recent game and the season averages', async () => {
    renderAt('/players/1628983')

    expect(await screen.findByRole('link', { name: 'Kawhi Leonard' })).toBeInTheDocument()
    expect(await screen.findByText('31.1')).toBeInTheDocument()
  })

  it('reports an unknown player', async () => {
    renderAt('/players/42')

    expect(await screen.findByText('Nie ma takiego zawodnika w bazie.')).toBeInTheDocument()
  })
})
