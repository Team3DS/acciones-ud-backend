import type { PaperOrder, PaperQuote, PaperResult, PaperSnapshot } from '@/types/paper'
import type { ActivityPage } from '@/types/reports'

export class PaperError extends Error {
  code: string
  constructor(message: string, code: string) { super(message); this.code = code }
}
async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  let response: Response
  try { response = await fetch(`/api/paper/${path}`, { ...options, headers: { 'Content-Type': 'application/json', ...options.headers }, signal: AbortSignal.timeout(5500) }) }
  catch { throw new PaperError('La conexión tardó demasiado. Revisa el estado de la orden antes de volver a operar.', 'NETWORK_TIMEOUT') }
  let data
  try { data = await response.json() } catch { throw new PaperError('La API no está disponible. Usa npm run dev o npm start para iniciar el servidor.', 'INVALID_RESPONSE') }
  if (!response.ok) throw new PaperError(data.message || 'No se pudo consultar Alpaca.', data.code || 'API_ERROR')
  return data as T
}
export const paperApi = {
  status: () => request<{ configured: boolean; mode: 'paper'; feed: string }>('status'),
  snapshot: () => request<PaperSnapshot>('snapshot'),
  activities: (pageToken = '') => request<ActivityPage>(`activities?pageToken=${encodeURIComponent(pageToken)}`),
  quote: (symbol: string) => request<PaperQuote>(`quote?symbol=${encodeURIComponent(symbol)}`),
  order: (clientOrderId: string) => request<{ order: PaperOrder }>(`order?clientOrderId=${encodeURIComponent(clientOrderId)}`),
  submit: (input: { symbol: string; quantity: number; side: 'buy' | 'sell'; clientOrderId: string; type?: 'market' | 'limit'; limitPrice?: number; timeInForce?: 'day' | 'gtc'; queueConfirmed?: boolean }) => request<PaperResult>('orders', { method: 'POST', body: JSON.stringify(input) }),
  cancel: (clientOrderId: string) => request<{ requested: boolean; message: string }>('cancel', { method: 'POST', body: JSON.stringify({ clientOrderId }) }),
}
export const paperAssets = [
  { symbol: 'JNJ', name: 'Johnson & Johnson', exchange: 'NYSE' }, { symbol: 'JPM', name: 'JPMorgan Chase', exchange: 'NYSE' }, { symbol: 'XOM', name: 'Exxon Mobil', exchange: 'NYSE' },
  { symbol: 'AAPL', name: 'Apple', exchange: 'NASDAQ' }, { symbol: 'MSFT', name: 'Microsoft', exchange: 'NASDAQ' }, { symbol: 'GOOGL', name: 'Alphabet', exchange: 'NASDAQ' }, { symbol: 'AMZN', name: 'Amazon', exchange: 'NASDAQ' }, { symbol: 'META', name: 'Meta Platforms', exchange: 'NASDAQ' }, { symbol: 'TSLA', name: 'Tesla', exchange: 'NASDAQ' }, { symbol: 'NVDA', name: 'NVIDIA', exchange: 'NASDAQ' },
]
export const terminalOrder = (status: string) => ['filled', 'rejected', 'canceled', 'expired', 'replaced', 'done_for_day'].includes(status)
export function orderStatus(status: string) { return ({ new: 'Recibida', accepted: 'Aceptada', pending_new: 'Enviando al mercado', accepted_for_bidding: 'Aceptada', partially_filled: 'Ejecución parcial', filled: 'Ejecutada', rejected: 'Rechazada', canceled: 'Cancelada', expired: 'Expirada', pending_cancel: 'Cancelación pendiente', done_for_day: 'Finalizada por hoy', replaced: 'Reemplazada', suspended: 'Suspendida', stopped: 'Detenida', calculated: 'En liquidación' } as Record<string, string>)[status] || 'En seguimiento' }
