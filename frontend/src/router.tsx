import { LoaderCircleIcon } from 'lucide-react'
import { createBrowserRouter } from 'react-router'
import { Layout } from '@/components/Layout'
import { HomePage } from '@/pages/HomePage'
import { NotFoundPage } from '@/pages/NotFoundPage'

export const router = createBrowserRouter([
  {
    element: <Layout />,
    // shown when the app opens directly on a lazy route, until its code arrives
    hydrateFallbackElement: (
      <div className="flex min-h-svh items-center justify-center">
        <LoaderCircleIcon className="size-6 animate-spin text-muted-foreground" />
      </div>
    ),
    children: [
      { path: '/', element: <HomePage /> },
      {
        path: '/players/:playerId',
        // the chart library is only needed here
        lazy: async () => ({ Component: (await import('@/pages/PlayerPage')).PlayerPage }),
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
