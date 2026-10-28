import type { Account, Trade } from '@/types'

interface Order { symbol: string; name: string; price: number; quantity: number; side: Trade['side'] }
const round = (value: number) => Math.round((value + Number.EPSILON) * 100) / 100

export function executeTrade(account: Account, order: Order): Account {
  if (!Number.isInteger(order.quantity) || order.quantity < 1 || !Number.isFinite(order.price) || order.price <= 0) throw new Error('Ingresa un número entero de acciones mayor a cero.')
  const cost = round(order.quantity * order.price)
  if (!Number.isFinite(cost)) throw new Error('El monto de la operación no es válido.')
  if (order.side === 'Compra' && cost > account.balance) throw new Error('Tu saldo es insuficiente. Puedes agregar fondos virtuales.')
  const holding = account.trades.filter(t => t.symbol === order.symbol).reduce((sum, t) => sum + (t.side === 'Compra' ? t.quantity : -t.quantity), 0)
  if (order.side === 'Venta' && order.quantity > holding) throw new Error('No tienes suficientes acciones de este activo para vender.')
  const trade: Trade = { ...order, id: crypto.randomUUID(), date: new Date().toISOString() }
  return { ...account, balance: round(account.balance + (order.side === 'Compra' ? -cost : cost)), trades: [trade, ...account.trades] }
}

export function addFunds(account: Account, amount: number): Account {
  if (!Number.isFinite(amount) || amount < 100 || amount > 1000000) throw new Error('El monto debe estar entre 100 y 1.000.000 USD.')
  return { ...account, balance: round(account.balance + amount), deposit: round(account.deposit + amount) }
}
