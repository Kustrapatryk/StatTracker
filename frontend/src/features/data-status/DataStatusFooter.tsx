import type { DataStatus } from '@/api/client'
import { useDataStatus } from '@/api/queries'
import { formatDateTime, formatLongDate } from '@/lib/format'

export function DataStatusFooter() {
  const status = useDataStatus()
  return (
    <footer className="border-t">
      <div className="mx-auto flex max-w-[1600px] flex-wrap items-center justify-between gap-2 px-4 py-4 text-xs text-muted-foreground">
        <span>
          Dane:{' '}
          <a href="https://www.nba.com/stats" target="_blank" rel="noreferrer" className="underline-offset-2 hover:underline">
            NBA.com
          </a>
        </span>
        <span>{status.isError ? 'Brak połączenia z serwerem' : status.data && describe(status.data)}</span>
      </div>
    </footer>
  )
}

function describe(status: DataStatus): string {
  const parts: string[] = []
  if (status.importRunning) {
    parts.push(`Trwa import z NBA.com${status.lastRun?.progress ? ` (${status.lastRun.progress})` : ''}`)
  }
  if (status.lastGameDate) parts.push(`ostatni mecz w bazie: ${formatLongDate(status.lastGameDate)}`)
  if (status.lastSuccessAt) parts.push(`aktualizacja: ${formatDateTime(status.lastSuccessAt)}`)
  if (!status.importRunning && status.lastRun?.status === 'FAILED') parts.push('ostatni import nieudany')
  const text = parts.join(' · ')
  return text.charAt(0).toUpperCase() + text.slice(1)
}
