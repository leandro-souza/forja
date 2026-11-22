import { ReactNode, useCallback, useEffect, useState } from 'react'

export function Card({ children, className = '', onClick }: { children: ReactNode; className?: string; onClick?: () => void }) {
  return <div onClick={onClick} className={`bg-deck rounded-2xl p-4 ${onClick ? 'cursor-pointer active:bg-rail' : ''} ${className}`}>{children}</div>
}

type ButtonProps = { children: ReactNode; onClick?: () => void; kind?: 'primary' | 'quiet' | 'ghost' | 'danger'; disabled?: boolean; className?: string; type?: 'button' | 'submit' }

export function Button({ children, onClick, kind = 'primary', disabled, className = '', type = 'button' }: ButtonProps) {
  const styles = {
    primary: 'bg-amber text-ink font-semibold active:brightness-90',
    quiet: 'bg-rail text-chalk font-medium active:bg-line',
    ghost: 'text-steel font-medium active:text-chalk',
    danger: 'bg-flare/15 text-flare font-medium active:bg-flare/25',
  }[kind]
  return (
    <button type={type} onClick={onClick} disabled={disabled} className={`min-h-12 px-5 rounded-xl text-base transition-colors disabled:opacity-40 ${styles} ${className}`}>
      {children}
    </button>
  )
}

export function Field({ label, children, hint }: { label: string; children: ReactNode; hint?: string }) {
  return (
    <label className="block">
      <span className="block text-sm text-steel mb-1.5">{label}</span>
      {children}
      {hint && <span className="block text-xs text-steel mt-1">{hint}</span>}
    </label>
  )
}

export const inputClass = 'w-full min-h-12 px-3.5 rounded-xl bg-ink border border-line text-chalk placeholder:text-steel/60'

export function Check({ label, checked, onChange, hint }: { label: string; checked: boolean; onChange: (v: boolean) => void; hint?: string }) {
  return (
    <label className="flex items-start gap-3 py-2 cursor-pointer">
      <input type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)} className="mt-0.5 size-6 rounded accent-amber shrink-0" />
      <span>
        <span className="block">{label}</span>
        {hint && <span className="block text-sm text-steel">{hint}</span>}
      </span>
    </label>
  )
}

export function Sheet({ open, onClose, title, children }: { open: boolean; onClose: () => void; title: string; children: ReactNode }) {
  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose()
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])
  if (!open) return null
  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-black/60" onClick={onClose}>
      <div role="dialog" aria-label={title} onClick={(e) => e.stopPropagation()}
        className="sheet-in w-full max-w-xl max-h-[88vh] overflow-y-auto bg-deck rounded-t-3xl p-5 safe-bottom">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-2xl">{title}</h2>
          <button onClick={onClose} aria-label="Fechar" className="size-10 rounded-full bg-rail text-steel text-xl leading-none">×</button>
        </div>
        {children}
      </div>
    </div>
  )
}

export function Meter({ value, max, tone = 'amber' }: { value: number; max: number; tone?: 'amber' | 'mint' | 'steel' }) {
  const pct = max > 0 ? Math.min(100, Math.round((value / max) * 100)) : 0
  const color = { amber: 'bg-amber', mint: 'bg-mint', steel: 'bg-steel' }[tone]
  return (
    <div className="h-2 rounded-full bg-rail overflow-hidden" role="progressbar" aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100}>
      <div className={`h-full rounded-full ${color}`} style={{ width: `${pct}%` }} />
    </div>
  )
}

export function Segmented<T extends string>({ options, value, onChange }: { options: { value: T; label: string }[]; value: T; onChange: (v: T) => void }) {
  return (
    <div className="flex bg-deck rounded-xl p-1 gap-1">
      {options.map((o) => (
        <button key={o.value} onClick={() => onChange(o.value)} aria-pressed={o.value === value}
          className={`flex-1 min-h-10 rounded-lg text-sm font-medium ${o.value === value ? 'bg-rail text-chalk' : 'text-steel'}`}>
          {o.label}
        </button>
      ))}
    </div>
  )
}

export function Notice({ children, tone = 'info' }: { children: ReactNode; tone?: 'info' | 'warn' | 'error' | 'good' }) {
  const styles = { info: 'bg-rail text-chalk', warn: 'bg-amber/15 text-amber', error: 'bg-flare/15 text-flare', good: 'bg-mint/15 text-mint' }[tone]
  return <div className={`rounded-xl px-4 py-3 text-sm ${styles}`}>{children}</div>
}

export function Loading({ label = 'Carregando' }: { label?: string }) {
  return <div className="py-16 text-center text-steel">{label}…</div>
}

/** Carrega dados da API e expoe estado de carregamento, erro e recarga. */
export function useLoad<T>(loader: () => Promise<T>, deps: unknown[] = []) {
  const [data, setData] = useState<T | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const reload = useCallback(() => {
    setLoading(true)
    setError(null)
    return loader().then(setData).catch((e) => setError(e.message)).finally(() => setLoading(false))
  }, deps)
  useEffect(() => { reload() }, [reload])
  return { data, error, loading, reload, setData }
}

export function LoadError({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div className="py-10 text-center">
      <p className="text-flare mb-4">{message}</p>
      <Button kind="quiet" onClick={onRetry}>Tentar de novo</Button>
    </div>
  )
}

export function PageTitle({ title, sub }: { title: string; sub?: string }) {
  return (
    <header className="pt-5 pb-4">
      <h1 className="text-4xl">{title}</h1>
      {sub && <p className="text-steel mt-1">{sub}</p>}
    </header>
  )
}
