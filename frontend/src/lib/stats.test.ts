import { describe, expect, it } from 'vitest'
import { statLine } from '@/test/fixtures'
import { classify, defaultLine, median, parseLine, STAT_GROUPS, STATS, summarize } from './stats'

describe('classify', () => {
  it('compares a value with the line', () => {
    expect(classify(15, 14.5)).toBe('over')
    expect(classify(14, 14.5)).toBe('under')
    expect(classify(14, 14)).toBe('push')
  })
})

describe('STATS', () => {
  const line = statLine({ minutes: 36, pts: 31, reb: 5, ast: 7, stl: 2, blk: 1, fg3m: 3, fg3a: 8, oreb: 1, dreb: 4 })

  it('reads single stats from the game line', () => {
    expect(STATS.PTS.value(line)).toBe(31)
    expect(STATS.FG3M.value(line)).toBe(3)
    expect(STATS.MIN.value(line)).toBe(36)
  })

  it('adds up combined stats on the fly', () => {
    expect(STATS.PRA.value(line)).toBe(43)
    expect(STATS.PR.value(line)).toBe(36)
    expect(STATS.PA.value(line)).toBe(38)
    expect(STATS.RA.value(line)).toBe(12)
    expect(STATS.SB.value(line)).toBe(3)
  })

  it('offers every stat in exactly one picker group', () => {
    const grouped = STAT_GROUPS.flatMap((group) => group.keys)
    expect(grouped.toSorted()).toEqual(Object.keys(STATS).toSorted())
  })
})

describe('summarize', () => {
  it('counts games over the line, pushes, average and median', () => {
    expect(summarize([10, 20, 15, 30], 15)).toEqual({ games: 4, over: 2, push: 1, average: 18.75, median: 17.5 })
  })

  it('handles no games', () => {
    expect(summarize([], 14.5)).toEqual({ games: 0, over: 0, push: 0, average: null, median: null })
  })
})

describe('median', () => {
  it('takes the middle value of an odd count', () => {
    expect(median([7, 1, 3])).toBe(3)
  })
})

describe('defaultLine', () => {
  it('rounds the average to the nearest x.5', () => {
    expect(defaultLine([31, 31, 31.3])).toBe(31.5)
    expect(defaultLine([6, 7])).toBe(6.5)
    expect(defaultLine([4, 4])).toBe(4.5)
    expect(defaultLine([0, 0])).toBe(0.5)
  })

  it('falls back to 0.5 without games', () => {
    expect(defaultLine([])).toBe(0.5)
  })
})

describe('parseLine', () => {
  it('accepts a dot or a comma and snaps to steps of 0.5', () => {
    expect(parseLine('14.5')).toBe(14.5)
    expect(parseLine('14,5')).toBe(14.5)
    expect(parseLine(' 22 ')).toBe(22)
    expect(parseLine('14.3')).toBe(14.5)
  })

  it('rejects anything else', () => {
    for (const text of [null, '', 'abc', '-1', '1e9', 'Infinity']) {
      expect(parseLine(text)).toBeNull()
    }
  })
})
