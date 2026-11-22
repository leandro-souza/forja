import { FormEvent, useState } from 'react'
import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { api, fmtDate, fmtNum, Measurement, today } from '../api'
import { Button, Card, Field, inputClass, Loading, LoadError, Notice, PageTitle, Sheet, useLoad } from '../ui'

const MEASURES: { key: keyof Measurement; label: string; unit: string }[] = [
  { key: 'weightKg', label: 'Peso', unit: 'kg' }, { key: 'bodyFatPct', label: 'Gordura corporal', unit: '%' },
  { key: 'chest', label: 'Peitoral', unit: 'cm' }, { key: 'shoulders', label: 'Ombros', unit: 'cm' },
  { key: 'arm', label: 'Braço contraído', unit: 'cm' }, { key: 'waist', label: 'Cintura', unit: 'cm' },
  { key: 'hip', label: 'Quadril', unit: 'cm' }, { key: 'thigh', label: 'Coxa', unit: 'cm' }, { key: 'calf', label: 'Panturrilha', unit: 'cm' },
]

const VERDICT: Record<string, { label: string; tone: string }> = {
  RESPONDEU: { label: 'Respondeu', tone: 'text-mint' }, ESTAVEL: { label: 'Estável', tone: 'text-chalk' },
  ESTAGNOU: { label: 'Estagnou', tone: 'text-flare' }, POUCOS_DADOS: { label: 'Poucos dados', tone: 'text-steel' },
}

const axis = { stroke: '#9db0ca', fontSize: 12, tickLine: false, axisLine: false }
const tooltipStyle = { background: '#223654', border: '1px solid #2e4566', borderRadius: 12, color: '#eef2f7' }

export default function Progress({ go }: { go: (tab: any) => void }) {
  const measures = useLoad<Measurement[]>(() => api('/measurements'))
  const series = useLoad<any[]>(() => api('/progress'))
  const analysis = useLoad<any>(() => api('/protocols/analysis'))
  const protocols = useLoad<any[]>(() => api('/protocols'))
  const options = useLoad<{ code: string; label: string }[]>(() => api('/protocols/focus-options'))
  const [adding, setAdding] = useState(false)
  const [form, setForm] = useState<Record<string, string>>({ date: today() })
  const [exercise, setExercise] = useState('')
  const [choosing, setChoosing] = useState(false)
  const [focus, setFocus] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  if (measures.loading && !measures.data) return <Loading />
  if (measures.error) return <LoadError message={measures.error} onRetry={measures.reload} />

  const all = measures.data || []
  const weights = all.filter((m) => m.weightKg != null).map((m) => ({ date: m.date, label: fmtDate(m.date, { day: '2-digit', month: '2-digit', year: '2-digit' }), kg: m.weightKg }))
  const latestOf = (key: keyof Measurement) => { const xs = all.filter((m) => m[key] != null); return [xs[xs.length - 1], xs[xs.length - 2]] as const }
  const selected = (series.data || []).find((s) => s.code === exercise) || (series.data || [])[0]
  const report = analysis.data
  const chosenFocus = focus || report?.suggestedFocus

  const saveMeasure = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    const body: any = { date: form.date }
    MEASURES.forEach((m) => { if (form[m.key]) body[m.key] = Number(form[m.key].replace(',', '.')) })
    try {
      await api('/measurements', { method: 'POST', body })
      setAdding(false)
      setForm({ date: today() })
      measures.reload()
      analysis.reload()
    } catch (err: any) { setError(err.message) } finally { setBusy(false) }
  }

  const generate = async () => {
    setBusy(true)
    setError(null)
    try {
      await api('/protocols/next', { method: 'POST', body: { focus: chosenFocus } })
      setChoosing(false)
      go('treino')
    } catch (err: any) { setError(err.message) } finally { setBusy(false) }
  }

  return (
    <div>
      <PageTitle title="Evolução" />
      <div className="space-y-3">
        <Card>
          <div className="flex items-baseline justify-between mb-2">
            <h2 className="text-2xl">Peso corporal</h2>
            {weights.length > 0 && <span className="num text-2xl text-amber">{fmtNum(weights[weights.length - 1].kg)} kg</span>}
          </div>
          {weights.length >= 2 ? (
            <div className="h-48 -ml-3">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={weights} margin={{ top: 8, right: 12, bottom: 0, left: 0 }}>
                  <CartesianGrid stroke="#2e4566" strokeDasharray="3 3" vertical={false} />
                  <XAxis dataKey="label" {...axis} minTickGap={28} />
                  <YAxis {...axis} domain={['dataMin - 1', 'dataMax + 1']} width={40} tickFormatter={(v) => String(Math.round(v))} />
                  <Tooltip contentStyle={tooltipStyle} formatter={(v: any) => [`${fmtNum(v)} kg`, 'Peso']} />
                  <Line type="monotone" dataKey="kg" stroke="#ffb020" strokeWidth={2.5} dot={{ r: 3, fill: '#ffb020' }} isAnimationActive={false} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          ) : <p className="text-steel text-sm">O gráfico aparece a partir da segunda pesagem.</p>}
          <Button className="w-full mt-3" onClick={() => setAdding(true)}>Registrar peso e medidas</Button>
        </Card>

        <Card>
          <h2 className="text-2xl mb-2">Medidas</h2>
          <ul className="divide-y divide-line">
            {MEASURES.map((m) => {
              const [cur, prev] = latestOf(m.key)
              const delta = cur && prev ? (cur[m.key] as number) - (prev[m.key] as number) : null
              return (
                <li key={m.key} className="py-2.5 flex items-baseline justify-between">
                  <span>{m.label}</span>
                  <span>
                    {cur ? <span className="num text-xl">{fmtNum(cur[m.key] as number)} {m.unit}</span> : <span className="text-steel text-sm">Sem registro</span>}
                    {delta != null && delta !== 0 && <span className="num text-sm text-steel ml-2">{delta > 0 ? '+' : ''}{fmtNum(delta)}</span>}
                  </span>
                </li>
              )
            })}
          </ul>
          <p className="text-xs text-steel mt-2">Meça sempre nas mesmas condições: de manhã, em jejum, antes do treino. A cintura é o melhor sinal de ganho de gordura.</p>
        </Card>

        <Card>
          <h2 className="text-2xl mb-2">Força por exercício</h2>
          {series.data && series.data.length > 0 && selected ? (
            <>
              <select className={inputClass} value={selected.code} onChange={(e) => setExercise(e.target.value)} aria-label="Exercício">
                {series.data.map((s) => <option key={s.code} value={s.code}>{s.name}</option>)}
              </select>
              <div className="h-48 -ml-3 mt-3">
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={selected.points.map((p: any) => ({ ...p, label: fmtDate(p.date, { day: '2-digit', month: '2-digit' }) }))} margin={{ top: 8, right: 12, bottom: 0, left: 0 }}>
                    <CartesianGrid stroke="#2e4566" strokeDasharray="3 3" vertical={false} />
                    <XAxis dataKey="label" {...axis} minTickGap={24} />
                    <YAxis {...axis} domain={['dataMin - 2', 'dataMax + 2']} width={40} tickFormatter={(v) => String(Math.round(v))} />
                    <Tooltip contentStyle={tooltipStyle} formatter={(v: any, _n: any, item: any) => [`${fmtNum(v)} kg (melhor série: ${fmtNum(item.payload.topWeight)} kg × ${item.payload.topReps})`, 'Força estimada']} />
                    <Line type="monotone" dataKey="e1rm" stroke="#3dd6b0" strokeWidth={2.5} dot={{ r: 3, fill: '#3dd6b0' }} isAnimationActive={false} />
                  </LineChart>
                </ResponsiveContainer>
              </div>
              <p className="text-xs text-steel mt-1">Carga máxima estimada para 1 repetição, calculada pela melhor série de cada sessão.</p>
            </>
          ) : <p className="text-steel text-sm">Registre seus treinos para ver a evolução de carga de cada exercício.</p>}
        </Card>

        {report && (
          <Card>
            <h2 className="text-2xl">Análise do protocolo atual</h2>
            <p className="text-sm mt-2">{report.focusVerdict}</p>
            <ul className="text-sm space-y-2 list-disc pl-5 mt-3 text-chalk/90">{report.insights.map((i: string) => <li key={i}>{i}</li>)}</ul>
            {report.groups.length > 0 && (
              <div className="mt-4">
                <h3 className="text-lg mb-2">Ganho de força por grupo</h3>
                <ul className="space-y-2">
                  {report.groups.map((g: any) => {
                    const max = Math.max(...report.groups.map((x: any) => Math.abs(x.gainPct)), 1)
                    return (
                      <li key={g.group} className="grid grid-cols-[8.5rem_1fr_3.5rem] items-center gap-2 text-sm">
                        <span className={g.focus ? 'text-amber font-medium' : ''}>{g.label}</span>
                        <span className="h-2.5 rounded-full bg-rail overflow-hidden"><span className={`block h-full rounded-full ${g.focus ? 'bg-amber' : 'bg-mint'}`} style={{ width: `${Math.max(2, (Math.max(0, g.gainPct) / max) * 100)}%` }} /></span>
                        <span className="num text-right">{g.gainPct > 0 ? '+' : ''}{fmtNum(g.gainPct)}%</span>
                      </li>
                    )
                  })}
                </ul>
              </div>
            )}
            {report.exercises.length > 0 && (
              <details className="mt-4">
                <summary className="text-amber text-sm font-medium cursor-pointer min-h-10 flex items-center">Ver resultado de cada exercício</summary>
                <ul className="divide-y divide-line">
                  {report.exercises.map((e: any) => (
                    <li key={e.exercise} className="py-2 flex justify-between gap-3 text-sm">
                      <span>{e.name}</span>
                      <span className={`shrink-0 ${VERDICT[e.verdict].tone}`}>{VERDICT[e.verdict].label}{e.verdict !== 'POUCOS_DADOS' ? ` ${e.gainPct > 0 ? '+' : ''}${fmtNum(e.gainPct)}%` : ''}</span>
                    </li>
                  ))}
                </ul>
              </details>
            )}
            <Button className="w-full mt-4" kind={report.adherencePct >= 80 ? 'primary' : 'quiet'} onClick={() => setChoosing(true)}>Gerar próximo protocolo</Button>
            <p className="text-xs text-steel mt-2">O ideal é concluir todas as sessões e registrar as medidas antes de gerar o próximo.</p>
          </Card>
        )}

        {protocols.data && protocols.data.length > 1 && (
          <Card>
            <h2 className="text-2xl mb-2">Histórico de protocolos</h2>
            <ul className="divide-y divide-line">
              {[...protocols.data].reverse().map((p) => (
                <li key={p.id} className="py-3">
                  <div className="flex justify-between">
                    <span className="font-medium">Protocolo {p.number}: {p.focusLabel}</span>
                    <span className="text-sm text-steel">{p.status === 'ACTIVE' ? 'Em andamento' : `${fmtDate(p.startDate)} a ${fmtDate(p.endDate)}`}</span>
                  </div>
                  {p.report && <p className="text-sm text-chalk/80 mt-1">{p.report.focusVerdict}</p>}
                </li>
              ))}
            </ul>
          </Card>
        )}
      </div>

      <Sheet open={adding} onClose={() => setAdding(false)} title="Registrar medidas">
        <form onSubmit={saveMeasure} className="space-y-4">
          <Field label="Data"><input className={inputClass} type="date" value={form.date} max={today()} onChange={(e) => setForm({ ...form, date: e.target.value })} required /></Field>
          <div className="grid grid-cols-2 gap-3">
            {MEASURES.map((m) => (
              <Field key={m.key} label={`${m.label} (${m.unit})`}>
                <input className={inputClass} inputMode="decimal" value={form[m.key] || ''} onChange={(e) => setForm({ ...form, [m.key]: e.target.value })} />
              </Field>
            ))}
          </div>
          <p className="text-sm text-steel">Preencha só o que mediu hoje. O peso atualiza o cálculo da dieta.</p>
          {error && <Notice tone="error">{error}</Notice>}
          <Button type="submit" className="w-full" disabled={busy}>Salvar medidas</Button>
        </form>
      </Sheet>

      <Sheet open={choosing} onClose={() => setChoosing(false)} title="Próximo protocolo">
        <div className="space-y-4">
          <p className="text-sm">O protocolo atual será encerrado e o novo mantém o que mais deu resultado, troca o que estagnou e ajusta a dieta. Escolha o grupo do dia dedicado.</p>
          <div className="grid grid-cols-2 gap-2">
            {(options.data || []).map((o) => (
              <button key={o.code} onClick={() => setFocus(o.code)} aria-pressed={chosenFocus === o.code}
                className={`min-h-14 rounded-xl px-3 text-left font-medium border ${chosenFocus === o.code ? 'border-amber bg-amber/10 text-amber' : 'border-line'}`}>
                {o.label}
                {report?.suggestedFocus === o.code && <span className="block text-xs font-normal text-steel">Sugerido pelo sistema</span>}
                {report?.focus === o.code && <span className="block text-xs font-normal text-steel">Foco atual</span>}
              </button>
            ))}
          </div>
          {report && report.adherencePct < 50 && <Notice tone="warn">Você fez {report.sessions} de {report.expectedSessions} sessões. Com poucos registros, o novo protocolo terá pouca base para se adaptar.</Notice>}
          {error && <Notice tone="error">{error}</Notice>}
          <Button className="w-full" disabled={busy || !chosenFocus} onClick={generate}>Encerrar o atual e gerar o novo</Button>
        </div>
      </Sheet>
    </div>
  )
}
