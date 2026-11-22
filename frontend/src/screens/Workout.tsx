import { useEffect, useMemo, useRef, useState } from 'react'
import { api, Exercise, fmtNum, Item, ProtocolView, TECHNIQUES, today, Workout as WorkoutT } from '../api'
import { Button, Card, Field, inputClass, Meter, Notice, Sheet } from '../ui'

type Entry = { kg: string; reps: string; done: boolean }
type Draft = { protocolId: number; workoutCode: string; startedAt: number; entries: Entry[][]; swaps: Record<number, string>; open: number }

const DRAFT_KEY = 'forja.workout'

export function readDraft(): Draft | null {
  try { return JSON.parse(localStorage.getItem(DRAFT_KEY) || 'null') } catch { return null }
}
function writeDraft(d: Draft | null) {
  try { d ? localStorage.setItem(DRAFT_KEY, JSON.stringify(d)) : localStorage.removeItem(DRAFT_KEY) } catch { /* segue sem persistir */ }
}

const clock = (ms: number) => {
  const s = Math.max(0, Math.floor(ms / 1000))
  return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`
}

/** Fases de uma repeticao a partir da cadencia (descida, pausa embaixo, subida, pausa em cima). */
function cadencePhases(cadence: string) {
  if (cadence === 'ISO') return []
  const [down, hold1, up, hold2] = cadence.split('').map(Number)
  return [
    { label: 'Desce', sec: down }, { label: 'Segura', sec: hold1 }, { label: 'Sobe', sec: up }, { label: 'Contrai', sec: hold2 },
  ].filter((p) => p.sec > 0)
}

let audio: AudioContext | null = null
function beep(times: number) {
  try {
    audio = audio || new (window.AudioContext || (window as any).webkitAudioContext)()
    for (let i = 0; i < times; i++) {
      const osc = audio.createOscillator()
      const gain = audio.createGain()
      osc.frequency.value = i === times - 1 ? 1175 : 880
      gain.gain.value = 0.25
      osc.connect(gain).connect(audio.destination)
      osc.start(audio.currentTime + i * 0.28)
      osc.stop(audio.currentTime + i * 0.28 + 0.18)
    }
  } catch { /* sem audio disponivel */ }
  try { navigator.vibrate?.([200, 100, 200]) } catch { /* sem vibracao */ }
}

function Demo({ ex }: { ex: Exercise }) {
  const [frame, setFrame] = useState(0)
  const [playing, setPlaying] = useState(true)
  useEffect(() => {
    if (!playing) return
    const id = setInterval(() => setFrame((f) => 1 - f), 1100)
    return () => clearInterval(id)
  }, [playing])
  const video = `https://www.youtube.com/results?search_query=${encodeURIComponent(`${ex.name} execução correta`)}`
  return (
    <div>
      <button type="button" onClick={() => setPlaying(!playing)} className="relative block w-full aspect-[3/2] rounded-xl overflow-hidden bg-ink"
        aria-label={playing ? 'Pausar demonstração' : 'Reproduzir demonstração'}>
        {[0, 1].map((i) => (
          <img key={i} src={`/ex/${ex.img}/${i}.jpg`} alt={i === 0 ? `Posição inicial: ${ex.name}` : `Posição final: ${ex.name}`}
            className={`absolute inset-0 size-full object-cover ${frame === i ? 'opacity-100' : 'opacity-0'}`} />
        ))}
        <span className="absolute left-2 bottom-2 rounded-md bg-ink/80 px-2 py-0.5 text-xs text-steel">{frame === 0 ? 'Início' : 'Final'} do movimento</span>
      </button>
      <p className="text-sm text-chalk/90 mt-3">{ex.cue}</p>
      <a href={video} target="_blank" rel="noreferrer" className="inline-flex items-center gap-2 mt-3 min-h-10 px-3 rounded-lg bg-rail text-sm font-medium">
        <svg viewBox="0 0 24 24" className="size-4 text-flare" fill="currentColor" aria-hidden="true"><path d="M8 5v14l11-7z" /></svg>
        Ver vídeo da execução
      </a>
    </div>
  )
}

export default function Workout({ workout, protocol, onExit }: { workout: WorkoutT; protocol: ProtocolView; onExit: (saved: boolean) => void }) {
  const initial = useMemo<Draft>(() => {
    const d = readDraft()
    if (d && d.protocolId === protocol.id && d.workoutCode === workout.code && d.entries.length === workout.items.length) return d
    return {
      protocolId: protocol.id, workoutCode: workout.code, startedAt: Date.now(), swaps: {}, open: 0,
      entries: workout.items.map((it) => it.rx.sets.map((s) => ({ kg: s.suggestedKg != null ? String(s.suggestedKg) : '', reps: '', done: false }))),
    }
  }, [workout, protocol])
  const [draft, setDraft] = useState<Draft>(initial)
  const [now, setNow] = useState(Date.now())
  const [exec, setExec] = useState<{ item: number; set: number; startedAt: number } | null>(null)
  const [rest, setRest] = useState<{ until: number; total: number; fired: boolean } | null>(null)
  const [swapFor, setSwapFor] = useState<number | null>(null)
  const [showWarmup, setShowWarmup] = useState(false)
  const [finishing, setFinishing] = useState(false)
  const [pain, setPain] = useState(0)
  const [energy, setEnergy] = useState(3)
  const [notes, setNotes] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const restRef = useRef(rest)
  restRef.current = rest

  useEffect(() => { writeDraft(draft) }, [draft])

  useEffect(() => {
    const id = setInterval(() => {
      setNow(Date.now())
      const r = restRef.current
      if (r && !r.fired && Date.now() >= r.until) { beep(3); setRest({ ...r, fired: true }) }
    }, 250)
    return () => clearInterval(id)
  }, [])

  // Mantem a tela acesa durante o treino
  useEffect(() => {
    let lock: any = null
    const request = () => (navigator as any).wakeLock?.request('screen').then((l: any) => { lock = l }).catch(() => {})
    request()
    const onVisible = () => document.visibilityState === 'visible' && request()
    document.addEventListener('visibilitychange', onVisible)
    return () => { document.removeEventListener('visibilitychange', onVisible); lock?.release?.().catch(() => {}) }
  }, [])

  const exerciseOf = (i: number): Exercise => {
    const code = draft.swaps[i]
    return (code && workout.items[i].alternatives.find((a) => a.code === code)) || workout.items[i].exercise
  }
  const update = (item: number, set: number, patch: Partial<Entry>) =>
    setDraft((d) => ({ ...d, entries: d.entries.map((sets, i) => (i !== item ? sets : sets.map((e, j) => (j === set ? { ...e, ...patch } : e)))) }))

  const totalSets = draft.entries.reduce((n, s) => n + s.length, 0)
  const doneSets = draft.entries.reduce((n, s) => n + s.filter((e) => e.done).length, 0)

  const startSet = (item: number, set: number) => { beep(1); setRest(null); setExec({ item, set, startedAt: Date.now() }) }

  const finishSet = (item: number, set: number, elapsedMs?: number) => {
    const it = workout.items[item]
    const rx = it.rx.sets[set]
    const phases = cadencePhases(it.rx.cadence)
    const repSec = phases.reduce((n, p) => n + p.sec, 0)
    const entry = draft.entries[item][set]
    let reps = entry.reps
    if (!reps) {
      if (it.rx.cadence === 'ISO') reps = String(elapsedMs ? Math.round(elapsedMs / 1000) : rx.repHigh)
      else if (elapsedMs && repSec > 0) reps = String(Math.max(1, Math.min(rx.repHigh + 5, Math.round(elapsedMs / 1000 / repSec))))
      else reps = String(rx.repHigh)
    }
    update(item, set, { done: true, reps })
    setExec(null)
    const lastOfWorkout = doneSets + 1 >= totalSets
    if (!lastOfWorkout) setRest({ until: Date.now() + it.rx.restSec * 1000, total: it.rx.restSec, fired: false })
    // Ao fechar a ultima serie do exercicio, abre o proximo
    if (draft.entries[item].every((e, j) => e.done || j === set) && item + 1 < workout.items.length) setDraft((d) => ({ ...d, open: item + 1 }))
  }

  const save = async () => {
    setBusy(true)
    setError(null)
    const sets = draft.entries.flatMap((entries, i) =>
      entries.map((e, j) => ({ e, j })).filter(({ e }) => e.done && Number(e.reps) > 0)
        .map(({ e, j }) => ({ exercise: exerciseOf(i).code, setNumber: j + 1, weight: Number(e.kg.replace(',', '.')) || 0, reps: Number(e.reps) })))
    try {
      await api('/sessions', { method: 'POST', body: { workoutCode: workout.code, date: today(), backPain: pain, energy, notes, durationMin: Math.round((Date.now() - draft.startedAt) / 60000), sets } })
      writeDraft(null)
      onExit(true)
    } catch (e: any) {
      setError(e.message)
    } finally {
      setBusy(false)
    }
  }

  const execItem = exec ? workout.items[exec.item] : null
  const execPhases = execItem ? cadencePhases(execItem.rx.cadence) : []
  const execElapsed = exec ? now - exec.startedAt : 0
  const repSec = execPhases.reduce((n, p) => n + p.sec, 0)
  let phaseLabel = 'Segure a posição'
  let repCount = 0
  if (exec && repSec > 0) {
    const t = execElapsed / 1000
    repCount = Math.floor(t / repSec) + 1
    let within = t % repSec
    for (const p of execPhases) { if (within < p.sec) { phaseLabel = p.label; break } within -= p.sec }
  }
  const restLeft = rest ? rest.until - now : 0

  return (
    <div className="pb-44">
      <header className="sticky top-0 z-20 -mx-4 px-4 pt-3 pb-3 bg-ink/95 backdrop-blur">
        <div className="flex items-center justify-between gap-3">
          <button onClick={() => onExit(false)} className="text-steel text-sm min-h-10 pr-2">← Sair</button>
          <div className="num text-2xl" aria-label="Tempo total de treino">{clock(now - draft.startedAt)}</div>
          <Button kind={doneSets >= totalSets ? 'primary' : 'quiet'} className="min-h-10! px-4! text-sm!" onClick={() => setFinishing(true)}>Finalizar</Button>
        </div>
        <div className="flex items-baseline justify-between mt-2 mb-1.5">
          <h1 className="text-2xl truncate">Treino {workout.code}: {workout.title}</h1>
          <span className="num text-steel shrink-0 ml-3">{doneSets}/{totalSets}</span>
        </div>
        <Meter value={doneSets} max={totalSets} tone="mint" />
      </header>

      <div className="space-y-3 mt-3">
        <Notice>
          <strong>Semana {protocol.week}, {protocol.phase.label.toLowerCase()}.</strong> {protocol.phase.guidance}
        </Notice>
        <Card className="py-3!">
          <button className="w-full flex justify-between items-center text-left" onClick={() => setShowWarmup(!showWarmup)} aria-expanded={showWarmup}>
            <span className="font-medium">Aquecimento e mobilidade</span><span className="text-steel">{showWarmup ? 'Ocultar' : 'Ver'}</span>
          </button>
          {showWarmup && <ol className="mt-3 space-y-2 text-sm list-decimal pl-5 text-chalk/90">{workout.warmup.map((w) => <li key={w}>{w}</li>)}</ol>}
        </Card>

        {workout.items.map((it, i) => {
          const ex = exerciseOf(i)
          const entries = draft.entries[i]
          const done = entries.filter((e) => e.done).length
          const open = draft.open === i
          const phases = cadencePhases(it.rx.cadence)
          const swapped = ex.code !== it.exercise.code
          return (
            <Card key={i} className={done === entries.length ? 'opacity-70' : ''}>
              <button className="w-full flex items-center gap-3 text-left" onClick={() => setDraft((d) => ({ ...d, open: open ? -1 : i }))} aria-expanded={open}>
                <img src={`/ex/${ex.img}/0.jpg`} alt="" className="size-14 rounded-lg object-cover bg-ink shrink-0" loading="lazy" />
                <span className="flex-1 min-w-0">
                  <span className="block text-xs text-steel">{ex.groupLabel}</span>
                  <span className="block font-semibold leading-snug">{ex.name}</span>
                </span>
                <span className={`num text-lg shrink-0 ${done === entries.length ? 'text-mint' : 'text-steel'}`}>{done}/{entries.length}</span>
              </button>

              {open && (
                <div className="mt-4">
                  <Demo ex={ex} />
                  <div className="flex flex-wrap gap-x-5 gap-y-1 text-sm text-steel mt-4">
                    {phases.length > 0 && <span>Cadência: {phases.map((p) => `${p.label.toLowerCase()} ${p.sec} s`).join(', ')}</span>}
                    <span>Descanso: {it.rx.restSec} s</span>
                  </div>
                  {!swapped && it.last.length > 0 && (
                    <p className="text-sm text-steel mt-1">Última vez: {it.last.map((l) => `${fmtNum(l.weight)} kg × ${l.reps}`).join(' / ')}</p>
                  )}

                  <div className="mt-4 space-y-2">
                    <div className="grid grid-cols-[2rem_1fr_1fr_1fr_3.25rem] gap-2 text-xs text-steel px-1">
                      <span>Série</span><span>Meta</span><span>Carga (kg)</span><span>{it.rx.cadence === 'ISO' ? 'Segundos' : 'Repetições'}</span><span />
                    </div>
                    {it.rx.sets.map((s, j) => {
                      const e = entries[j]
                      const running = exec?.item === i && exec.set === j
                      const tech = s.technique ? TECHNIQUES[s.technique] : null
                      return (
                        <div key={j}>
                          <div className="grid grid-cols-[2rem_1fr_1fr_1fr_3.25rem] gap-2 items-center">
                            <span className="num text-xl text-center">{s.n}</span>
                            <span className="num text-xl">{s.reps}{it.rx.cadence === 'ISO' ? ' s' : ''}</span>
                            <input className={`${inputClass} num text-xl text-center px-1!`} inputMode="decimal" value={e.kg} placeholder="kg"
                              aria-label={`Carga da série ${s.n}`} onChange={(ev) => update(i, j, { kg: ev.target.value })} disabled={ex.group === 'CORE' && false} />
                            <input className={`${inputClass} num text-xl text-center px-1!`} inputMode="numeric" value={e.reps} placeholder={String(s.repHigh)}
                              aria-label={`Repetições da série ${s.n}`} onChange={(ev) => update(i, j, { reps: ev.target.value.replace(/\D/g, '') })} />
                            {e.done ? (
                              <button onClick={() => update(i, j, { done: false })} aria-label={`Desmarcar série ${s.n}`}
                                className="h-12 rounded-xl bg-mint/20 text-mint text-xl">✓</button>
                            ) : running ? (
                              <button onClick={() => finishSet(i, j, Date.now() - exec!.startedAt)} aria-label="Concluir série"
                                className="h-12 rounded-xl bg-flare text-ink font-semibold text-sm">Fim</button>
                            ) : (
                              <button onClick={() => startSet(i, j)} aria-label={`Iniciar série ${s.n}`}
                                className="h-12 rounded-xl bg-amber text-ink font-semibold text-sm">Ir</button>
                            )}
                          </div>
                          {tech && <p className="text-xs text-amber mt-1.5 pl-10"><strong>{tech.label}.</strong> {tech.how}</p>}
                        </div>
                      )
                    })}
                  </div>
                  <div className="flex gap-2 mt-4">
                    <Button kind="quiet" className="min-h-10! text-sm! flex-1" onClick={() => {
                      const next = entries.findIndex((e) => !e.done)
                      if (next >= 0) finishSet(i, next)
                    }}>Marcar série feita</Button>
                    {it.alternatives.length > 0 && <Button kind="quiet" className="min-h-10! text-sm!" onClick={() => setSwapFor(i)}>Trocar exercício</Button>}
                  </div>
                </div>
              )}
            </Card>
          )
        })}

        {workout.cardio && <Card><h3 className="text-lg mb-1">Cardio</h3><p className="text-sm text-chalk/90">{workout.cardio}</p></Card>}
      </div>

      {(exec || rest) && (
        <div className="fixed bottom-0 inset-x-0 z-30 safe-bottom">
          <div className="max-w-xl mx-auto m-3 rounded-2xl bg-rail border border-line p-4 shadow-2xl shadow-black/50" role="timer" aria-live="off">
            {exec && execItem && (
              <div className="flex items-center gap-4">
                <div className="flex-1 min-w-0">
                  <div className="text-sm text-steel truncate">Executando série {exec.set + 1}: {exerciseOf(exec.item).name}</div>
                  <div className="flex items-baseline gap-3">
                    <span className="num text-5xl text-amber">{clock(execElapsed)}</span>
                    {repSec > 0 ? <span className="display text-2xl">{phaseLabel} <span className="text-steel">rep {repCount}</span></span> : <span className="display text-2xl">{phaseLabel}</span>}
                  </div>
                </div>
                <Button onClick={() => finishSet(exec.item, exec.set, execElapsed)}>Concluir</Button>
              </div>
            )}
            {!exec && rest && (
              <div>
                <div className="flex items-center gap-4">
                  <div className="flex-1">
                    <div className="text-sm text-steel">{restLeft > 0 ? 'Descanso' : 'Descanso encerrado. Próxima série!'}</div>
                    <span className={`num text-5xl ${restLeft > 0 ? 'text-mint' : 'text-flare'}`}>{restLeft > 0 ? clock(restLeft + 999) : `+${clock(-restLeft)}`}</span>
                  </div>
                  <div className="flex gap-2">
                    <Button kind="quiet" className="px-3!" onClick={() => setRest({ until: rest.until - 15000, total: rest.total, fired: rest.fired })}>−15</Button>
                    <Button kind="quiet" className="px-3!" onClick={() => setRest({ until: Math.max(rest.until, Date.now()) + 15000, total: rest.total + 15, fired: false })}>+15</Button>
                    <Button kind="quiet" className="px-3!" onClick={() => setRest(null)}>Pular</Button>
                  </div>
                </div>
                <div className="mt-3"><Meter value={Math.max(0, restLeft)} max={rest.total * 1000} tone="mint" /></div>
              </div>
            )}
          </div>
        </div>
      )}

      <Sheet open={swapFor !== null} onClose={() => setSwapFor(null)} title="Trocar exercício">
        {swapFor !== null && (
          <div className="space-y-2">
            <p className="text-sm text-steel mb-3">Aparelho ocupado? Escolha outro do mesmo grupo muscular. A troca vale só para hoje.</p>
            {[workout.items[swapFor].exercise, ...workout.items[swapFor].alternatives].map((a) => (
              <button key={a.code} className="w-full flex items-center gap-3 p-2 rounded-xl bg-rail text-left" onClick={() => {
                const original = a.code === workout.items[swapFor].exercise.code
                setDraft((d) => {
                  const swaps = { ...d.swaps }
                  if (original) delete swaps[swapFor]; else swaps[swapFor] = a.code
                  const entries = d.entries.map((s, i) => (i !== swapFor ? s : s.map((e, j) => (e.done ? e : { ...e, kg: original && workout.items[swapFor].rx.sets[j].suggestedKg != null ? String(workout.items[swapFor].rx.sets[j].suggestedKg) : '' }))))
                  return { ...d, swaps, entries }
                })
                setSwapFor(null)
              }}>
                <img src={`/ex/${a.img}/0.jpg`} alt="" className="size-14 rounded-lg object-cover bg-ink" />
                <span className="font-medium">{a.name}</span>
              </button>
            ))}
          </div>
        )}
      </Sheet>

      <Sheet open={finishing} onClose={() => setFinishing(false)} title="Finalizar treino">
        <div className="space-y-5">
          {doneSets < totalSets && <Notice tone="warn">Você concluiu {doneSets} de {totalSets} séries. Só as séries marcadas serão registradas.</Notice>}
          <Field label={`Dor na coluna durante ou depois do treino: ${pain} de 10`} hint="0 é nenhuma dor. A partir de 4, o próximo protocolo troca os exercícios deste treino que pesam na coluna.">
            <input type="range" min={0} max={10} value={pain} onChange={(e) => setPain(Number(e.target.value))} className="w-full accent-amber h-10" />
          </Field>
          <Field label="Disposição no treino">
            <div className="grid grid-cols-5 gap-2">
              {['Péssima', 'Baixa', 'Normal', 'Boa', 'Ótima'].map((l, i) => (
                <button key={l} type="button" onClick={() => setEnergy(i + 1)} aria-pressed={energy === i + 1}
                  className={`min-h-12 rounded-xl text-xs font-medium border ${energy === i + 1 ? 'border-amber bg-amber/10 text-amber' : 'border-line text-steel'}`}>{l}</button>
              ))}
            </div>
          </Field>
          <Field label="Observações"><textarea className={`${inputClass} py-3`} rows={2} value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="Opcional" /></Field>
          {error && <Notice tone="error">{error}</Notice>}
          <Button className="w-full" disabled={busy || doneSets === 0} onClick={save}>Salvar treino</Button>
          <Button kind="danger" className="w-full" onClick={() => { writeDraft(null); onExit(false) }}>Descartar este treino</Button>
        </div>
      </Sheet>
    </div>
  )
}
