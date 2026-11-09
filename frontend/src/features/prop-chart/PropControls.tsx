import type { ReactNode } from 'react'
import type { SeasonType } from '@/api/client'
import { Checkbox } from '@/components/ui/checkbox'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectGroup, SelectItem, SelectLabel, SelectTrigger, SelectValue } from '@/components/ui/select'
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group'
import { SEASON_TYPE_LABELS } from '@/lib/format'
import { GAME_COUNTS, type GameCount, SEASON_TYPES } from '@/lib/games'
import { isStatKey, STAT_GROUPS, STATS } from '@/lib/stats'
import type { ChartParams } from './chartParams'
import { LineStepper } from './LineStepper'

interface Props {
  params: ChartParams
  line: number
  latestSeason: string | undefined
  onChange: (changes: Partial<ChartParams>) => void
}

export function PropControls({ params, line, latestSeason, onChange }: Props) {
  return (
    <div className="flex flex-wrap items-end gap-x-6 gap-y-3">
      <Field label="Statystyka" htmlFor="stat">
        <Select value={params.stat} onValueChange={(value) => isStatKey(value) && onChange({ stat: value })}>
          <SelectTrigger id="stat" className="w-48">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {STAT_GROUPS.map((group) => (
              <SelectGroup key={group.label}>
                <SelectLabel>{group.label}</SelectLabel>
                {group.keys.map((key) => (
                  <SelectItem key={key} value={key}>
                    {STATS[key].label} <span className="text-muted-foreground">{STATS[key].short}</span>
                  </SelectItem>
                ))}
              </SelectGroup>
            ))}
          </SelectContent>
        </Select>
      </Field>

      <Field label="Linia" htmlFor="line">
        <LineStepper id="line" value={line} onChange={(next) => onChange({ line: next })} />
      </Field>

      <Field label="Mecze">
        <ToggleGroup
          type="single"
          variant="outline"
          spacing={0}
          value={String(params.count)}
          onValueChange={(value) => value && onChange({ count: Number(value) as GameCount })}
          aria-label="Liczba meczów"
        >
          {GAME_COUNTS.map((count) => (
            <ToggleGroupItem key={count} value={String(count)} className="w-10 tabular-nums">
              {count}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
      </Field>

      <Field label="Fazy">
        <ToggleGroup
          type="multiple"
          variant="outline"
          spacing={0}
          value={params.phases}
          onValueChange={(value) => value.length > 0 && onChange({ phases: value as SeasonType[] })}
          aria-label="Fazy sezonu"
        >
          {SEASON_TYPES.map((type) => (
            <ToggleGroupItem key={type} value={type} className="px-3">
              {SEASON_TYPE_LABELS[type]}
            </ToggleGroupItem>
          ))}
        </ToggleGroup>
      </Field>

      {latestSeason && (
        <div className="flex h-8 items-center gap-2">
          <Checkbox
            id="latest-season-only"
            checked={params.latestSeasonOnly}
            onCheckedChange={(checked) => onChange({ latestSeasonOnly: checked === true })}
          />
          <Label htmlFor="latest-season-only">Tylko sezon {latestSeason}</Label>
        </div>
      )}
    </div>
  )
}

function Field({ label, htmlFor, children }: { label: string; htmlFor?: string; children: ReactNode }) {
  return (
    <div className="flex flex-col gap-1.5">
      {htmlFor ? (
        <Label htmlFor={htmlFor} className="text-xs text-muted-foreground">
          {label}
        </Label>
      ) : (
        <span className="text-xs font-medium text-muted-foreground">{label}</span>
      )}
      {children}
    </div>
  )
}
