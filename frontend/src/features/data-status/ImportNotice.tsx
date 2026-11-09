import { AlertTriangleIcon, LoaderCircleIcon } from 'lucide-react'
import { useDataStatus } from '@/api/queries'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'

/** Explains an empty database: the first import is still running or it failed. */
export function ImportNotice() {
  const { data: status } = useDataStatus()
  if (!status || status.games > 0) return null

  if (status.importRunning) {
    return (
      <Alert>
        <LoaderCircleIcon className="animate-spin" />
        <AlertTitle>Trwa pierwszy import danych z NBA.com</AlertTitle>
        <AlertDescription>
          Najpierw pobierany jest bieżący sezon, potem starsze. Krok: {status.lastRun?.progress ?? 'start'}.
        </AlertDescription>
      </Alert>
    )
  }

  if (status.lastRun?.status === 'FAILED') {
    return (
      <Alert variant="destructive">
        <AlertTriangleIcon />
        <AlertTitle>Import danych z NBA.com nie powiódł się</AlertTitle>
        <AlertDescription>
          <p className="line-clamp-3 break-words">{status.lastRun.error}</p>
          <p>
            Backend spróbuje ponownie przy następnym uruchomieniu (pełna historia) i o zaplanowanej porze (bieżący
            sezon). Ręcznie: <code>POST /api/admin/ingestion/backfill</code>.
          </p>
        </AlertDescription>
      </Alert>
    )
  }

  return (
    <Alert>
      <AlertTriangleIcon />
      <AlertTitle>Baza jest pusta</AlertTitle>
      <AlertDescription>
        Uruchom import: <code>POST /api/admin/ingestion/backfill</code>.
      </AlertDescription>
    </Alert>
  )
}
