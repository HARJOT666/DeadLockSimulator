import { Link } from 'react-router-dom'
import { Mark } from '../../components/brand/Wordmark'

export default function NotFoundPage() {
  return (
    <main className="grid min-h-screen place-items-center bg-ivory px-6">
      <div className="max-w-md">
        <Mark className="size-9" />
        <p className="kicker mt-8">Error 404</p>
        <h1 className="display mt-3 text-[40px]">No such page.</h1>
        <p className="mt-3 text-cocoa">The address does not correspond to any part of the simulator.</p>
        <div className="mt-8 flex gap-6 text-[14px]">
          <Link to="/dashboard" className="link font-medium">Go to the overview</Link>
          <Link to="/" className="link">How it works</Link>
        </div>
      </div>
    </main>
  )
}
