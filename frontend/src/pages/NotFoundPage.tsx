import { Link } from 'react-router'
import { Button } from '@/components/ui/button'

export function NotFoundPage({ message = 'Nie ma takiej strony.' }: { message?: string }) {
  return (
    <div className="flex flex-col items-center gap-4 py-24 text-center">
      <p className="text-5xl font-semibold text-muted-foreground">404</p>
      <p>{message}</p>
      <Button asChild variant="outline">
        <Link to="/">Wróć do wyszukiwarki</Link>
      </Button>
    </div>
  )
}
