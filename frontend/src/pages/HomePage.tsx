import { Link } from 'react-router'
import { ImportNotice } from '@/features/data-status/ImportNotice'
import { PlayerSearch } from '@/features/player-search/PlayerSearch'

const EXAMPLES = [
  { id: 1628983, name: 'Shai Gilgeous-Alexander' },
  { id: 203999, name: 'Nikola Jokić' },
  { id: 1641705, name: 'Victor Wembanyama' },
  { id: 1629029, name: 'Luka Dončić' },
  { id: 203507, name: 'Giannis Antetokounmpo' },
  { id: 1630162, name: 'Anthony Edwards' },
]

export function HomePage() {
  return (
    <div className="mx-auto flex max-w-2xl flex-col items-center gap-6 py-16 text-center">
      <h1 className="text-3xl font-semibold tracking-tight">Statystyki zawodników NBA</h1>
      <p className="text-muted-foreground">
        Wybierz zawodnika, statystykę i linię, a zobaczysz, jak często przekraczał ją w ostatnich meczach.
      </p>
      <PlayerSearch autoFocus className="w-full max-w-md" />
      <div className="w-full text-left">
        <ImportNotice />
      </div>
      <div className="flex flex-wrap justify-center gap-2 text-sm">
        <span className="text-muted-foreground">Na przykład:</span>
        {EXAMPLES.map((example) => (
          <Link key={example.id} to={`/players/${example.id}`} className="underline-offset-2 hover:underline">
            {example.name}
          </Link>
        ))}
      </div>
    </div>
  )
}
