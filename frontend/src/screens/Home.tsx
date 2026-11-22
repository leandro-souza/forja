import { api, fmtDate, fmtNum, Measurement, session, today, TrainView } from '../api'
import { Button, Card, Loading, LoadError, Meter, PageTitle, useLoad } from '../ui'

export default function Home({ go }: { go: (tab: any) => void }) {
  const train = useLoad<TrainView>(() => api('/train'))
  const menu = useLoad<any>(() => api(`/nutrition/menu?date=${today()}`))
  const measures = useLoad<Measurement[]>(() => api('/measurements'))

  if (train.loading && !train.data) return <Loading />
  if (train.error || !train.data) return <LoadError message={train.error || 'Não foi possível carregar.'} onRetry={train.reload} />

  const { protocol, workouts } = train.data
  const next = workouts.find((w) => w.code === protocol.nextWorkout)!
  const hour = new Date().getHours()
  const greeting = hour < 12 ? 'Bom dia' : hour < 18 ? 'Boa tarde' : 'Boa noite'
  const first = (session.name() || '').split(' ')[0]
  const trainedToday = workouts.some((w) => w.lastDone === today())

  const nowMin = hour * 60 + new Date().getMinutes()
  const nextMeal = menu.data?.meals.find((m: any) => { const [h, mi] = m.time.split(':').map(Number); return h * 60 + mi >= nowMin - 30 }) || menu.data?.meals[0]

  const weights = (measures.data || []).filter((m) => m.weightKg != null)
  const last = weights[weights.length - 1]
  const base = weights.filter((m) => m.date <= protocol.startDate).pop() || weights[0]
  const daysSince = last ? Math.round((Date.now() - new Date(`${last.date}T12:00:00`).getTime()) / 86400000) : null

  return (
    <div>
      <PageTitle title={`${greeting}${first ? `, ${first}` : ''}`} sub={fmtDate(today(), { weekday: 'long', day: 'numeric', month: 'long' })} />
      <div className="space-y-3">
        <Card className="ring-1 ring-amber/50">
          <p className="text-sm text-steel">{trainedToday ? 'Treino de hoje concluído. O próximo é' : 'Treino de hoje'}</p>
          <div className="flex gap-4 items-center mt-1">
            <div className="num text-7xl leading-none text-amber">{next.code}</div>
            <div className="min-w-0">
              <h2 className="text-2xl">{next.title}</h2>
              <p className="text-sm text-steel">{next.items.length} exercícios, {next.totalSets} séries</p>
            </div>
          </div>
          <div className="mt-4 mb-1.5 flex justify-between text-sm">
            <span>Protocolo {protocol.number}, semana {protocol.week} de {protocol.weeks}: {protocol.phase.label.toLowerCase()}</span>
            <span className="num text-steel">{protocol.sessionsDone}/{protocol.sessionsTotal}</span>
          </div>
          <Meter value={protocol.sessionsDone} max={protocol.sessionsTotal} />
          <Button className="w-full mt-4" onClick={() => { sessionStorage.setItem('forja.start', next.code); go('treino') }}>
            {trainedToday ? 'Ver o próximo treino' : 'Começar o treino'}
          </Button>
        </Card>

        {menu.data && (
          <Card onClick={() => go('nutricao')}>
            <div className="flex items-baseline justify-between">
              <h2 className="text-2xl">Dieta de hoje</h2>
              <span className="num text-xl text-amber">{menu.data.targets.kcal.toLocaleString('pt-BR')} kcal</span>
            </div>
            <div className="grid grid-cols-3 gap-3 mt-3 text-center">
              {[['Proteína', menu.data.targets.protein], ['Carboidrato', menu.data.targets.carbs], ['Gordura', menu.data.targets.fat]].map(([l, v]) => (
                <div key={l} className="bg-rail rounded-xl py-2.5">
                  <div className="num text-2xl">{v} g</div>
                  <div className="text-xs text-steel">{l}</div>
                </div>
              ))}
            </div>
            {nextMeal && (
              <p className="mt-3 text-sm">
                <span className="text-steel">Próxima refeição, {nextMeal.time}: </span>{nextMeal.option}
              </p>
            )}
          </Card>
        )}

        <Card onClick={() => go('evolucao')}>
          <div className="flex items-baseline justify-between">
            <h2 className="text-2xl">Peso</h2>
            {last && <span className="num text-xl">{fmtNum(last.weightKg)} kg</span>}
          </div>
          {last && base && base !== last && (
            <p className="text-sm mt-1">
              {fmtNum((last.weightKg || 0) - (base.weightKg || 0)) || '0'} kg desde {fmtDate(base.date)}
            </p>
          )}
          <p className="text-sm text-steel mt-1">
            {daysSince == null ? 'Registre seu peso para acompanhar o ganho.' : daysSince >= 7 ? `Última pesagem há ${daysSince} dias. Pese-se em jejum e registre.` : 'Pese-se uma vez por semana, em jejum, sempre no mesmo dia.'}
          </p>
        </Card>
      </div>
    </div>
  )
}
