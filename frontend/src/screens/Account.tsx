import { useRef, useState } from 'react'
import { api, Profile, today } from '../api'
import { Button, Card, Notice, PageTitle } from '../ui'
import ProfileForm from './ProfileForm'

export default function Account({ profile, onChanged, onLogout }: { profile: Profile | null; onChanged: () => void; onLogout: () => void }) {
  const [message, setMessage] = useState<{ tone: 'good' | 'error'; text: string } | null>(null)
  const file = useRef<HTMLInputElement>(null)

  const exportData = async () => {
    try {
      const data = await api('/backup')
      const url = URL.createObjectURL(new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' }))
      const a = document.createElement('a')
      a.href = url
      a.download = `forja-backup-${today()}.json`
      a.click()
      URL.revokeObjectURL(url)
      setMessage({ tone: 'good', text: 'Cópia de segurança baixada.' })
    } catch (e: any) { setMessage({ tone: 'error', text: e.message }) }
  }

  const importData = async (f: File) => {
    try {
      const data = JSON.parse(await f.text())
      if (!window.confirm('Restaurar substitui todos os dados atuais pelos da cópia. Continuar?')) return
      const r = await api('/backup/restore', { method: 'POST', body: data })
      setMessage({ tone: 'good', text: `Cópia restaurada: ${r.sessions} sessões e ${r.sets} séries.` })
      onChanged()
    } catch (e: any) { setMessage({ tone: 'error', text: e.message || 'Arquivo de cópia inválido.' }) }
  }

  return (
    <div>
      <PageTitle title="Perfil" sub="Mantenha a avaliação em dia: ela alimenta o treino e a dieta." />
      <div className="space-y-4">
        <ProfileForm initial={profile} onSaved={onChanged} />

        <Card>
          <h2 className="text-2xl mb-2">Instalar no celular</h2>
          <p className="text-sm text-chalk/90">No Android (Chrome): toque nos três pontos e em "Adicionar à tela inicial" ou "Instalar app". No iPhone (Safari): toque em Compartilhar e em "Adicionar à Tela de Início". O Forja passa a abrir em tela cheia, como um aplicativo.</p>
        </Card>

        <Card>
          <h2 className="text-2xl mb-2">Cópia de segurança</h2>
          <p className="text-sm text-chalk/90 mb-3">Baixe um arquivo com todos os seus dados: avaliação, medidas, protocolos e treinos. Faça isso ao fim de cada protocolo.</p>
          <div className="flex gap-2">
            <Button kind="quiet" className="flex-1" onClick={exportData}>Baixar cópia</Button>
            <Button kind="quiet" className="flex-1" onClick={() => file.current?.click()}>Restaurar cópia</Button>
            <input ref={file} type="file" accept="application/json" className="hidden" onChange={(e) => { const f = e.target.files?.[0]; if (f) importData(f); e.target.value = '' }} />
          </div>
          {message && <div className="mt-3"><Notice tone={message.tone}>{message.text}</Notice></div>}
        </Card>

        <Button kind="danger" className="w-full" onClick={onLogout}>Sair da conta</Button>
      </div>
    </div>
  )
}
