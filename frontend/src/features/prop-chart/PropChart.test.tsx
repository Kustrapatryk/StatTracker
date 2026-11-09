import { render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { SEASON_TYPES, selectGames } from '@/lib/games'
import { games } from '@/test/fixtures'
import { OUTCOME_COLORS } from './outcomeColors'
import { PropChart } from './PropChart'

const { over, under, push } = OUTCOME_COLORS
// points, oldest first: 30, 14, 22, 35, 28, 34
const chartGames = selectGames(games, { count: 6, phases: SEASON_TYPES, latestSeasonOnly: false })

function renderChart(line: number) {
  return render(
    <PropChart games={chartGames} stat="PTS" line={line} activeGameId={undefined} onHover={vi.fn()} onSelect={vi.fn()} />,
  )
}

const barColors = (container: HTMLElement) =>
  [...container.querySelectorAll('.recharts-bar-rectangle path')].map((bar) => bar.getAttribute('fill'))

describe('PropChart', () => {
  // the chart measures its container, which jsdom reports as 0×0
  beforeEach(() => {
    vi.spyOn(HTMLElement.prototype, 'getBoundingClientRect').mockReturnValue(new DOMRect(0, 0, 800, 340))
  })
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('colors bars above the line green and below it red', () => {
    const { container } = renderChart(28.5)

    expect(barColors(container)).toEqual([over, under, under, over, under, over])
  })

  it('colors a value equal to a whole line grey', () => {
    const { container } = renderChart(28)

    expect(barColors(container)).toEqual([over, under, under, over, push, over])
  })

  it('draws the line, playoff markers and opponents', () => {
    renderChart(28.5)

    expect(screen.getByText('28.5')).toBeInTheDocument()
    expect(screen.getAllByText('PO')).toHaveLength(2)
    expect(screen.getByText('@LAC')).toBeInTheDocument()
    expect(screen.getByText('vs HOU')).toBeInTheDocument()
  })

  it('separates seasons between the last game of 2025-26 and the first of 2026-27', () => {
    const { container } = renderChart(28.5)

    const separator = [...container.querySelectorAll('.recharts-reference-line line')].find(
      (line) => line.getAttribute('x1') === line.getAttribute('x2'),
    )
    const separatorX = Number(separator?.getAttribute('x1'))
    const barStarts = [...container.querySelectorAll('.recharts-bar-rectangle path')].map((bar) =>
      Number(/^M\s*([\d.]+)/.exec(bar.getAttribute('d') ?? '')?.[1]),
    )
    expect(screen.getByText('2026-27')).toBeInTheDocument()
    expect(separatorX).toBeGreaterThan(barStarts[3])
    expect(separatorX).toBeLessThan(barStarts[4])
  })
})
