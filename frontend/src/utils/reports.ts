import type { Trade } from '@/types'
import type { PaperActivity, ReportData, ReportFee, ReportTransaction } from '@/types/reports'
import { tradeAmounts } from './commission.mjs'

export const REPORT_TIMEZONE = 'America/Bogota'
const SCALE = 1000000n
// Los cálculos usan enteros en millonésimas de USD, sin sumar valores binarios de coma flotante.
function decimal(value: string) {
  if (!/^-?\d+(\.\d{1,18})?$/.test(value) || value.length > 50) throw new Error('Importe decimal inválido en el historial.')
  const [whole, fraction = ''] = value.replace('-', '').split('.')
  return { value: BigInt(whole + fraction) * (value.startsWith('-') ? -1n : 1n), scale: 10n ** BigInt(fraction.length) }
}
function divideRounded(value: bigint, divisor: bigint) { const sign = value < 0n ? -1n : 1n; return sign * (((value < 0n ? -value : value) + divisor / 2n) / divisor) }
export function usdMicros(value: string) { const parsed = decimal(value); return divideRounded(parsed.value * SCALE, parsed.scale) }
export function productMicros(price: string, quantity: string) { const a = decimal(price); const b = decimal(quantity); return divideRounded(a.value * b.value * SCALE, a.scale * b.scale) }
export function decimalUSD(value: bigint, digits = 6) {
  const negative = value < 0n; const rounded = divideRounded(negative ? -value : value, 10n ** BigInt(6 - digits)); const unit = 10n ** BigInt(digits)
  return `${negative && rounded !== 0n ? '-' : ''}${rounded / unit}${digits ? `.${(rounded % unit).toString().padStart(digits, '0')}` : ''}`
}
export function formatUSD(value: bigint) { return new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'USD', minimumFractionDigits: 2, maximumFractionDigits: 6 }).format(Number(decimalUSD(value))) }
export function dateInBogota(value: string | Date) {
  const date = new Date(value)
  if (!Number.isFinite(date.getTime())) throw new Error('Fecha inválida en el historial.')
  const parts = new Intl.DateTimeFormat('en-CA', { timeZone: REPORT_TIMEZONE, year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(date)
  return `${parts.find(p => p.type === 'year')!.value}-${parts.find(p => p.type === 'month')!.value}-${parts.find(p => p.type === 'day')!.value}`
}
export function validDate(value: string) { return /^\d{4}-\d{2}-\d{2}$/.test(value) && Number.isFinite(Date.parse(`${value}T12:00:00Z`)) && new Date(`${value}T12:00:00Z`).toISOString().slice(0, 10) === value }
export function validatePeriod(start: string, end: string) {
  if (!validDate(start) || !validDate(end)) return 'Selecciona fechas válidas de inicio y fin.'
  if (start > end) return 'La fecha inicial no puede ser posterior a la fecha final.'
  return ''
}
export function presetPeriod(preset: string, now = new Date()) {
  const today = dateInBogota(now)
  const date = new Date(`${today}T12:00:00Z`)
  if (preset === 'hoy') return { start: today, end: today }
  if (preset === '7dias') { date.setUTCDate(date.getUTCDate() - 6); return { start: date.toISOString().slice(0, 10), end: today } }
  if (preset === 'anterior') { const end = new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), 0)); return { start: `${end.toISOString().slice(0, 7)}-01`, end: end.toISOString().slice(0, 10) } }
  if (preset === 'anio') return { start: `${today.slice(0, 4)}-01-01`, end: today }
  return { start: `${today.slice(0, 7)}-01`, end: today }
}
export function localReport(trades: Trade[]): ReportData {
  return { transactions: trades.map(t => ({ id: t.id, orderId: t.id, symbol: t.symbol, date: dateInBogota(t.date), timestamp: t.date, side: t.side, quantity: String(t.quantity), price: String(t.price), grossMicros: productMicros(String(t.price), String(t.quantity)) })), fees: [], excluded: 0 }
}
export function activityReport(activities: PaperActivity[]): ReportData {
  const transactions: ReportTransaction[] = []; const fees: ReportFee[] = []; const seen = new Set<string>(); let excluded = 0
  for (const a of activities) {
    if (seen.has(a.id)) continue
    seen.add(a.id)
    if (a.currency && a.currency !== 'USD') { excluded++; continue }
    if (a.activity_type === 'FILL') {
      if (!a.transaction_time || !a.symbol || !['buy', 'sell'].includes(a.side || '') || !a.price || !a.qty) throw new Error('Una ejecución de Alpaca no tiene todos sus campos. No se generará un reporte incompleto.')
      const grossMicros = productMicros(a.price, a.qty)
      if (grossMicros < 0n || decimal(a.qty).value <= 0n) throw new Error('Alpaca devolvió una cantidad o un precio inválido.')
      transactions.push({ id: a.id, orderId: a.order_id || '', symbol: a.symbol, date: dateInBogota(a.transaction_time), timestamp: a.transaction_time, side: a.side === 'buy' ? 'Compra' : 'Venta', quantity: a.qty, price: a.price, grossMicros })
    } else if (['FEE', 'DIVFEE', 'PTC', 'PTR'].includes(a.activity_type)) {
      const date = a.date?.slice(0, 10)
      if (!date || !validDate(date) || a.net_amount === undefined) throw new Error('Un cargo de Alpaca no tiene fecha o importe. No se generará un reporte incompleto.')
      fees.push({ id: a.id, orderId: a.order_id || '', symbol: a.symbol || '', date, type: a.activity_type, description: a.description || feeLabel(a.activity_type), netMicros: usdMicros(a.net_amount) })
    }
  }
  return { transactions, fees, excluded }
}
export function feeLabel(type: string) { return ({ FEE: 'Comisiones y cargos en USD', DIVFEE: 'Cargos por dividendos', PTC: 'Cargos trasladados', PTR: 'Reembolsos de cargos' } as Record<string, string>)[type] || type }
export function filterReport(data: ReportData, start: string, end: string): ReportData {
  if (validatePeriod(start, end)) return { transactions: [], fees: [], excluded: data.excluded }
  return { transactions: data.transactions.filter(t => t.date >= start && t.date <= end).sort((a, b) => b.timestamp.localeCompare(a.timestamp)), fees: data.fees.filter(f => f.date >= start && f.date <= end).sort((a, b) => b.date.localeCompare(a.date)), excluded: data.excluded }
}
export function summarize(data: ReportData) {
  const purchases = data.transactions.filter(t => t.side === 'Compra').reduce((sum, t) => sum + t.grossMicros, 0n)
  const sales = data.transactions.filter(t => t.side === 'Venta').reduce((sum, t) => sum + t.grossMicros, 0n)
  const charges = data.fees.filter(f => f.netMicros < 0n).reduce((sum, f) => sum - f.netMicros, 0n)
  const refunds = data.fees.filter(f => f.netMicros > 0n).reduce((sum, f) => sum + f.netMicros, 0n)
  return { count: data.transactions.length, purchases, sales, volume: purchases + sales, charges, refunds, netFees: charges - refunds, netCash: sales - purchases - charges + refunds }
}
export function groupReport(data: ReportData, grouping: 'day' | 'month') {
  const groups = new Map<string, ReportData>()
  const group = (date: string) => { const key = grouping === 'month' ? date.slice(0, 7) : date; if (!groups.has(key)) groups.set(key, { transactions: [], fees: [], excluded: 0 }); return groups.get(key)! }
  data.transactions.forEach(t => group(t.date).transactions.push(t)); data.fees.forEach(f => group(f.date).fees.push(f))
  return [...groups].sort(([a], [b]) => b.localeCompare(a)).map(([period, rows]) => ({ period, ...summarize(rows) }))
}
export function csvText(rows: (string | number)[][]) {
  const cell = (value: string | number) => {
    let text = String(value)
    // Neutraliza fórmulas al abrir campos de texto en una hoja de cálculo.
    if (typeof value === 'string' && /^[\s\u0000-\u001f]*[=+@-]/.test(text) && !/^-?\d+(\.\d+)?$/.test(text)) text = `'${text}`
    return `"${text.replaceAll('"', '""')}"`
  }
  return '\ufeff' + rows.map(row => row.map(cell).join(';')).join('\r\n')
}
export function reportCsv(data: ReportData, start: string, end: string, source: string, kind: 'transactions' | 'fees' | 'summary', grouping: 'day' | 'month') {
  const metadata = [source, start, end, 'USD', REPORT_TIMEZONE]
  if (kind === 'transactions') return csvText([['Origen', 'Desde', 'Hasta', 'Moneda', 'Zona horaria', 'ID ejecución', 'ID orden', 'Fecha Bogotá', 'Fecha ejecución ISO', 'Activo', 'Operación', 'Cantidad', 'Precio USD', 'Importe bruto USD'], ...data.transactions.map(t => [...metadata, t.id, t.orderId, t.date, t.timestamp, t.symbol, t.side, t.quantity, t.price, decimalUSD(t.grossMicros)])])
  if (kind === 'fees') return csvText([['Origen', 'Desde', 'Hasta', 'Moneda', 'Zona horaria', 'ID cargo', 'ID orden', 'Fecha contable proveedor', 'Activo', 'Tipo', 'Descripción', 'Cargo USD', 'Reembolso USD', 'Impacto efectivo USD'], ...data.fees.map(f => [...metadata, f.id, f.orderId, f.date, f.symbol, f.type, f.description, decimalUSD(f.netMicros < 0n ? -f.netMicros : 0n), decimalUSD(f.netMicros > 0n ? f.netMicros : 0n), decimalUSD(f.netMicros)])])
  return csvText([['Origen', 'Desde', 'Hasta', 'Moneda', 'Zona horaria', 'Período', 'Ejecuciones', 'Compras USD', 'Ventas USD', 'Volumen USD', 'Cargos USD', 'Reembolsos USD', 'Cargos netos USD', 'Flujo neto USD'], ...groupReport(data, grouping).map(g => [...metadata, g.period, g.count, decimalUSD(g.purchases), decimalUSD(g.sales), decimalUSD(g.volume), decimalUSD(g.charges), decimalUSD(g.refunds), decimalUSD(g.netFees), decimalUSD(g.netCash)])])
}

export function platformCommissions(data: ReportData) {
  return data.transactions.map(t => ({ ...t, ...tradeAmounts(decimalUSD(t.grossMicros), 1, 200, t.side === 'Compra' ? 'buy' : 'sell') }))
}
export function commissionCsv(data: ReportData, start: string, end: string, source: string, user: string) {
  return csvText([['Origen','Usuario de prototipo','Desde','Hasta','ID ejecución','ID orden','Fecha Bogotá','Activo','Operación','Valor bruto USD','Tasa','Comisión estimada USD','Total compra / neto venta USD','Estado de cobro'], ...platformCommissions(data).map(t=>[source,user,start,end,t.id,t.orderId,t.date,t.symbol,t.side,t.gross.toFixed(2),'2 %',t.commission.toFixed(2),t.total.toFixed(2),'Estimación del prototipo; no cobrada'])])
}
