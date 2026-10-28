import test from 'node:test'
import assert from 'node:assert/strict'
import { activityReport, csvText, dateInBogota, decimalUSD, filterReport, groupReport, localReport, presetPeriod, productMicros, reportCsv, summarize, usdMicros, validatePeriod } from '../src/utils/reports.ts'
import { createBroker } from '../server/alpaca.mjs'
import { createApiHandler } from '../server/api.mjs'
import { Readable } from 'node:stream'

const fill = (overrides = {}) => ({ id: 'execution-1', activity_type: 'FILL', symbol: 'AAPL', side: 'buy', price: '0.1', qty: '3', order_id: 'order-1', transaction_time: '2026-10-17T15:00:00Z', ...overrides })
const fee = (overrides = {}) => ({ id: 'fee-1', activity_type: 'FEE', net_amount: '-0.30', date: '2026-10-17', description: 'Cargo del proveedor', ...overrides })

test('Cálculos monetarios exactos, acciones fraccionarias y cargos inferiores a un centavo', () => {
  assert.equal(productMicros('0.1', '3'), 300000n)
  assert.equal(productMicros('100.25', '0.123456'), 12376464n)
  assert.equal(usdMicros('-0.00012'), -120n)
  assert.equal(decimalUSD(-120n), '-0.000120')
  assert.equal(usdMicros('0.0000005'), 1n)
  assert.equal(usdMicros('-0.0000005'), -1n)
  assert.throws(() => usdMicros('NaN'))
  assert.throws(() => productMicros('100', 'Infinity'))
})

test('Períodos incluyen ambos días completos en Bogotá y no usan el día UTC', () => {
  const data = activityReport([
    fill({ id: 'before', transaction_time: '2026-10-01T04:59:59.999Z' }),
    fill({ id: 'start', transaction_time: '2026-10-01T05:00:00Z' }),
    fill({ id: 'end', transaction_time: '2026-11-01T04:59:59.999Z' }),
    fill({ id: 'after', transaction_time: '2026-11-01T05:00:00Z' }),
  ])
  assert.equal(dateInBogota('2026-10-01T04:59:59Z'), '2026-09-30')
  const filtered = filterReport(data, '2026-10-01', '2026-10-31')
  assert.deepEqual(filtered.transactions.map(t => t.id), ['end', 'start'])
})

test('Las fechas personalizadas rechazan días inexistentes, valores vacíos e intervalos invertidos', () => {
  assert.equal(validatePeriod('2026-10-01', '2026-10-31'), '')
  assert.ok(validatePeriod('2026-02-30', '2026-10-31'))
  assert.ok(validatePeriod('', '2026-10-31'))
  assert.ok(validatePeriod('2026-10-31', '2026-10-01'))
  assert.equal(filterReport(activityReport([fill()]), '2026-10-31', '2026-10-01').transactions.length, 0)
})

test('Presets resuelven cambio de año, mes anterior y año bisiesto', () => {
  assert.deepEqual(presetPeriod('anterior', new Date('2026-01-05T12:00:00Z')), { start: '2025-12-01', end: '2025-12-31' })
  assert.deepEqual(presetPeriod('anterior', new Date('2024-03-05T12:00:00Z')), { start: '2024-02-01', end: '2024-02-29' })
  assert.deepEqual(presetPeriod('7dias', new Date('2026-01-03T12:00:00Z')), { start: '2025-12-28', end: '2026-01-03' })
})

test('Desglosa compras, ventas, cargos y reembolsos sin presentar flujo como rentabilidad', () => {
  const data = activityReport([fill(), fill({ id: 'sale', side: 'sell', price: '1', qty: '1' }), fee(), fee({ id: 'refund', activity_type: 'PTR', net_amount: '0.10' }), fee({ id: 'dividend-fee', activity_type: 'DIVFEE', net_amount: '-0.05' })])
  assert.deepEqual(summarize(data), { count: 2, purchases: 300000n, sales: 1000000n, volume: 1300000n, charges: 350000n, refunds: 100000n, netFees: 250000n, netCash: 450000n })
  assert.equal(groupReport(data, 'month')[0].netCash, 450000n)
})

test('Las ejecuciones parciales se cuentan por qty, sin duplicar actividades ni usar cum_qty', () => {
  const partial = fill({ qty: '1', cum_qty: '1' })
  const data = activityReport([partial, partial, fill({ id: 'execution-2', qty: '2', cum_qty: '3' })])
  assert.equal(data.transactions.length, 2)
  assert.equal(summarize(data).purchases, 300000n)
})

test('Un cargo posterior no se atribuye a la fecha de compra ni a una orden no informada', () => {
  const data = activityReport([fill({ transaction_time: '2026-10-31T19:00:00Z' }), fee({ date: '2026-11-02' })])
  const october = filterReport(data, '2026-10-01', '2026-10-31')
  const november = filterReport(data, '2026-11-01', '2026-11-30')
  assert.equal(october.fees.length, 0)
  assert.equal(november.fees.length, 1)
  assert.equal(november.fees[0].orderId, '')
  assert.equal(november.fees[0].date, '2026-11-02')
})

test('No convierte divisas ni omite silenciosamente ejecuciones o cargos inválidos', () => {
  assert.equal(activityReport([fee({ currency: 'EUR' })]).excluded, 1)
  assert.throws(() => activityReport([fill({ qty: undefined })]), /todos sus campos/)
  assert.throws(() => activityReport([fee({ net_amount: 'invalid' })]))
  assert.throws(() => activityReport([fee({ date: '2026-02-30' })]))
})

test('La demostración local conserva su historial y no inventa tarifas', () => {
  const trade = { id: 'local-1', symbol: 'AAPL', name: 'Apple', quantity: 3, price: 0.1, side: 'Compra', date: '2026-10-17T15:00:00Z' }
  const data = localReport([trade])
  assert.equal(data.transactions[0].grossMicros, 300000n)
  assert.equal(summarize(data).netFees, 0n)
  assert.equal(data.fees.length, 0)
})

test('CSV exporta todo el período, conserva metadatos y protege fórmulas, comillas y saltos de línea', () => {
  const data = activityReport(Array.from({ length: 40 }, (_, i) => fill({ id: `execution-${i}` })))
  const csv = reportCsv(data, '2026-10-01', '2026-10-31', 'Alpaca Paper', 'transactions', 'day')
  assert.equal(csv.split('\r\n').length, 41)
  assert.ok(csv.startsWith('\ufeff'))
  assert.ok(csv.includes('America/Bogota'))
  assert.ok(csv.includes('"0.300000"'))
  assert.equal(csvText([['=HYPERLINK("bad")', 'a;b\nc', '-0.000120']]), '\ufeff"\'=HYPERLINK(""bad"")";"a;b\nc";"-0.000120"')
})

function paginatedBroker({ repeated = false } = {}) {
  const calls = []
  const firstPage = Array.from({ length: 100 }, (_, i) => fill({ id: `page-1-${i}` }))
  const broker = createBroker({ env: { ALPACA_API_KEY: 'test-key', ALPACA_SECRET_KEY: 'test-secret' }, fetcher: async url => {
    const parsed = new URL(url); calls.push(parsed)
    assert.equal(parsed.origin, 'https://paper-api.alpaca.markets')
    assert.equal(parsed.pathname, '/v2/account/activities')
    assert.equal(parsed.searchParams.get('activity_types'), 'FILL,FEE,DIVFEE,PTC,PTR')
    assert.equal(parsed.searchParams.get('page_size'), '100')
    return Response.json(!parsed.searchParams.get('page_token') || repeated ? firstPage : [fee({ id: 'page-2-fee' })])
  } })
  return { broker, calls }
}

test('API de actividades recorre más de 100 registros con cursor sin limitarse a las órdenes recientes', async () => {
  const { broker, calls } = paginatedBroker()
  const page1 = await broker.activities()
  assert.equal(page1.activities.length, 100)
  assert.equal(page1.nextPageToken, 'page-1-99')
  const page2 = await broker.activities(page1.nextPageToken)
  assert.equal(page2.activities.length, 1)
  assert.equal(page2.nextPageToken, null)
  assert.equal(calls[1].searchParams.get('page_token'), 'page-1-99')
  assert.equal(activityReport([...page1.activities, ...page2.activities]).fees.length, 1)
})

test('Rechaza cursores inválidos y páginas repetidas en vez de presentar reportes completos', async () => {
  const { broker } = paginatedBroker({ repeated: true })
  await assert.rejects(broker.activities('x'.repeat(257)), err => err.status === 400)
  await assert.rejects(broker.activities('bad\nvalue'), err => err.status === 400)
  await assert.rejects(broker.activities('page-1-99'), /repitió una página/)
})

test('La nueva ruta de reportes entrega actividades y no devuelve secretos', async () => {
  const { broker } = paginatedBroker()
  const request = Readable.from([])
  request.method = 'GET'; request.url = '/api/paper/activities'; request.headers = { host: 'localhost:5173' }
  const response = { status: 0, body: '', writeHead(status) { this.status = status }, end(body) { this.body = body } }
  await createApiHandler(broker)(request, response)
  assert.equal(response.status, 200)
  assert.equal(JSON.parse(response.body).activities.length, 100)
  assert.ok(!response.body.includes('test-secret'))
})
