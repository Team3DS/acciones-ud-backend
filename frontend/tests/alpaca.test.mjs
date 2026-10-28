import test from 'node:test'
import assert from 'node:assert/strict'
import { Readable } from 'node:stream'
import { createBroker, PAPER_URL, SYMBOLS } from '../server/alpaca.mjs'
import { createApiHandler } from '../server/api.mjs'

const id = 'soltech-12345678-1234-1234-1234-123456789abc'
const input = { symbol: 'AAPL', quantity: 2, side: 'buy', clientOrderId: id }
const credentials = { ALPACA_API_KEY: 'test-key', ALPACA_SECRET_KEY: 'test-secret' }
function fixture(options = {}) {
  const calls = []
  let remoteOrder = options.existing || null
  let progress = 0
  const clock = { is_open: options.open !== false, timestamp: new Date().toISOString(), next_open: '2026-10-19T09:30:00-04:00', next_close: '2026-10-19T16:00:00-04:00' }
  const account = { id: 'paper-account', account_number: 'PA123', cash: options.cash ?? '10000', buying_power: '10000', equity: '10000', status: 'ACTIVE', trading_blocked: Boolean(options.blocked) }
  const fetcher = async (url, request = {}) => {
    calls.push({ url, request })
    const path = new URL(url).pathname
    if (url.startsWith('https://finnhub.io/')) return Response.json({ c: 100, t: Math.floor(Date.now() / 1000) })
    assert.ok(url.startsWith(PAPER_URL) || url.startsWith('https://data.alpaca.markets'))
    assert.equal(request.headers['APCA-API-KEY-ID'], 'test-key')
    if (path === '/v2/orders:by_client_order_id') {
      if (remoteOrder && options.progress) {
        const status = options.progress[Math.min(progress++, options.progress.length - 1)]
        remoteOrder = { ...remoteOrder, status, filled_qty: status === 'filled' ? remoteOrder.qty : status === 'partially_filled' ? '1' : '0', filled_avg_price: ['filled', 'partially_filled'].includes(status) ? '100.10' : null }
      }
      return remoteOrder ? Response.json(remoteOrder) : Response.json({}, { status: 404 })
    }
    if (path === '/v2/clock') return Response.json(clock)
    if (path === '/v2/account') return Response.json(account)
    if (path.startsWith('/v2/assets/')) return Response.json({ tradable: !options.untradable, status: 'active', class: 'us_equity', exchange: SYMBOLS[path.split('/').at(-1)] })
    if (path.endsWith('/quotes/latest')) return options.quoteError ? Response.json({}, { status: 429 }) : Response.json({ quote: { ap: 100, bp: 99.9, t: new Date(Date.now() - (options.stale ? 120000 : 0)).toISOString() } })
    if (path.endsWith('/snapshot')) return Response.json({latestTrade:{p:100},dailyBar:{v:12500},prevDailyBar:{c:98}})
    if (path === '/v2/orders/provider-order' && request.method === 'DELETE') { remoteOrder={...remoteOrder,status:'canceled',canceled_at:new Date().toISOString()};return new Response(null,{status:204}) }
    if (path === '/v2/orders' && request.method === 'POST') {
      if (options.timeout) throw new Error('Network timeout')
      if (options.reject) return Response.json({ message: 'do-not-expose-test-secret' }, { status: 422 })
      const body = JSON.parse(request.body)
      remoteOrder = { id: 'provider-order', client_order_id: body.client_order_id, symbol: body.symbol, side: body.side, qty: body.qty, type:body.type,limit_price:body.limit_price||null,time_in_force:body.time_in_force,filled_qty: options.pending ? '0' : body.qty, filled_avg_price: options.pending ? null : '100.10', status: options.pending ? 'accepted' : 'filled', submitted_at: new Date().toISOString(), filled_at: options.pending ? null : new Date().toISOString() }
      return Response.json(remoteOrder)
    }
    if (path === '/v2/positions') return Response.json(options.positions || (remoteOrder ? [{ symbol: 'AAPL', qty: '2', qty_available: '2', avg_entry_price: '100.1', current_price: '100.1', market_value: '200.2', unrealized_pl: '0' }] : []))
    if (path === '/v2/orders') return Response.json(remoteOrder ? [remoteOrder] : [])
    throw new Error(`Unexpected test route ${path}`)
  }
  let virtualTime = Date.now()
  return { calls, broker: createBroker({ env: { ...credentials, ...options.env }, fetcher, ...(options.virtualClock ? { now: () => virtualTime, sleep: async ms => { virtualTime += ms } } : {}) }), fetcher }
}

test('Recorrido cotización → orden paper → ejecución → posiciones; claves solo en solicitudes del servidor', async () => {
  const { broker, calls } = fixture()
  assert.equal((await broker.quote('AAPL')).ask, 100)
  const result = await broker.submit(input)
  assert.equal(result.order.status, 'filled')
  assert.equal(result.order.filledPrice, 100.1)
  assert.ok(result.elapsedMs < 5000)
  const snapshot = await broker.snapshot()
  assert.equal(snapshot.positions[0].quantity, 2)
  assert.equal(snapshot.orders[0].id, result.order.id)
  const submission = calls.find(c => c.request.method === 'POST')
  assert.equal(submission.url, `${PAPER_URL}/v2/orders`)
  assert.deepEqual(JSON.parse(submission.request.body), { symbol: 'AAPL', qty: '2', side: 'buy', type: 'market', time_in_force: 'day', extended_hours: false, client_order_id: id })
  assert.ok(!JSON.stringify(snapshot).includes('test-secret'))
})

test('Acepta los diez símbolos solicitados y consulta la bolsa verificada en Alpaca', async () => {
  for (const [symbol, exchange] of Object.entries(SYMBOLS)) {
    const { broker } = fixture()
    assert.equal((await broker.quote(symbol)).exchange, exchange)
    assert.equal((await broker.submit({ ...input, symbol })).order.symbol, symbol)
  }
})

test('Mercado cerrado, saldo insuficiente, cuenta bloqueada, activo no negociable y cotización vieja nunca envían órdenes', async () => {
  for (const [option, code] of [[{ open: false }, 'MARKET_CLOSED'], [{ cash: '100' }, 'INSUFFICIENT_FUNDS'], [{ blocked: true }, 'ACCOUNT_BLOCKED'], [{ untradable: true }, 'ASSET_BLOCKED'], [{ stale: true }, 'STALE_QUOTE']]) {
    const { broker, calls } = fixture(option)
    await assert.rejects(broker.submit(input), err => err.code === code)
    assert.equal(calls.filter(c => c.request.method === 'POST').length, 0)
  }
})

test('Impide ventas en corto y valida cantidades y símbolos en el servidor', async () => {
  const { broker, calls } = fixture()
  await assert.rejects(broker.submit({ ...input, side: 'sell' }), err => err.code === 'INSUFFICIENT_POSITION')
  for (const quantity of [0, -1, 1.5, Infinity, NaN, 100001]) await assert.rejects(broker.submit({ ...input, quantity }))
  await assert.rejects(broker.submit({ ...input, symbol: 'INVALID' }))
  assert.equal(calls.filter(c => c.request.method === 'POST').length, 0)
})

test('La misma orden concurrente o reenviada se envía una sola vez', async () => {
  const { broker, calls } = fixture()
  const [a, b] = await Promise.all([broker.submit(input), broker.submit(input)])
  assert.equal(a.order.id, b.order.id)
  assert.equal((await broker.submit(input)).order.id, a.order.id)
  assert.equal(calls.filter(c => c.request.method === 'POST').length, 1)
  await assert.rejects(broker.submit({ ...input, quantity: 3 }), err => err.status === 409)
})

test('Un timeout tras enviar no se anuncia como ejecución y no provoca un segundo POST', async () => {
  const { broker, calls } = fixture({ timeout: true })
  const result = await broker.submit(input)
  assert.equal(result.uncertain, true)
  assert.equal(result.order, null)
  assert.equal((await broker.submit(input)).uncertain, true)
  assert.equal(calls.filter(c => c.request.method === 'POST').length, 1)
})

test('Una orden aceptada sin ejecución devuelve estado pendiente antes del presupuesto y sigue siendo consultable', async () => {
  const { broker } = fixture({ pending: true, virtualClock: true })
  const result = await broker.submit(input)
  assert.equal(result.order.status, 'accepted')
  assert.equal(result.order.filledQuantity, 0)
  assert.equal(result.order.filledPrice, null)
  assert.ok(result.elapsedMs < 5000)
  assert.equal((await broker.findOrder(id)).status, 'accepted')
})

test('Sigue la ejecución parcial hasta confirmar el llenado completo con precio del proveedor', async () => {
  const { broker } = fixture({ pending: true, progress: ['partially_filled', 'filled'], virtualClock: true })
  const result = await broker.submit(input)
  assert.equal(result.order.status, 'filled')
  assert.equal(result.order.filledQuantity, 2)
  assert.equal(result.order.filledPrice, 100.1)
})

test('Respaldo Finnhub identifica último precio, sin inventar bid/ask ni filtrar credenciales de Alpaca', async () => {
  const { broker, calls } = fixture({ quoteError: true, env: { FINNHUB_API_KEY: 'test-finnhub' } })
  const quote = await broker.quote('JNJ')
  assert.equal(quote.source, 'Finnhub · respaldo')
  assert.equal(quote.last, 100)
  assert.equal(quote.ask, null)
  assert.equal(quote.indicative, true)
  const fallback = calls.find(c => c.url.startsWith('https://finnhub.io/'))
  assert.equal(fallback.request.headers['APCA-API-KEY-ID'], undefined)
  assert.ok(!fallback.url.includes('test-finnhub'))
})

test('Un rechazo de Alpaca conserva el error sin exponer la respuesta cruda', async () => {
  const { broker } = fixture({ reject: true })
  await assert.rejects(broker.submit(input), err => err.code === 'PROVIDER_422' && !err.message.includes('test-secret'))
})

async function apiRequest(handler, { path = '/api/paper/status', method = 'GET', origin, host = 'localhost:5173', body = '' } = {}) {
  const req = Readable.from(body ? [body] : [])
  req.url = path; req.method = method; req.headers = { host, ...(origin ? { origin } : {}), 'content-type': 'application/json' }
  const response = { status: 0, headers: {}, body: '', writeHead(status, headers) { this.status = status; this.headers = headers }, end(data) { this.body = data || '' } }
  await handler(req, response)
  return { ...response, data: JSON.parse(response.body) }
}

test('API local: bloquea hosts externos, orígenes ajenos y POST sin origen', async () => {
  const handler = createApiHandler(fixture().broker)
  assert.equal((await apiRequest(handler, { host: 'evil.example' })).status, 403)
  assert.equal((await apiRequest(handler, { origin: 'https://evil.example' })).status, 403)
  assert.equal((await apiRequest(handler, { path: '/api/paper/orders', method: 'POST', body: JSON.stringify(input) })).status, 403)
  const valid = await apiRequest(handler, { path: '/api/paper/orders', method: 'POST', origin: 'http://localhost:5173', body: JSON.stringify(input) })
  assert.equal(valid.status, 200)
  assert.equal(valid.data.order.status, 'filled')
  assert.equal(valid.headers['Cache-Control'], 'no-store')
})

test('Sin claves muestra configuración pendiente y no consulta proveedores', async () => {
  let fetched = false
  const broker = createBroker({ env: {}, fetcher: async () => { fetched = true; throw new Error() } })
  const handler = createApiHandler(broker)
  assert.equal((await apiRequest(handler)).data.configured, false)
  const result = await apiRequest(handler, { path: '/api/paper/snapshot' })
  assert.equal(result.status, 503)
  assert.equal(fetched, false)
})

test('RF-26: envía precio límite y vigencia sin inventar una ejecución',async()=>{
  const {broker,calls}=fixture({pending:true,virtualClock:true})
  const result=await broker.submit({...input,type:'limit',limitPrice:100,timeInForce:'gtc'})
  assert.equal(result.order.status,'accepted');assert.equal(result.order.type,'limit')
  const body=JSON.parse(calls.find(c=>c.request.method==='POST').request.body)
  assert.equal(body.limit_price,'100.00');assert.equal(body.time_in_force,'gtc')
})
test('RF-13: mercado cerrado exige confirmación explícita para encolar en Alpaca',async()=>{
  const {broker,calls}=fixture({open:false,pending:true,virtualClock:true})
  await assert.rejects(broker.submit(input),err=>err.code==='MARKET_CLOSED')
  assert.equal(calls.filter(c=>c.request.method==='POST').length,0)
  const result=await broker.submit({...input,queueConfirmed:true})
  assert.equal(result.order.status,'accepted');assert.notEqual(result.order.status,'filled')
})
test('RF-29: disponibilidad debe cubrir valor, comisión y margen Market',async()=>{
  const {broker}=fixture({cash:'208'})
  await assert.rejects(broker.submit(input),err=>err.code==='INSUFFICIENT_FUNDS')
})
test('RF-34: cancelación se solicita al proveedor y no se finge liberación de fondos',async()=>{
  const {broker,calls}=fixture({pending:true,virtualClock:true})
  await broker.submit({...input,type:'limit',limitPrice:100})
  assert.equal((await broker.cancel(id)).requested,true)
  assert.equal((await broker.findOrder(id)).status,'canceled')
  assert.equal(calls.filter(c=>c.request.method==='DELETE').length,1)
})
test('RF-34: no cancela una orden ya ejecutada',async()=>{
  const {broker}=fixture();await broker.submit(input)
  await assert.rejects(broker.cancel(id),err=>err.status===409)
})
test('RF-17: último precio, spread y volumen provienen de snapshot; no inventa capitalización',async()=>{
  const {broker}=fixture();const q=await broker.quote('AAPL',1400,true)
  assert.equal(q.last,100);assert.equal(q.volume,12500);assert.equal(q.marketCap,null)
  assert.ok(Math.abs(q.spread-.1)<.00001)
})
