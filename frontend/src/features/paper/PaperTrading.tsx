import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react'
import { Icon } from '@/components/Icon'
import { money } from '@/services/market'
import { paperApi, paperAssets, PaperError, orderStatus, terminalOrder } from '@/services/paper'
import type { PaperOrder, PaperQuote, PaperSnapshot } from '@/types/paper'
import { tradeAmounts } from '@/utils/commission.mjs'
import '@/styles/paper.css'

const errorText = (err: unknown) => err instanceof Error ? err.message : 'No se pudo completar la solicitud.'
function readPending() { try { return sessionStorage.getItem('soltech.paper.pending') || '' } catch { return '' } }
const dateNY = (date: string) => new Intl.DateTimeFormat('es-CO', { timeZone: 'America/New_York', dateStyle: 'medium', timeStyle: 'short' }).format(new Date(date))

export default function PaperTrading({ notify }: { notify: (message: string) => void }) {
  const [configured, setConfigured] = useState<boolean | null>(null)
  const [snapshot, setSnapshot] = useState<PaperSnapshot | null>(null)
  const [symbol, setSymbol] = useState('AAPL')
  const [exchange, setExchange] = useState('Todos')
  const [quote, setQuote] = useState<PaperQuote | null>(null)
  const [quoteBusy, setQuoteBusy] = useState(false)
  const [quoteError, setQuoteError] = useState('')
  const [quantity, setQuantity] = useState('1')
  const [side, setSide] = useState<'buy' | 'sell'>('buy')
  const [orderType,setOrderType] = useState<'market' | 'limit'>('market')
  const [limitPrice,setLimitPrice] = useState('')
  const [timeInForce,setTimeInForce] = useState<'day' | 'gtc'>('day')
  const [queueConfirmed,setQueueConfirmed] = useState(false)
  const [reviewed,setReviewed] = useState(false)
  const [pending, setPending] = useState(readPending)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [syncError, setSyncError] = useState('')
  const [lastOrder, setLastOrder] = useState<PaperOrder | null>(null)
  const [elapsed, setElapsed] = useState<number | null>(null)
  const [messages, setMessages] = useState<string[]>([])
  const [tick, setTick] = useState(Date.now())
  const mounted = useRef(true)
  const submission = useRef(false)
  const notified = useRef(new Set<string>())
  const observedOrders = useRef(new Map<string, PaperOrder>())
  const quoteSequence = useRef(0)
  const refreshSequence = useRef(0)
  const refreshRunning = useRef(false)
  const refreshAgain = useRef(false)
  const selected = paperAssets.find(a => a.symbol === symbol)!
  const track = (id: string) => { setPending(id); try { if (id) sessionStorage.setItem('soltech.paper.pending', id); else sessionStorage.removeItem('soltech.paper.pending') } catch { /* El seguimiento continúa en memoria. */ } }
  useEffect(() => { mounted.current = true; const timer = setInterval(() => setTick(Date.now()), 1000); return () => { mounted.current = false; clearInterval(timer) } }, [])
  const refresh = useCallback(async () => {
    if (refreshRunning.current) { refreshAgain.current = true; return }
    refreshRunning.current = true
    const sequence = ++refreshSequence.current
    try { const value = await paperApi.snapshot(); if (mounted.current && sequence === refreshSequence.current) { setSnapshot(value); setSyncError('') } }
    catch (err) { if (mounted.current) setSyncError(`No se pudo actualizar el portafolio: ${errorText(err)}`) }
    finally { refreshRunning.current = false; if (refreshAgain.current && mounted.current) { refreshAgain.current = false; void refresh() } }
  }, [])
  const report = useCallback((order: PaperOrder) => {
    if (!mounted.current) return
    const previous = observedOrders.current.get(order.id)
    if (previous && ((terminalOrder(previous.status) && !terminalOrder(order.status)) || previous.filledQuantity > order.filledQuantity)) return
    observedOrders.current.set(order.id, order)
    setError('')
    setLastOrder(order)
    const key = `${order.id}:${order.status}:${order.filledQuantity}`
    if (!notified.current.has(key)) {
      notified.current.add(key)
      const message = `${orderStatus(order.status)}: ${order.side === 'buy' ? 'compra' : 'venta'} de ${order.quantity} ${order.symbol}${order.filledQuantity ? ` · ${order.filledQuantity} ejecutadas` : ''}.`
      notify(message); setMessages(current => [message, ...current].slice(0, 8))
      void refresh()
    }
    if (terminalOrder(order.status)) track('')
  }, [notify, refresh])
  useEffect(() => {
    let active = true
    paperApi.status().then(value => { if (active) { setConfigured(value.configured); setError('') } }).catch(err => { if (active) setError(errorText(err)) })
    return () => { active = false }
  }, [])
  useEffect(() => {
    if (!configured) return
    void refresh()
    const timer = setInterval(() => void refresh(), 15000)
    return () => clearInterval(timer)
  }, [configured, refresh])
  const loadQuote = useCallback(async () => {
    const sequence = ++quoteSequence.current
    setQuoteBusy(true); setQuoteError('')
    try { const value = await paperApi.quote(symbol); if (mounted.current && sequence === quoteSequence.current) setQuote(value) }
    catch (err) { if (mounted.current && sequence === quoteSequence.current) { setQuoteError(errorText(err)); setQuote(null) } }
    finally { if (mounted.current && sequence === quoteSequence.current) setQuoteBusy(false) }
  }, [symbol])
  useEffect(() => {
    if (!configured) return
    setQuote(null); void loadQuote()
    const timer = setInterval(() => void loadQuote(), 15000)
    return () => { clearInterval(timer); quoteSequence.current++ }
  }, [configured, loadQuote])
  useEffect(() => {
    if (!pending || !configured) return
    let active = true
    let running = false
    const check = async () => {
      if (running) return
      running = true
      try { const result = await paperApi.order(pending); if (active) { setError(''); report(result.order) } }
      catch (err) { if (active) setError(`${errorText(err)} Identificador: ${pending}`) }
      finally { running = false }
    }
    void check()
    const timer = setInterval(() => void check(), 2000)
    return () => { active = false; clearInterval(timer) }
  }, [pending, configured, report])

  const quoteMatches = quote?.symbol === symbol
  const fresh = quoteMatches && !quote.stale && tick - Date.parse(quote.timestamp) <= 90000
  const price = quoteMatches ? (side === 'buy' ? quote.ask ?? quote.last : quote.bid ?? quote.last) : null
  const reference = orderType === 'limit' ? Number(limitPrice) : price
  let amounts = { gross: 0, commission: 0, total: 0, rateBps: 200 }
  try { if(reference && Number.isSafeInteger(Number(quantity)) && Number(quantity)>0) amounts=tradeAmounts(reference,Number(quantity),200,side) } catch { /* La validación bloqueará el envío. */ }
  const syncFresh = snapshot && tick - Date.parse(snapshot.updatedAt) < 45000
  const sessionOpen = Boolean(snapshot && syncFresh && snapshot.clock.isOpen && tick < Date.parse(snapshot.clock.nextClose))
  const affordable = snapshot ? amounts.total + (orderType === 'market' ? amounts.gross*.03 : 0) <= Math.min(snapshot.account.cash, snapshot.account.buyingPower) : false
  const available = snapshot?.positions.find(p => p.symbol === symbol)?.available || 0
  const validQuantity = Number.isSafeInteger(Number(quantity)) && Number(quantity) > 0 && Number(quantity) <= 100000
  const validReference = orderType === 'limit' ? Number(limitPrice)>0 && /^\d+(\.\d{1,2})?$/.test(limitPrice) : Boolean(quoteMatches && (fresh || (!sessionOpen && queueConfirmed)) && reference)
  const canTrade = configured && snapshot && syncFresh && !snapshot.account.blocked && snapshot.account.status === 'ACTIVE' && (sessionOpen || queueConfirmed) && validReference && validQuantity && !busy && !pending && (side === 'buy' ? affordable : available >= Number(quantity))
  useEffect(()=>{setReviewed(false);setQueueConfirmed(false)},[symbol,side,quantity,orderType,limitPrice,timeInForce])
  async function submit(e: FormEvent) {
    e.preventDefault()
    if (!canTrade || submission.current) return
    if (!reviewed) { setReviewed(true); return }
    submission.current = true; setBusy(true); setError(''); setElapsed(null); setLastOrder(null)
    const clientOrderId = `soltech-${crypto.randomUUID()}`
    track(clientOrderId)
    try {
      const result = await paperApi.submit({ symbol, quantity: Number(quantity), side, clientOrderId, type:orderType, ...(orderType==='limit'?{limitPrice:Number(limitPrice)}:{}), timeInForce:orderType==='market'?'day':timeInForce, queueConfirmed })
      if (!mounted.current) return
      setElapsed(result.elapsedMs)
      if (result.order) report(result.order)
      else setError('Aún no se puede confirmar si Alpaca recibió la orden. El seguimiento continúa con el mismo identificador; no la envíes nuevamente.')
      void refresh()
    } catch (err) {
      if (!mounted.current) return
      setError(errorText(err))
      const uncertain = !(err instanceof PaperError) || ['NETWORK_TIMEOUT', 'INVALID_RESPONSE', 'PROVIDER_TIMEOUT', 'INTERNAL_ERROR'].includes(err.code) || /^PROVIDER_5/.test(err.code)
      if (!uncertain) track('')
    } finally { submission.current = false; if (mounted.current) setBusy(false) }
  }
  if (configured === null) return <article className="card paper-setup"><Icon name="shield" size={30}/><h2>Conectando con Alpaca Paper…</h2>{error && <p className="form-error" role="alert">{error}</p>}<p>El servidor comprueba la configuración de tu cuenta de práctica.</p></article>
  if (!configured) return <article className="card paper-setup"><span className="form-emblem"><Icon name="chart" size={28}/></span><span className="section-tag">NYSE + NASDAQ</span><h2>El mercado real.<br/>Con dinero de práctica.</h2><p>Conecta tu cuenta de Alpaca Paper para consultar cotizaciones, enviar órdenes y ver tus posiciones directamente desde Soltech.</p><ol><li>Crea tus claves en el entorno <strong>Paper Trading</strong> de Alpaca.</li><li>Copia <code>.env.example</code> como <code>.env</code> en la raíz del proyecto.</li><li>Completa <code>ALPACA_API_KEY</code> y <code>ALPACA_SECRET_KEY</code>. Reinicia <code>npm run dev</code>.</li></ol><div className="demo-note"><Icon name="lock" size={18}/><span>Las claves permanecen en el servidor. No las pegues en el chat ni uses variables VITE_. Esta conexión es local y usa una sola cuenta paper.</span></div><a className="button button-primary" href="https://app.alpaca.markets/" target="_blank" rel="noreferrer">Abrir Alpaca <span>↗</span></a></article>
  return <section className="paper-workspace" aria-label="Operaciones conectadas con Alpaca Paper">
    <div className="paper-banner"><Icon name="shield" size={24}/><div><strong>Alpaca Paper · Conexión de extremo a extremo</strong><p>Las órdenes y posiciones pertenecen a tu cuenta paper de Alpaca. El saldo de la demostración local no se utiliza aquí.</p></div><span className="pill">Dinero virtual</span></div>
    <div className="paper-session"><div><span className={`market-dot ${sessionOpen ? 'open' : ''}`}/><strong>{!snapshot ? 'Consultando mercado…' : !syncFresh ? 'Estado por actualizar' : sessionOpen ? 'Mercado abierto' : 'Mercado cerrado'}</strong><span>NYSE / NASDAQ · 9:30–16:00 · America/New_York</span></div><button className="text-link" onClick={() => void refresh()}>Actualizar cuenta <Icon name="arrow" size={15}/></button></div>
    {snapshot && <p className="paper-next-session">{sessionOpen ? 'Cierre' : 'Próxima apertura'}: {dateNY(sessionOpen ? snapshot.clock.nextClose : snapshot.clock.nextOpen)} · Hora de Nueva York (con horario de verano y calendario de Alpaca).</p>}
    {syncError && <p className="form-error" role="alert">{syncError} Los valores anteriores pueden estar desactualizados.</p>}
    {snapshot?.account.blocked && <p className="form-error" role="alert">La cuenta tiene restricciones de operación en Alpaca.</p>}
    <div className="stats-grid paper-stats">{[['Patrimonio en Alpaca', snapshot?.account.equity], ['Efectivo paper', snapshot?.account.cash], ['Poder de compra', snapshot?.account.buyingPower]].map(([label, value]) => <article className="stat-card" key={label}><div><span>{label}</span><Icon name="wallet" size={18}/></div><strong>{typeof value === 'number' ? money(value) : '—'} <small>USD</small></strong><p>Cuenta {snapshot?.account.number || 'por consultar'} · Datos de Alpaca</p></article>)}</div>
    <div className="paper-grid"><article className="card paper-quote-card"><div className="card-heading"><div><h3>Consultar cotización</h3><p>Diez empresas. Dos bolsas. Nuevas posibilidades.</p></div><Icon name="chart" size={20}/></div><div className="trade-tabs">{['Todos', 'NYSE', 'NASDAQ'].map(value => <button type="button" key={value} className={exchange === value ? 'selected' : ''} onClick={() => setExchange(value)}>{value}</button>)}</div><div className="paper-symbols">{paperAssets.filter(a => exchange === 'Todos' || a.exchange === exchange).map(a => <button key={a.symbol} className={symbol === a.symbol ? 'selected' : ''} onClick={() => { setSymbol(a.symbol); setError('') }}><strong>{a.symbol}</strong><small>{a.exchange}</small></button>)}</div><div className="paper-asset-heading"><div><span className="section-tag">{selected.exchange}</span><h2>{selected.name}</h2><p>{symbol} · USD</p></div><button className="small-button" disabled={quoteBusy} onClick={() => void loadQuote()}>{quoteBusy ? 'Consultando…' : 'Actualizar precio'}</button></div>{quoteError && <p className="form-error" role="alert">{quoteError}</p>}<div className="paper-prices"><div><span>Compra (ask)</span><strong>{quoteMatches && quote.ask ? money(quote.ask) : '—'}</strong></div><div><span>Venta (bid)</span><strong>{quoteMatches && quote.bid ? money(quote.bid) : '—'}</strong></div>{quoteMatches && quote.last && <div><span>Último precio</span><strong>{money(quote.last)}</strong></div>}</div>{quoteMatches && <div className="quote-provenance"><span className="pill">{quote.source}</span><span>{dateNY(quote.timestamp)} ET</span><strong className={fresh ? 'positive' : 'negative'}>{fresh ? 'Cotización reciente' : 'Cotización desactualizada'}</strong></div>}{quoteMatches&&<div className="paper-metrics"><div><span>Spread</span><strong>{quote.spread!=null?money(quote.spread):'No disponible'}</strong></div><div><span>Variación diaria</span><strong>{quote.dailyChange!=null?`${quote.dailyChange.toFixed(2)} %`:'No disponible'}</strong></div><div><span>Volumen de la fuente</span><strong>{quote.volume!=null?new Intl.NumberFormat('es-CO').format(quote.volume):'No disponible'}</strong></div><div><span>Capitalización</span><strong>No disponible en el feed integrado</strong></div></div>}<div className="demo-note">El feed IEX gratuito tiene cobertura limitada y puede diferir del mercado consolidado. El respaldo Finnhub, si está configurado, muestra el último precio, sin bid/ask. El precio de ejecución puede variar.</div></article>
    <article className="card paper-order-card"><div className="card-heading"><h3>Crear orden · Market / Limit</h3><span className="pill">PAPER</span></div><form className="trade-form" onSubmit={submit}><div className="trade-tabs">{(['buy', 'sell'] as const).map(value => <button type="button" key={value} disabled={busy || Boolean(pending)} className={side === value ? 'selected' : ''} onClick={() => { setSide(value); setError('') }}>{value === 'buy' ? 'Comprar' : 'Vender'}</button>)}</div><label>Activo seleccionado<input readOnly value={`${symbol} · ${selected.exchange}`}/></label><label>Tipo de orden<select disabled={busy || Boolean(pending)} value={orderType} onChange={e=>setOrderType(e.target.value as 'market'|'limit')}><option value="market">Market · Al mercado</option><option value="limit">Limit · Precio límite</option><option disabled>Stop Loss · Integración pendiente (Should)</option><option disabled>Take Profit · Integración pendiente (Should)</option></select></label>{orderType==='limit'&&<><label>Precio límite en USD<input required type="number" min="0.01" step="0.01" value={limitPrice} disabled={busy || Boolean(pending)} onChange={e=>setLimitPrice(e.target.value)}/></label><label>Vigencia<select value={timeInForce} onChange={e=>setTimeInForce(e.target.value as 'day'|'gtc')}><option value="day">Día</option><option value="gtc">Hasta cancelación</option></select></label></>}<label>Cantidad de acciones<input type="number" required min="1" step="1" max="100000" disabled={busy || Boolean(pending)} value={quantity} onChange={e => setQuantity(e.target.value)}/></label><div className="trade-total"><span>Importe estimado</span><strong>{reference ? money(amounts.gross) : '—'} <small>USD</small></strong></div><div className="trade-balance"><span>{side === 'buy' ? 'Margen de validación' : 'Acciones disponibles'}</span><span>{side === 'buy' ? orderType==='market'?'3 % por variación':'Sin margen adicional' : available}</span></div>{side === 'buy' && snapshot && fresh && !affordable && <p className="form-error">Fondos insuficientes para el importe, la comisión estimada y el margen de precio aplicable.</p>}{!sessionOpen && <label className="checkbox-label"><input type="checkbox" checked={queueConfirmed} disabled={busy || Boolean(pending)} onChange={e=>{setQueueConfirmed(e.target.checked);setReviewed(false)}}/><span>El mercado está cerrado. Confirmo expresamente que deseo encolar esta orden en Alpaca para su próxima apertura hábil. La cola es responsabilidad del proveedor.</span></label>}{!fresh && orderType==='market'&&<p className="demo-note">Referencia de cierre o cotización antigua: el precio de ejecución en la próxima apertura puede cambiar.</p>}<div className="trade-balance"><span>Comisión Soltech estimada (2 %)</span><strong>{money(amounts.commission)}</strong></div><div className="trade-total"><span>{side==='buy'?'Total a debitar (estimado)':'Neto a acreditar (estimado)'}</span><strong>{money(amounts.total)}</strong></div><p className="paper-sla">RF-30: compra suma comisión y venta la resta. Se muestra el cálculo con dos decimales; su cobro y liquidación por usuario aún no están implementados. Alpaca no descuenta esta comisión de Soltech.</p>{reviewed&&<div className="demo-note">Revisa {quantity} {symbol}, tipo {orderType==='limit'?'Limit':'Market'}, comisión estimada {money(amounts.commission)} y total {money(amounts.total)}. Confirma para enviar a Alpaca Paper.</div>}{error && <p className="form-error" role="alert">{error}</p>}<button className="button button-primary full" disabled={!canTrade}>{busy ? 'Enviando a Alpaca…' : pending ? 'Orden en seguimiento…' : reviewed?`Confirmar ${side==='buy'?'compra':'venta'} en paper`:'Revisar orden'}<Icon name="arrow" size={17}/></button><p className="paper-sla">Objetivo: respuesta en ≤ 5 s. Una orden aceptada aún puede estar pendiente de ejecución.</p></form></article></div>
    {(pending || lastOrder) && <article className="card paper-order-status" role="status"><Icon name={lastOrder?.status === 'filled' ? 'check' : 'orders'} size={24}/><div><strong>{lastOrder ? orderStatus(lastOrder.status) : 'Esperando confirmación de Alpaca'}</strong><p>{lastOrder ? `${lastOrder.symbol} · ${lastOrder.filledQuantity}/${lastOrder.quantity} acciones ejecutadas${lastOrder.filledPrice ? ` · Precio medio ${money(lastOrder.filledPrice)}` : ''}` : 'Si la conexión se interrumpe, se consulta el mismo identificador para evitar duplicar la orden.'}</p><code>{pending || lastOrder?.clientOrderId}</code></div>{elapsed !== null && <span className="pill">Respuesta en {(elapsed / 1000).toFixed(2)} s</span>}</article>}
    <article className="card"><div className="card-heading"><div><h3>Mi portafolio · Alpaca</h3><p>{snapshot ? `Última sincronización: ${dateNY(snapshot.updatedAt)} ET` : 'Consultando posiciones…'}</p></div><span className="pill">{snapshot?.positions.length || 0} posiciones</span></div><div className="table-scroll"><table><thead><tr><th>Activo</th><th>Cantidad</th><th>Precio promedio</th><th>Precio actual</th><th>Valor de mercado</th><th>Ganancia / pérdida</th></tr></thead><tbody>{snapshot?.positions.map(p => <tr key={p.symbol}><td><strong>{p.symbol}</strong></td><td>{p.quantity}</td><td>{money(p.averagePrice)}</td><td>{money(p.currentPrice)}</td><td>{money(p.marketValue)}</td><td className={p.unrealizedPL >= 0 ? 'positive' : 'negative'}>{money(p.unrealizedPL)}</td></tr>)}</tbody></table>{snapshot?.positions.length === 0 && <p className="empty-state">Aún no hay posiciones en tu cuenta de Alpaca Paper.</p>}</div></article>
    <article className="card"><div className="card-heading"><div><h3>Órdenes en Alpaca</h3><p>Últimas 100 órdenes de tu cuenta, con su estado real en paper.</p></div></div><div className="table-scroll"><table><thead><tr><th>Activo</th><th>Operación</th><th>Solicitadas / ejecutadas</th><th>Precio ejecutado</th><th>Estado</th><th>Fecha (Nueva York)</th></tr></thead><tbody>{snapshot?.orders.map(order => <tr key={order.id}><td><strong>{order.symbol}</strong></td><td>{order.side === 'buy' ? 'Compra' : 'Venta'}</td><td>{order.quantity} / {order.filledQuantity}</td><td>{order.filledPrice !== null ? money(order.filledPrice) : 'Pendiente'}</td><td><span className="pill">{orderStatus(order.status)}</span></td><td>{order.submittedAt ? dateNY(order.submittedAt) : '—'}</td></tr>)}</tbody></table>{snapshot?.orders.length === 0 && <p className="empty-state">Tus órdenes aparecerán aquí cuando las reciba Alpaca.</p>}</div></article>
    <article className="card paper-notifications"><div className="card-heading"><h3><Icon name="bell" size={18}/> Notificaciones de esta sesión</h3></div>{messages.length ? <ul>{messages.map((message, i) => <li key={`${i}-${message}`}>{message}</li>)}</ul> : <p>Recibirás una notificación cuando cambie el estado de tu orden.</p>}</article>
  </section>
}
