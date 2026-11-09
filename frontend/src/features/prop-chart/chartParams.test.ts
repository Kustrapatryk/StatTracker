import { describe, expect, it } from 'vitest'
import { parseChartParams, serializeChartParams } from './chartParams'

describe('chart params in the URL', () => {
  it('defaults to points over the last 20 games of every phase', () => {
    expect(parseChartParams(new URLSearchParams())).toEqual({
      stat: 'PTS',
      line: null,
      count: 20,
      latestSeasonOnly: false,
      phases: ['REGULAR', 'PLAY_IN', 'PLAYOFFS'],
    })
  })

  it('reads every setting', () => {
    const params = parseChartParams(new URLSearchParams('stat=PRA&line=42.5&n=10&season=latest&phases=PLAYOFFS,REGULAR'))

    expect(params).toEqual({
      stat: 'PRA',
      line: 42.5,
      count: 10,
      latestSeasonOnly: true,
      phases: ['REGULAR', 'PLAYOFFS'],
    })
  })

  it('ignores invalid values', () => {
    const params = parseChartParams(new URLSearchParams('stat=XYZ&line=-3&n=7&phases=PRESEASON'))

    expect(params).toEqual(parseChartParams(new URLSearchParams()))
  })

  it('writes only what differs from the defaults', () => {
    const params = parseChartParams(new URLSearchParams('stat=AST&line=6.5'))

    expect(serializeChartParams(params).toString()).toBe('stat=AST&line=6.5')
    expect(serializeChartParams({ ...params, stat: 'PTS', line: null }).toString()).toBe('')
  })
})
