// Cliente da API do Forja e tipos compartilhados entre as telas.

export interface Exercise { code: string; name: string; group: string; groupLabel: string; img: string; compound: boolean; cue: string }
export interface SetRx { n: number; reps: string; repLow: number; repHigh: number; technique: string | null; suggestedKg: number | null }
export interface Rx { sets: SetRx[]; cadence: string; restSec: number; rir: number }
export interface Item { exercise: Exercise; technique: string | null; rx: Rx; last: { weight: number; reps: number }[]; alternatives: Exercise[] }
export interface Workout { code: string; title: string; warmup: string[]; cardio: string | null; totalSets: number; lastDone: string | null; items: Item[] }
export interface PhaseView { code: string; label: string; guidance: string; rir: number }
export interface ProtocolView {
  id: number; number: number; focus: string; focusLabel: string; startDate: string; weeks: number; week: number; phase: PhaseView
  phases: PhaseView[]; sessionsDone: number; sessionsTotal: number; nextWorkout: string; finished: boolean; rationale: string[]
}
export interface TrainView { protocol: ProtocolView; workouts: Workout[] }
export interface Measurement {
  id?: number; date: string; weightKg?: number | null; bodyFatPct?: number | null; chest?: number | null; shoulders?: number | null
  arm?: number | null; waist?: number | null; hip?: number | null; thigh?: number | null; calf?: number | null; notes?: string | null
}
export interface Profile {
  name?: string; birthDate?: string | null; sex?: string; heightCm?: number | null; weightKg?: number | null; goal?: string; trainingDays?: number
  trainingStart?: string; trainingEnd?: string; jobActivity?: string; monthsOff?: number | null; backPain?: boolean; kneePain?: boolean
  machinesOnly?: boolean; lactoseIntolerant?: boolean; restrictions?: string; surgeries?: string; medications?: string; conditions?: string
  sleepHours?: number | null; gym?: string; parqAlert?: boolean; notes?: string; kcalAdjust?: number
}

const TOKEN_KEY = 'forja.token'
const NAME_KEY = 'forja.name'

function read(key: string): string | null {
  try { return localStorage.getItem(key) } catch { return null }
}

export const session = {
  token: () => read(TOKEN_KEY),
  name: () => read(NAME_KEY) || '',
  set(token: string, name: string) {
    try { localStorage.setItem(TOKEN_KEY, token); localStorage.setItem(NAME_KEY, name) } catch { /* armazenamento indisponivel */ }
  },
  clear() {
    try { localStorage.removeItem(TOKEN_KEY); localStorage.removeItem(NAME_KEY) } catch { /* nada a limpar */ }
  },
}

export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) { super(message); this.status = status }
}

export async function api<T = any>(path: string, options: { method?: string; body?: unknown } = {}): Promise<T> {
  const headers: Record<string, string> = {}
  const token = session.token()
  if (token) headers.Authorization = `Bearer ${token}`
  if (options.body !== undefined) headers['Content-Type'] = 'application/json'
  let res: Response
  try {
    res = await fetch(`/api${path}`, { method: options.method || 'GET', headers, body: options.body !== undefined ? JSON.stringify(options.body) : undefined })
  } catch {
    throw new ApiError(0, 'Sem conexão com o servidor. Confira a internet e tente de novo.')
  }
  const text = await res.text()
  let data: any = null
  try { data = text ? JSON.parse(text) : null } catch { /* resposta sem JSON */ }
  if (!res.ok) {
    if (res.status === 401 && !path.startsWith('/auth/')) { session.clear(); window.dispatchEvent(new Event('forja:logout')) }
    throw new ApiError(res.status, data?.message || 'O servidor não respondeu como esperado. Tente novamente em instantes.')
  }
  return data as T
}

export const today = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

export const addDays = (iso: string, n: number) => {
  const d = new Date(`${iso}T12:00:00`)
  d.setDate(d.getDate() + n)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

export const fmtDate = (iso: string, opts: Intl.DateTimeFormatOptions = { day: '2-digit', month: 'short' }) =>
  new Date(`${iso}T12:00:00`).toLocaleDateString('pt-BR', opts).replace('.', '')

export const fmtNum = (n: number | null | undefined, digits = 1) =>
  n == null ? '' : n.toLocaleString('pt-BR', { maximumFractionDigits: digits })

export const TECHNIQUES: Record<string, { label: string; how: string }> = {
  REST_PAUSE: { label: 'Rest-pause', how: 'Ao terminar a série, descanse 20 segundos e faça mais repetições com a mesma carga até quase falhar. Repita mais uma vez.' },
  DROP_SET: { label: 'Drop-set', how: 'Ao terminar a série, reduza a carga em 30% e continue sem descanso. Reduza mais 20% e vá até quase falhar.' },
}
