import { formatDecimal, formatLine, formatShare } from '@/lib/format'
import { summarize } from '@/lib/stats'

interface Props {
  /** Oldest first, like the chart. */
  values: readonly number[]
  line: number
}

const WINDOWS = [5, 10, 20]

export function HitSummary({ values, line }: Props) {
  const all = summarize(values, line)
  const windows = WINDOWS.filter((size) => size < values.length).map((size) => ({
    size,
    summary: summarize(values.slice(-size), line),
  }))

  return (
    <dl className="flex flex-wrap gap-x-8 gap-y-3">
      <Figure
        label={`Powyżej ${formatLine(line)}`}
        value={`${all.over}/${all.games}`}
        detail={formatShare(all.over, all.games)}
        emphasis
      />
      {windows.map(({ size, summary }) => (
        <Figure key={size} label={`L${size}`} value={`${summary.over}/${size}`} detail={formatShare(summary.over, size)} />
      ))}
      <Figure label="Średnia" value={formatDecimal(all.average)} />
      <Figure label="Mediana" value={formatDecimal(all.median)} />
      {all.push > 0 && <Figure label="Równo z linią" value={String(all.push)} />}
    </dl>
  )
}

function Figure({ label, value, detail, emphasis }: { label: string; value: string; detail?: string; emphasis?: boolean }) {
  return (
    <div>
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className={emphasis ? 'text-2xl font-semibold tabular-nums' : 'text-lg font-medium tabular-nums'}>
        {value}
        {detail && <span className="ml-1.5 text-sm font-normal text-muted-foreground">{detail}</span>}
      </dd>
    </div>
  )
}
