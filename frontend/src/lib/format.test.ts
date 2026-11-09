import { describe, expect, it } from 'vitest'
import { formatHeight, formatLongDate, formatPlusMinus, formatShortDate, formatValue, formatWeight, opponentLabel } from './format'

describe('opponentLabel', () => {
  it('marks away games with @', () => {
    expect(opponentLabel({ home: true, neutralSite: false, opponentAbbreviation: 'HOU' })).toBe('vs HOU')
    expect(opponentLabel({ home: false, neutralSite: false, opponentAbbreviation: 'LAC' })).toBe('@LAC')
  })

  it('does not mark a neutral-site game as away', () => {
    expect(opponentLabel({ home: false, neutralSite: true, opponentAbbreviation: 'SAS' })).toBe('vs SAS')
  })
})

describe('dates', () => {
  it('formats the axis label month first', () => {
    expect(formatShortDate('2026-11-04')).toBe('11/04')
  })

  it('formats the long date in Polish without shifting the day', () => {
    expect(formatLongDate('2026-03-09')).toBe('9 marca 2026')
  })
})

describe('numbers', () => {
  it('keeps whole numbers whole', () => {
    expect(formatValue(31)).toBe('31')
    expect(formatValue(42.5)).toBe('42.5')
  })

  it('signs plus-minus', () => {
    expect(formatPlusMinus(12)).toBe('+12')
    expect(formatPlusMinus(-3)).toBe('-3')
    expect(formatPlusMinus(0)).toBe('0')
    expect(formatPlusMinus(null)).toBe('–')
  })

  it('converts NBA.com height and weight to metric', () => {
    expect(formatHeight('6-6')).toBe('198 cm')
    expect(formatHeight(null)).toBeNull()
    expect(formatWeight(195)).toBe('88 kg')
  })
})
