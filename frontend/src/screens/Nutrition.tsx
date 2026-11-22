import { useState } from 'react'
import { addDays, api, fmtDate, today } from '../api'
import { Card, Loading, LoadError, Meter, Notice, PageTitle, Segmented, useLoad } from '../ui'

type View = 'cardapio' | 'suplementos' | 'compras'

const LEVEL_TONE: Record<string, string> = { Essencial: 'bg-amber/15 text-amber', Recomendado: 'bg-mint/15 text-mint' }

export default function Nutrition() {
  const [view, setView] = useState<View>('cardapio')
  const [date, setDate] = useState(today())
  const menu = useLoad<any>(() => api(`/nutrition/menu?date=${date}`), [date])
  const plan = useLoad<any>(() => api('/nutrition/plan'))
  const shopping = useLoad<any[]>(() => api(`/nutrition/shopping?date=${today()}`))
  const days = Array.from({ length: 7 }, (_, i) => addDays(today(), i))

  return (
    <div>
      <PageTitle title="Nutrição" />
      <Segmented value={view} onChange={setView} options={[{ value: 'cardapio', label: 'Cardápio' }, { value: 'suplementos', label: 'Suplementos' }, { value: 'compras', label: 'Compras' }]} />

      {view === 'cardapio' && (
        <div className="space-y-3 mt-3">
          <div className="flex gap-2 overflow-x-auto -mx-4 px-4 pb-1" role="tablist" aria-label="Dia do cardápio">
            {days.map((d) => (
              <button key={d} role="tab" aria-selected={d === date} onClick={() => setDate(d)}
                className={`shrink-0 w-14 py-2 rounded-xl text-center border ${d === date ? 'border-amber bg-amber/10 text-amber' : 'border-line text-steel'}`}>
                <span className="block text-xs">{d === today() ? 'Hoje' : fmtDate(d, { weekday: 'short' })}</span>
                <span className="num text-xl">{d.slice(8)}</span>
              </button>
            ))}
          </div>
          {menu.loading && !menu.data && <Loading />}
          {menu.error && <LoadError message={menu.error} onRetry={menu.reload} />}
          {menu.data && (
            <>
              <Card>
                <div className="flex items-baseline justify-between">
                  <h2 className="text-2xl">Total do dia</h2>
                  <span><span className="num text-2xl text-amber">{menu.data.kcal.toLocaleString('pt-BR')}</span><span className="text-steel text-sm"> de {menu.data.targets.kcal.toLocaleString('pt-BR')} kcal</span></span>
                </div>
                <div className="space-y-2.5 mt-3">
                  {[['Proteína', menu.data.p, menu.data.targets.protein, 'amber'], ['Carboidrato', menu.data.c, menu.data.targets.carbs, 'mint'], ['Gordura', menu.data.f, menu.data.targets.fat, 'steel']].map(([l, v, t, tone]) => (
                    <div key={l as string}>
                      <div className="flex justify-between text-sm mb-1"><span>{l}</span><span className="num">{v} / {t} g</span></div>
                      <Meter value={v as number} max={t as number} tone={tone as any} />
                    </div>
                  ))}
                </div>
                <p className="text-sm text-steel mt-3">Água: {String(menu.data.targets.waterL).replace('.', ',')} litros ao longo do dia.</p>
              </Card>
              {menu.data.meals.map((m: any) => (
                <Card key={m.name}>
                  <div className="flex items-baseline gap-3">
                    <span className="num text-2xl text-amber w-14 shrink-0">{m.time}</span>
                    <div className="flex-1 min-w-0">
                      <h3 className="text-xl">{m.name}</h3>
                      <p className="text-sm text-steel">{m.option}</p>
                    </div>
                    <span className="num text-steel shrink-0">{m.kcal} kcal</span>
                  </div>
                  <ul className="mt-3 divide-y divide-line">
                    {m.items.map((it: any) => (
                      <li key={it.food} className="py-2 flex justify-between gap-3 text-sm">
                        <span>{it.name}</span><span className="text-steel text-right shrink-0 max-w-[55%]">{it.qty}</span>
                      </li>
                    ))}
                  </ul>
                  <p className="text-xs text-steel mt-2">Proteína {m.p} g, carboidrato {m.c} g, gordura {m.f} g</p>
                  {m.note && <p className="text-sm text-chalk/80 mt-2">{m.note}</p>}
                </Card>
              ))}
              <Notice>Pesos de alimentos já cozidos ou prontos. Pode trocar entre si: arroz, cuscuz, macaxeira, inhame e batata-doce; frango, peixe, carne magra e ovos. Tempere à vontade com coentro, cebola, alho e limão.</Notice>
            </>
          )}
        </div>
      )}

      {view === 'suplementos' && (
        <div className="space-y-3 mt-3">
          {plan.loading && !plan.data && <Loading />}
          {plan.error && <LoadError message={plan.error} onRetry={plan.reload} />}
          {plan.data?.supplements.map((s: any) => (
            <Card key={s.name}>
              <div className="flex items-start justify-between gap-3">
                <h3 className="text-xl">{s.name}</h3>
                <span className={`shrink-0 rounded-full px-2.5 py-0.5 text-xs font-medium ${LEVEL_TONE[s.level] || 'bg-rail text-steel'}`}>{s.level}</span>
              </div>
              <p className="num text-2xl text-amber mt-1">{s.dose}</p>
              <p className="text-sm mt-2"><span className="text-steel">Quando: </span>{s.when}</p>
              <p className="text-sm text-chalk/80 mt-1.5">{s.why}</p>
            </Card>
          ))}
          {plan.data && (
            <Card>
              <h3 className="text-xl mb-2">Como a dieta foi calculada</h3>
              <p className="text-sm text-steel mb-2">Gasto em repouso de {plan.data.targets.bmr} kcal, manutenção estimada em {plan.data.targets.maintenance} kcal.</p>
              <ul className="text-sm space-y-2 list-disc pl-5 text-chalk/90">{plan.data.targets.notes.map((n: string) => <li key={n}>{n}</li>)}</ul>
            </Card>
          )}
          <Notice tone="warn">Suplemento complementa a dieta, não substitui. Se você iniciar algum medicamento ou tiver alteração em exames de rim ou fígado, converse com seu médico antes de continuar.</Notice>
        </div>
      )}

      {view === 'compras' && (
        <div className="space-y-3 mt-3">
          {shopping.loading && !shopping.data && <Loading />}
          {shopping.error && <LoadError message={shopping.error} onRetry={shopping.reload} />}
          {shopping.data && (
            <>
              <p className="text-steel text-sm">Quantidades para os próximos 7 dias de cardápio, de {fmtDate(today())} a {fmtDate(addDays(today(), 6))}.</p>
              {[...new Set(shopping.data.map((i) => i.section))].map((section) => (
                <Card key={section}>
                  <h3 className="text-xl mb-1">{section}</h3>
                  <ul className="divide-y divide-line">
                    {shopping.data!.filter((i) => i.section === section).map((i) => <ShoppingRow key={i.name} name={i.name} qty={i.qty} />)}
                  </ul>
                </Card>
              ))}
            </>
          )}
        </div>
      )}
    </div>
  )
}

function ShoppingRow({ name, qty }: { name: string; qty: string }) {
  const [got, setGot] = useState(false)
  return (
    <li>
      <label className="py-2.5 flex items-start gap-3 cursor-pointer">
        <input type="checkbox" checked={got} onChange={(e) => setGot(e.target.checked)} className="mt-0.5 size-5 accent-amber shrink-0" />
        <span className={`flex-1 text-sm ${got ? 'line-through text-steel' : ''}`}>{name}</span>
        <span className="text-sm text-steel text-right max-w-[50%]">{qty}</span>
      </label>
    </li>
  )
}
