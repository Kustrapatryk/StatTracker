import { useState } from 'react'
import type { PlayerProfile } from '@/api/client'
import { Badge } from '@/components/ui/badge'
import { formatHeight, formatWeight } from '@/lib/format'

export function PlayerHeader({ player }: { player: PlayerProfile }) {
  const details = [
    player.position,
    player.jerseyNumber && `#${player.jerseyNumber}`,
    formatHeight(player.height),
    formatWeight(player.weightLbs),
    player.country,
    player.draftYear && `draft ${player.draftYear}`,
  ].filter(Boolean)

  return (
    <div className="flex items-center gap-4">
      <Headshot player={player} />
      <div className="min-w-0">
        <h1 className="truncate text-2xl font-semibold tracking-tight">{player.fullName}</h1>
        <p className="flex flex-wrap items-center gap-x-2 gap-y-1 text-sm text-muted-foreground">
          {player.team ? (
            <span className="flex items-center gap-1.5 font-medium text-foreground">
              <TeamLogo teamId={player.team.id} />
              {player.team.city} {player.team.name}
            </span>
          ) : (
            <Badge variant="outline">{player.active ? 'bez drużyny' : 'zakończył karierę'}</Badge>
          )}
          {details.length > 0 && <span>{details.join(' · ')}</span>}
        </p>
      </div>
    </div>
  )
}

function Headshot({ player }: { player: PlayerProfile }) {
  const [failed, setFailed] = useState(false)
  const initials = `${player.firstName.charAt(0)}${player.lastName.charAt(0)}`
  if (failed) {
    return (
      <div className="flex size-16 shrink-0 items-center justify-center rounded-full bg-muted text-lg font-semibold text-muted-foreground">
        {initials}
      </div>
    )
  }
  return (
    <img
      src={`https://cdn.nba.com/headshots/nba/latest/260x190/${player.id}.png`}
      alt=""
      className="h-16 w-[88px] shrink-0 rounded-lg bg-muted object-cover object-top"
      onError={() => setFailed(true)}
    />
  )
}

function TeamLogo({ teamId }: { teamId: number }) {
  const [failed, setFailed] = useState(false)
  if (failed) return null
  return (
    <img
      src={`https://cdn.nba.com/logos/nba/${teamId}/global/L/logo.svg`}
      alt=""
      className="size-5"
      onError={() => setFailed(true)}
    />
  )
}
