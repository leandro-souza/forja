import { FormEvent, useEffect, useState } from 'react'
import { api, session } from '../api'
import { Button, Field, inputClass, Notice } from '../ui'

export default function Auth({ onDone }: { onDone: () => void }) {
  const [signupOpen, setSignupOpen] = useState<boolean | null>(null)
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    api('/auth/status').then((s) => { setSignupOpen(s.signupOpen); if (s.signupOpen) setMode('register') }).catch(() => setSignupOpen(false))
  }, [])

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const s = await api(`/auth/${mode}`, { method: 'POST', body: { email, password, name } })
      session.set(s.token, s.name)
      onDone()
    } catch (err: any) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="min-h-full max-w-md mx-auto px-5 flex flex-col justify-center py-10">
      <img src="/icon-192.png" alt="" className="size-16 rounded-2xl mb-6" />
      <h1 className="text-5xl mb-2">Forja</h1>
      <p className="text-steel mb-8">Seu treino, sua dieta e sua evolução, um protocolo melhor que o outro.</p>
      <form onSubmit={submit} className="space-y-4">
        {mode === 'register' && (
          <Field label="Como quer ser chamado">
            <input className={inputClass} value={name} onChange={(e) => setName(e.target.value)} autoComplete="name" required />
          </Field>
        )}
        <Field label="E-mail">
          <input className={inputClass} type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" required />
        </Field>
        <Field label="Senha" hint={mode === 'register' ? 'Pelo menos 8 caracteres.' : undefined}>
          <input className={inputClass} type="password" value={password} onChange={(e) => setPassword(e.target.value)} minLength={8}
            autoComplete={mode === 'register' ? 'new-password' : 'current-password'} required />
        </Field>
        {error && <Notice tone="error">{error}</Notice>}
        <Button type="submit" disabled={busy} className="w-full">{mode === 'register' ? 'Criar minha conta' : 'Entrar'}</Button>
      </form>
      {signupOpen && (
        <Button kind="ghost" className="mt-3" onClick={() => setMode(mode === 'login' ? 'register' : 'login')}>
          {mode === 'login' ? 'Criar conta' : 'Já tenho conta'}
        </Button>
      )}
    </div>
  )
}
