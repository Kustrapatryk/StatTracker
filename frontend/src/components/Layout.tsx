import { Link, Outlet, useLocation } from 'react-router'
import { DataStatusFooter } from '@/features/data-status/DataStatusFooter'
import { PlayerSearch } from '@/features/player-search/PlayerSearch'

export function Layout() {
  const onHomePage = useLocation().pathname === '/'
  return (
    <div className="flex min-h-svh flex-col">
      <header className="border-b">
        <div className="mx-auto flex h-14 max-w-[1600px] items-center justify-between gap-4 px-4">
          <Link to="/" className="text-lg font-semibold tracking-tight">
            Stat<span className="text-orange-600">Tracker</span>
          </Link>
          {!onHomePage && <PlayerSearch className="w-full max-w-sm" />}
        </div>
      </header>
      <main className="mx-auto w-full max-w-[1600px] flex-1 px-4 py-6">
        <Outlet />
      </main>
      <DataStatusFooter />
    </div>
  )
}
