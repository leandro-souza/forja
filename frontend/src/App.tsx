import { useEffect, useState } from 'react'
import { api, session, Profile } from './api'
import { Loading, LoadError } from './ui'
import Auth from './screens/Auth'
import ProfileForm from './screens/ProfileForm'
import Home from './screens/Home'
import Train from './screens/Train'
import Nutrition from './screens/Nutrition'
import Progress from './screens/Progress'
import Account from './screens/Account'

type Tab = 'inicio' | 'treino' | 'nutricao' | 'evolucao' | 'perfil'

const TABS: { id: Tab; label: string; icon: string }[] = [
  { id: 'inicio', label: 'Início', icon: 'M3 11.5 12 4l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z' },
  { id: 'treino', label: 'Treino', icon: 'M2 10h2v4H2zM5 7h3v10H5zM9 11h6v2H9zM16 7h3v10h-3zM20 10h2v4h-2z' },
  { id: 'nutricao', label: 'Nutrição', icon: 'M7 3v8a2 2 0 0 0 2 2v8h2v-8a2 2 0 0 0 2-2V3h-2v6h-1V3H9v6H8V3zM17 3c-1.7 1-3 3.5-3 6.5 0 2 1 3.200 2 3.500V21h2V3z' },
  { id: 'evolucao', label: 'Evolução', icon: 'M4 20V4h2v14h14v2zM8 15l3.500-4.500 3 2.500L19 7l1.500 1.300-5.700 7.700-3-2.500L9.500 16.200z' },
  { id: 'perfil', label: 'Perfil', icon: 'M12 12a4.500 4.500 0 1 0 0-9 4.500 4.500 0 0 0 0 9zm0 2c-4.400 0-8 2.200-8 5v2h16v-2c0-2.800-3.600-5-8-5z' },
]

function currentTab(): Tab {
  const hash = location.hash.replace('#/', '') as Tab
  return TABS.some((t) => t.id === hash) ? hash : 'inicio'
}

export default function App() {
  const [authed, setAuthed] = useState(!!session.token())
  const [state, setState] = useState<{ profile: Profile | null; hasProtocol: boolean } | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [tab, setTab] = useState<Tab>(currentTab())
  const [inWorkout, setInWorkout] = useState(false)

  const load = () => {
    setError(null)
    api('/profile').then(setState).catch((e) => { if (e.status !== 401) setError(e.message) })
  }

  useEffect(() => {
    const onLogout = () => { setAuthed(false); setState(null) }
    const onHash = () => setTab(currentTab())
    window.addEventListener('forja:logout', onLogout)
    window.addEventListener('hashchange', onHash)
    return () => { window.removeEventListener('forja:logout', onLogout); window.removeEventListener('hashchange', onHash) }
  }, [])

  useEffect(() => { if (authed) load() }, [authed])

  const go = (t: Tab) => { location.hash = `#/${t}`; setTab(t); window.scrollTo(0, 0) }

  if (!authed) return <Auth onDone={() => setAuthed(true)} />
  if (error) return <div className="max-w-xl mx-auto px-4"><LoadError message={error} onRetry={load} /></div>
  if (!state) return <Loading />
  if (!state.hasProtocol) {
    return (
      <div className="max-w-xl mx-auto px-4 pb-10 safe-top">
        <ProfileForm initial={state.profile} first onSaved={load} />
      </div>
    )
  }

  return (
    <div className="max-w-xl mx-auto min-h-full">
      <main className={`px-4 safe-top ${inWorkout ? 'pb-6' : 'pb-28'}`}>
        {tab === 'inicio' && <Home go={go} />}
        {tab === 'treino' && <Train onWorkoutChange={setInWorkout} />}
        {tab === 'nutricao' && <Nutrition />}
        {tab === 'evolucao' && <Progress go={go} />}
        {tab === 'perfil' && <Account profile={state.profile} onChanged={load} onLogout={() => { session.clear(); setAuthed(false); setState(null) }} />}
      </main>
      {!inWorkout && (
        <nav className="fixed bottom-0 inset-x-0 z-30 bg-deck/95 backdrop-blur border-t border-line safe-bottom" aria-label="Navegação principal">
          <div className="max-w-xl mx-auto grid grid-cols-5">
            {TABS.map((t) => (
              <button key={t.id} onClick={() => go(t.id)} aria-current={tab === t.id ? 'page' : undefined}
                className={`flex flex-col items-center gap-1 pt-2.5 pb-2 text-xs font-medium ${tab === t.id ? 'text-amber' : 'text-steel'}`}>
                <svg viewBox="0 0 24 24" className="size-6" fill="currentColor" aria-hidden="true"><path d={t.icon} /></svg>
                {t.label}
              </button>
            ))}
          </div>
        </nav>
      )}
    </div>
  )
}
