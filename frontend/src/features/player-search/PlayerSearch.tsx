import { useState } from 'react'
import { useNavigate } from 'react-router'
import type { PlayerSearchResult } from '@/api/client'
import { usePlayerSearch } from '@/api/queries'
import { Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList } from '@/components/ui/command'
import { useDebouncedValue } from '@/lib/useDebouncedValue'
import { cn } from '@/lib/utils'

interface Props {
  className?: string
  autoFocus?: boolean
}

export function PlayerSearch({ className, autoFocus }: Props) {
  const navigate = useNavigate()
  const [query, setQuery] = useState('')
  const [open, setOpen] = useState(false)
  const search = usePlayerSearch(useDebouncedValue(query.trim(), 250))
  const results = search.data ?? []

  function choose(player: PlayerSearchResult) {
    setQuery('')
    setOpen(false)
    navigate(`/players/${player.id}`)
  }

  return (
    // the server already ranks the results, cmdk must not filter them again
    <Command
      label="Szukaj zawodnika"
      shouldFilter={false}
      className={cn('relative overflow-visible bg-transparent p-0', className)}
    >
      <CommandInput
        value={query}
        onValueChange={(value) => {
          setQuery(value)
          setOpen(true)
        }}
        onFocus={() => setOpen(true)}
        onBlur={() => setOpen(false)}
        placeholder="Szukaj zawodnika…"
        autoFocus={autoFocus}
      />
      {open && query.trim().length >= 2 && (
        <CommandList
          className="absolute inset-x-0 top-full z-50 mt-1 rounded-lg border bg-popover p-1 shadow-lg"
          // keeps focus in the input, otherwise blur closes the list before the click lands
          onMouseDown={(event) => event.preventDefault()}
        >
          {search.isError && <p className="py-6 text-center text-sm text-destructive">Wyszukiwarka nie odpowiada</p>}
          {search.isFetching && results.length === 0 && (
            <p className="py-6 text-center text-sm text-muted-foreground">Szukam…</p>
          )}
          {search.isSuccess && !search.isFetching && <CommandEmpty>Nie znaleziono zawodnika</CommandEmpty>}
          <CommandGroup>
            {results.map((player) => (
              <CommandItem key={player.id} value={String(player.id)} onSelect={() => choose(player)}>
                <span className="font-medium">{player.fullName}</span>
                <span className="truncate text-xs text-muted-foreground">{describe(player)}</span>
              </CommandItem>
            ))}
          </CommandGroup>
        </CommandList>
      )}
    </Command>
  )
}

function describe(player: PlayerSearchResult): string {
  const years = player.fromYear && player.toYear ? `${player.fromYear}–${player.toYear}` : null
  return [player.teamAbbreviation, player.position, player.active ? null : years].filter(Boolean).join(' · ')
}
