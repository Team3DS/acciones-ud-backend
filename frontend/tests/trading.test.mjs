import test from 'node:test'
import assert from 'node:assert/strict'
import { executeTrade, addFunds } from '../src/services/trading.ts'

const initial = () => ({ profile: { name: 'Prueba', surname: 'Local', email: 'prueba@example.com', country: 'Colombia' }, step: 'dashboard', balance: 1000, deposit: 1000, trades: [], watchlist: [], accountId: 'ST-TEST' })
const order = { symbol: 'AAPL', name: 'Apple Inc.', price: 227.63, quantity: 3, side: 'Compra' }

test('Compra y venta completas conservan el capital y el historial sin mutar el estado anterior', () => {
  const start = initial()
  const bought = executeTrade(start, order)
  assert.equal(bought.balance, 317.11)
  assert.equal(bought.balance + 3 * 227.63, 1000)
  const sold = executeTrade(bought, { ...order, side: 'Venta' })
  assert.equal(sold.balance, 1000)
  assert.equal(sold.trades.length, 2)
  assert.notEqual(sold.trades[0].id, sold.trades[1].id)
  assert.equal(start.balance, 1000)
  assert.equal(start.trades.length, 0)
})

test('Impide compras sin fondos y ventas sin posiciones, incluso tras una venta parcial', () => {
  assert.throws(() => executeTrade(initial(), { ...order, quantity: 5 }), /saldo es insuficiente/)
  assert.throws(() => executeTrade(initial(), { ...order, side: 'Venta' }), /suficientes acciones/)
  const bought = executeTrade(initial(), order)
  const partial = executeTrade(bought, { ...order, side: 'Venta', quantity: 2 })
  assert.throws(() => executeTrade(partial, { ...order, side: 'Venta', quantity: 2 }), /suficientes acciones/)
})

test('Rechaza cantidades inválidas y mantiene precisión de centavos', () => {
  for (const quantity of [0, -1, 1.5, NaN, Infinity]) assert.throws(() => executeTrade(initial(), { ...order, quantity }))
  const bought = executeTrade(initial(), { ...order, price: 0.1, quantity: 3 })
  assert.equal(bought.balance, 999.7)
  assert.equal(executeTrade(bought, { ...order, price: 0.1, quantity: 3, side: 'Venta' }).balance, 1000)
})

test('El fondeo aumenta efectivo y capital inicial, con límites y sin alterar las posiciones', () => {
  const bought = executeTrade(initial(), order)
  const funded = addFunds(bought, 10000)
  assert.equal(funded.balance, 10317.11)
  assert.equal(funded.deposit, 11000)
  assert.deepEqual(funded.trades, bought.trades)
  for (const amount of [0, -100, 99, 1000001, NaN, Infinity]) assert.throws(() => addFunds(bought, amount))
})
