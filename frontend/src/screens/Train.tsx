import { useEffect, useState } from 'react'
import { api, fmtDate, TrainView } from '../api'
import { Button, Card, Loading, LoadError, Meter, Notice, PageTitle, useLoad } from '../ui'
import Workout, { readDraft } from './Workout'

export default function Train({ onWorkoutChange }: { onWorkoutChange: (active: boolean) => void }) {
  const { data, error, loading, reload } = useLoad<TrainView>(() => api('/train'))
  const sessions = useLoad<any[]>(() => api('/sessions'))
  const [active, setActive] = useState<string | null>(null)
  const [justSaved, setJustSaved] = useState(false)
  const [showWhy, setShowWhy] = useState(false)

  useEffect(() => { onWorkoutChange(active !== null); return () => onWorkoutChange(false) }, [active, onWorkoutChange])

  // Abre direto o treino pedido pela tela inicial
  useEffect(() => {
    const wanted = sessionStorage.getItem('forja.start')
    if (wanted && data) { sessionStorage.removeItem('forja.start'); setActive(wanted) }
  }, [data])

  if (loading && !data) return <Loading />
  if (error || !data) return <LoadError message={error || 'Não foi possível carregar o treino.'} onRetry={reload} />

  const { protocol, workouts } = data
  const current = workouts.find((w) => w.code === active)
  if (current) {
    return <Workout workout={current} protocol={protocol} onExit={(saved) => { setActive(null); setJustSaved(saved); window.scrollTo(0, 0); if (saved) { reload(); sessions.reload() } }} />
  }
  const draft = readDraft()
  const resumable = draft && draft.protocolId === protocol.id ? draft.workoutCode : null

  return (
    <div>
      <PageTitle title={`Protocolo ${protocol.number}`} sub={`Foco em ${protocol.focusLabel.toLowerCase()}. Iniciado em ${fmtDate(protocol.startDate, { day: '2-digit', month: 'long' })}.`} />
      <div className="space-y-3">
        {justSaved && <Notice tone="good">Treino registrado. As cargas sugeridas da próxima sessão já consideram o que você fez hoje.</Notice>}
        {protocol.finished && <Notice tone="warn">Você concluiu todas as sessões deste protocolo. Registre suas medidas e gere o próximo na aba Evolução.</Notice>}

        <Card>
          <div className="flex items-baseline justify-between">
            <h2 className="text-2xl">Semana {protocol.week} de {protocol.weeks}: {protocol.phase.label}</h2>
            <span className="num text-steel">{protocol.sessionsDone}/{protocol.sessionsTotal}</span>
          </div>
          <div className="flex gap-1.5 my-3" aria-hidden="true">
            {protocol.phases.map((ph, i) => (
              <div key={i} className={`flex-1 h-2 rounded-full ${i + 1 < protocol.week ? 'bg-mint' : i + 1 === protocol.week ? 'bg-amber' : 'bg-rail'}`} title={ph.label} />
            ))}
          </div>
          <p className="text-sm text-chalk/90">{protocol.phase.guidance}</p>
          <button className="text-sm text-amber font-medium mt-3 min-h-10" onClick={() => setShowWhy(!showWhy)} aria-expanded={showWhy}>
            {showWhy ? 'Ocultar explicação' : 'Por que este protocolo é assim'}
          </button>
          {showWhy && <ul className="text-sm space-y-2 list-disc pl-5 text-chalk/90">{protocol.rationale.map((r) => <li key={r}>{r}</li>)}</ul>}
        </Card>

        {workouts.map((w) => {
          const next = w.code === protocol.nextWorkout
          const resume = resumable === w.code
          return (
            <Card key={w.code} className={next ? 'ring-1 ring-amber/60' : ''}>
              <div className="flex gap-4">
                <div className={`num text-6xl leading-none w-12 ${next ? 'text-amber' : 'text-line'}`}>{w.code}</div>
                <div className="flex-1 min-w-0">
                  <h3 className="text-xl">{w.title}</h3>
                  <p className="text-sm text-steel mt-0.5">
                    {w.items.length} exercícios, {w.totalSets} séries{w.lastDone ? `. Último em ${fmtDate(w.lastDone)}` : ''}
                  </p>
                  <p className="text-sm text-chalk/80 mt-2 line-clamp-2">{w.items.map((i) => i.exercise.name).join(', ')}</p>
                </div>
              </div>
              <Button kind={next || resume ? 'primary' : 'quiet'} className="w-full mt-4" onClick={() => { setJustSaved(false); setActive(w.code) }}>
                {resume ? 'Continuar treino em andamento' : next ? 'Começar o treino de hoje' : `Fazer o treino ${w.code}`}
              </Button>
            </Card>
          )
        })}

        {sessions.data && sessions.data.length > 0 && (
          <Card>
            <h2 className="text-2xl mb-3">Últimas sessões</h2>
            <ul className="divide-y divide-line">
              {[...sessions.data].reverse().slice(0, 8).map((s) => (
                <li key={s.id} className="py-2.5 flex items-center gap-3">
                  <span className="num text-2xl w-8 text-amber">{s.workoutCode}</span>
                  <span className="flex-1">
                    <span className="block">{fmtDate(s.date, { weekday: 'short', day: '2-digit', month: 'short' })}</span>
                    <span className="block text-sm text-steel">{s.sets} séries{s.durationMin ? `, ${s.durationMin} min` : ''}{s.backPain >= 4 ? `, dor na coluna ${s.backPain}` : ''}</span>
                  </span>
                  <span className="num text-steel">{Math.round(s.volumeKg).toLocaleString('pt-BR')} kg</span>
                </li>
              ))}
            </ul>
          </Card>
        )}
        <div className="pt-1"><Meter value={protocol.sessionsDone} max={protocol.sessionsTotal} tone="steel" /></div>
      </div>
    </div>
  )
}
