import { MinusIcon, PlusIcon } from 'lucide-react'
import { useState } from 'react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { formatLine } from '@/lib/format'
import { MAX_LINE, parseLine } from '@/lib/stats'

interface Props {
  id?: string
  value: number
  onChange: (line: number) => void
}

const STEP = 0.5

export function LineStepper({ id, value, onChange }: Props) {
  const [draft, setDraft] = useState<string | null>(null)

  function commit() {
    if (draft === null) return
    const parsed = parseLine(draft)
    if (parsed !== null) onChange(parsed)
    setDraft(null)
  }

  return (
    <div className="flex items-center gap-1">
      <Button
        variant="outline"
        size="icon"
        aria-label="Obniż linię"
        disabled={value - STEP < 0}
        onClick={() => onChange(value - STEP)}
      >
        <MinusIcon />
      </Button>
      <Input
        id={id}
        className="w-16 text-center font-semibold tabular-nums"
        inputMode="decimal"
        value={draft ?? formatLine(value)}
        onChange={(event) => setDraft(event.target.value)}
        onBlur={commit}
        onKeyDown={(event) => {
          if (event.key === 'Enter') commit()
          if (event.key === 'Escape') setDraft(null)
        }}
      />
      <Button
        variant="outline"
        size="icon"
        aria-label="Podnieś linię"
        disabled={value + STEP > MAX_LINE}
        onClick={() => onChange(value + STEP)}
      >
        <PlusIcon />
      </Button>
    </div>
  )
}
