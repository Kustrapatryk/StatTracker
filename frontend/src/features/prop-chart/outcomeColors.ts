import type { Outcome } from '@/lib/stats'

export const OUTCOME_COLORS: Record<Outcome, string> = {
  over: '#16a34a',
  under: '#dc2626',
  push: '#9ca3af',
}

export const OUTCOME_LABELS: Record<Outcome, string> = {
  over: 'powyżej',
  under: 'poniżej',
  push: 'równo z',
}
