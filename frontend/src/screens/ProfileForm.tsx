import { FormEvent, useEffect, useState } from 'react'
import { api, Profile, session } from '../api'
import { Button, Card, Check, Field, inputClass, Notice, PageTitle } from '../ui'

const PARQ = [
  'Sente dor no peito durante esforço físico?',
  'Já teve tontura forte ou desmaio ao se exercitar?',
  'Tem algum problema cardíaco diagnosticado?',
  'Tem pressão alta sem controle médico?',
]

const DEFAULTS: Profile = {
  sex: 'M', goal: 'HIPERTROFIA', trainingDays: 6, trainingStart: '07:00', trainingEnd: '09:00', jobActivity: 'SEDENTARIO',
  monthsOff: 0, backPain: false, kneePain: false, machinesOnly: true, lactoseIntolerant: false, parqAlert: false,
}

/** Avaliacao inicial (anamnese). Na primeira vez tambem escolhe o foco do Protocolo 1. */
export default function ProfileForm({ initial, first, onSaved }: { initial: Profile | null; first?: boolean; onSaved: () => void }) {
  const [p, setP] = useState<Profile>({ ...DEFAULTS, name: session.name(), ...(initial || {}) })
  const [parq, setParq] = useState<boolean[]>(PARQ.map(() => false))
  const [focus, setFocus] = useState('PEITO')
  const [options, setOptions] = useState<{ code: string; label: string }[]>([])
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [saved, setSaved] = useState(false)

  useEffect(() => { if (first) api('/protocols/focus-options').then(setOptions).catch(() => {}) }, [first])

  const set = <K extends keyof Profile>(key: K, value: Profile[K]) => { setP((old) => ({ ...old, [key]: value })); setSaved(false) }
  const num = (v: string) => (v === '' ? null : Number(v.replace(',', '.')))

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const body = { ...p, parqAlert: first ? parq.some(Boolean) : p.parqAlert }
      await api(`/profile?firstFocus=${focus}`, { method: 'PUT', body })
      setSaved(true)
      onSaved()
    } catch (err: any) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      {first && <PageTitle title="Avaliação inicial" sub="Estas respostas definem seu treino, sua dieta e os cuidados com a sua coluna. Leva uns 3 minutos." />}

      <Card className="space-y-4">
        <h2 className="text-2xl">Você</h2>
        <Field label="Nome"><input className={inputClass} value={p.name || ''} onChange={(e) => set('name', e.target.value)} required /></Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Nascimento"><input className={inputClass} type="date" value={p.birthDate || ''} onChange={(e) => set('birthDate', e.target.value || null)} required /></Field>
          <Field label="Sexo">
            <select className={inputClass} value={p.sex} onChange={(e) => set('sex', e.target.value)}>
              <option value="M">Masculino</option><option value="F">Feminino</option>
            </select>
          </Field>
          <Field label="Altura (cm)"><input className={inputClass} inputMode="decimal" value={p.heightCm ?? ''} onChange={(e) => set('heightCm', num(e.target.value))} required /></Field>
          <Field label="Peso (kg)"><input className={inputClass} inputMode="decimal" value={p.weightKg ?? ''} onChange={(e) => set('weightKg', num(e.target.value))} required /></Field>
        </div>
      </Card>

      <Card className="space-y-4">
        <h2 className="text-2xl">Rotina de treino</h2>
        <Field label="Academia"><input className={inputClass} value={p.gym || ''} onChange={(e) => set('gym', e.target.value)} placeholder="Nome e cidade" /></Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Início do treino"><input className={inputClass} type="time" value={p.trainingStart || ''} onChange={(e) => set('trainingStart', e.target.value)} required /></Field>
          <Field label="Fim do treino"><input className={inputClass} type="time" value={p.trainingEnd || ''} onChange={(e) => set('trainingEnd', e.target.value)} required /></Field>
          <Field label="Dias por semana">
            <select className={inputClass} value={p.trainingDays} onChange={(e) => set('trainingDays', Number(e.target.value))}>
              {[3, 4, 5, 6].map((n) => <option key={n} value={n}>{n} dias</option>)}
            </select>
          </Field>
          <Field label="Meses sem treinar"><input className={inputClass} inputMode="numeric" value={p.monthsOff ?? ''} onChange={(e) => set('monthsOff', num(e.target.value))} /></Field>
        </div>
        <Field label="Seu trabalho no dia a dia">
          <select className={inputClass} value={p.jobActivity} onChange={(e) => set('jobActivity', e.target.value)}>
            <option value="SEDENTARIO">Sentado a maior parte do tempo</option>
            <option value="MODERADO">Em pé ou andando boa parte do dia</option>
            <option value="ATIVO">Esforço físico pesado</option>
          </select>
        </Field>
        <Field label="Horas de sono por noite"><input className={inputClass} inputMode="decimal" value={p.sleepHours ?? ''} onChange={(e) => set('sleepHours', num(e.target.value))} /></Field>
      </Card>

      <Card>
        <h2 className="text-2xl mb-2">Corpo e saúde</h2>
        <Check label="Sinto dor na coluna às vezes" hint="O protocolo usa só máquinas e polias com o tronco apoiado." checked={!!p.backPain} onChange={(v) => set('backPain', v)} />
        <Check label="Sinto desconforto nos joelhos" hint="O treino de pernas começa pela extensora e evita técnicas agressivas." checked={!!p.kneePain} onChange={(v) => set('kneePain', v)} />
        <Check label="Prefiro máquinas a pesos livres" checked={!!p.machinesOnly} onChange={(v) => set('machinesOnly', v)} />
        <Check label="Tenho intolerância à lactose" hint="O cardápio usa só laticínios zero lactose." checked={!!p.lactoseIntolerant} onChange={(v) => set('lactoseIntolerant', v)} />
        <div className="space-y-4 mt-3">
          <Field label="Cirurgias ou lesões" hint="Deixe em branco se não houver."><input className={inputClass} value={p.surgeries || ''} onChange={(e) => set('surgeries', e.target.value)} /></Field>
          <Field label="Medicamentos de uso contínuo" hint="Deixe em branco se não usa."><input className={inputClass} value={p.medications || ''} onChange={(e) => set('medications', e.target.value)} /></Field>
          <Field label="Condições de saúde diagnosticadas"><input className={inputClass} value={p.conditions || ''} onChange={(e) => set('conditions', e.target.value)} /></Field>
          <Field label="Alimentos que não come"><input className={inputClass} value={p.restrictions || ''} onChange={(e) => set('restrictions', e.target.value)} /></Field>
        </div>
      </Card>

      {first && (
        <Card>
          <h2 className="text-2xl mb-2">Prontidão para o exercício</h2>
          {PARQ.map((q, i) => <Check key={q} label={q} checked={parq[i]} onChange={(v) => setParq(parq.map((x, j) => (j === i ? v : x)))} />)}
          {parq.some(Boolean) && <div className="mt-2"><Notice tone="warn">Você marcou um sinal de alerta. Faça uma avaliação médica antes de começar os treinos intensos.</Notice></div>}
        </Card>
      )}

      {first && options.length > 0 && (
        <Card>
          <h2 className="text-2xl">Foco do Protocolo 1</h2>
          <p className="text-steel text-sm mt-1 mb-3">O Treino A será dedicado só a este grupo. A cada protocolo o foco muda e o sistema compara os resultados.</p>
          <div className="grid grid-cols-2 gap-2">
            {options.map((o) => (
              <button type="button" key={o.code} onClick={() => setFocus(o.code)} aria-pressed={focus === o.code}
                className={`min-h-12 rounded-xl px-3 text-left font-medium border ${focus === o.code ? 'border-amber bg-amber/10 text-amber' : 'border-line text-chalk'}`}>
                {o.label}
              </button>
            ))}
          </div>
        </Card>
      )}

      {error && <Notice tone="error">{error}</Notice>}
      {saved && !first && <Notice tone="good">Avaliação salva. A dieta já usa os novos dados.</Notice>}
      <Button type="submit" disabled={busy} className="w-full">{first ? 'Gerar meu protocolo' : 'Salvar avaliação'}</Button>
    </form>
  )
}
