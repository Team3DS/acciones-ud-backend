import { useEffect, useMemo, useRef, useState } from 'react'
import { Icon } from '@/components/Icon'
import { paperApi } from '@/services/paper'
import { activityReport, commissionCsv, dateInBogota, feeLabel, filterReport, formatUSD, groupReport, localReport, platformCommissions, presetPeriod, reportCsv, summarize, validatePeriod } from '@/utils/reports'
import type { Account } from '@/types'
import type { PaperActivity } from '@/types/reports'
import '@/styles/reports.css'

type Tab = 'summary' | 'transactions' | 'fees' | 'commissions'
const PAGE_SIZE = 15
const tabs: [Tab, string][] = [['summary', 'Resumen por período'], ['transactions', 'Transacciones'], ['commissions', 'Comisión Soltech (estimada)'], ['fees', 'Cargos del proveedor']]
const errorMessage = (err: unknown) => err instanceof Error ? err.message : 'No se pudo consultar el historial.'
export default function Reports({ account, notify }: { account: Account; notify: (text: string) => void }) {
  const [source, setSource] = useState<'local' | 'paper'>('paper')
  const [preset, setPreset] = useState('mes')
  const [period, setPeriod] = useState(() => presetPeriod('mes'))
  const [grouping, setGrouping] = useState<'day' | 'month'>('day')
  const [tab, setTab] = useState<Tab>('summary')
  const [page, setPage] = useState(1)
  const [activities, setActivities] = useState<PaperActivity[]>([])
  const [complete, setComplete] = useState(false)
  const [loading, setLoading] = useState(false)
  const [loadError, setLoadError] = useState('')
  const [loadedAt, setLoadedAt] = useState('')
  const generation = useRef(0)
  const rowsRef = useRef(new Map<string, PaperActivity>())
  const cursor = useRef('')
  const cursors = useRef(new Set<string>())
  const running = useRef(false)
  const periodError = validatePeriod(period.start, period.end)

  async function loadHistory(reset = false) {
    if (running.current && !reset) return
    const version = reset ? ++generation.current : generation.current
    if (reset) { rowsRef.current.clear(); cursor.current = ''; cursors.current.clear(); setActivities([]); setComplete(false); setLoadedAt('') }
    running.current = true; setLoading(true); setLoadError('')
    try {
      // Carga hasta 1.000 actividades por lote. El usuario puede continuar sin truncar el reporte.
      for (let i = 0; i < 10; i++) {
        const response = await paperApi.activities(cursor.current)
        if (generation.current !== version) return
        if (!Array.isArray(response.activities)) throw new Error('El historial recibido no es válido.')
        if (response.nextPageToken && cursors.current.has(response.nextPageToken)) throw new Error('El proveedor repitió una página. No se puede confirmar que el historial esté completo.')
        // Validar antes de incluir una página evita omitir importes defectuosos silenciosamente.
        activityReport(response.activities)
        response.activities.forEach(row => rowsRef.current.set(row.id, row))
        setActivities([...rowsRef.current.values()])
        setLoadedAt(new Date().toISOString())
        if (!response.nextPageToken) { setComplete(true); cursor.current = ''; return }
        cursors.current.add(response.nextPageToken)
        cursor.current = response.nextPageToken
      }
    } catch (err) { if (generation.current === version) setLoadError(errorMessage(err)) }
    finally { if (generation.current === version) { running.current = false; setLoading(false) } }
  }
  useEffect(() => {
    if (source === 'paper') void loadHistory(true)
    else { generation.current++; running.current = false; setLoading(false); setLoadError('') }
    return () => { generation.current++; running.current = false }
  }, [source])
  useEffect(() => { setPage(1) }, [period.start, period.end, grouping, tab, source])

  const normalized = useMemo(() => {
    try { return { data: source === 'local' ? localReport(account.trades) : activityReport(activities), error: '' } }
    catch (err) { return { data: { transactions: [], fees: [], excluded: 0 }, error: errorMessage(err) } }
  }, [source, account.trades, activities])
  const data = useMemo(() => filterReport(normalized.data, period.start, period.end), [normalized.data, period.start, period.end])
  const totals = useMemo(() => summarize(data), [data])
  const groups = useMemo(() => groupReport(data, grouping), [data, grouping])
  const ready = !periodError && !normalized.error && (source === 'local' || (complete && !loadError && !loading))
  const commissionRows = platformCommissions(data)
  const itemsCount = tab === 'summary' ? groups.length : ['transactions','commissions'].includes(tab) ? data.transactions.length : data.fees.length
  const pages = Math.max(1, Math.ceil(itemsCount / PAGE_SIZE))
  const currentPage = Math.min(page, pages)
  const sliceStart = (currentPage - 1) * PAGE_SIZE
  const sourceName = source === 'local' ? 'Historial local anterior' : 'Alpaca Paper'
  const unlinked = data.fees.filter(f => !f.orderId).length
  const priceDisplay = (value: string) => new Intl.NumberFormat('es-CO', { maximumFractionDigits: 12 }).format(Number(value))
  function exportCSV(kind: Tab) {
    if (!ready) return
    const csv = kind === 'commissions' ? commissionCsv(data, period.start, period.end, sourceName, account.profile.email) : reportCsv(data, period.start, period.end, sourceName, kind, grouping)
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8;' }))
    const link = document.createElement('a')
    link.href = url; link.download = `soltech-${source}-${kind === 'summary' ? 'resumen' : kind === 'fees' ? 'cargos-proveedor' : kind === 'commissions' ? 'comision-soltech-estimada' : 'transacciones'}-${period.start}-${period.end}.csv`
    document.body.appendChild(link); link.click(); link.remove()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
    notify('Reporte CSV descargado con todos los registros del período, no solo la página visible.')
  }
  return <section className="reports-workspace" aria-label="Reportes de transacciones y comisiones">
    <article className="report-hero"><div><span className="section-tag">CADA MOVIMIENTO CUENTA</span><h2>Tus números, con claridad.</h2><p>Revisa tu actividad, entiende tus cargos y lleva el control de cada período.</p></div><span className="report-hero-icon"><Icon name="orders" size={40}/></span></article>
    <article className="card report-controls"><div className="card-heading"><div><h3>Elige qué quieres consultar</h3><p>Importes en USD · Ejecuciones en hora de Colombia (America/Bogota).</p></div><span className="pill">{sourceName}</span></div><div className="report-filters"><label>Origen de los datos<select value={source} onChange={e => setSource(e.target.value as 'local' | 'paper')}><option value="local">Historial local anterior</option><option value="paper">Alpaca Paper</option></select></label><label>Período<select value={preset} onChange={e => { setPreset(e.target.value); if (e.target.value !== 'personalizado') setPeriod(presetPeriod(e.target.value)) }}><option value="hoy">Hoy</option><option value="7dias">Últimos 7 días</option><option value="mes">Este mes</option><option value="anterior">Mes anterior</option><option value="anio">Este año</option><option value="personalizado">Personalizado</option></select></label><label>Desde<input type="date" required value={period.start} onChange={e => { setPreset('personalizado'); setPeriod(p => ({ ...p, start: e.target.value })) }}/></label><label>Hasta<input type="date" required value={period.end} onChange={e => { setPreset('personalizado'); setPeriod(p => ({ ...p, end: e.target.value })) }}/></label><label>Agrupar por<select value={grouping} onChange={e => setGrouping(e.target.value as 'day' | 'month')}><option value="day">Día</option><option value="month">Mes</option></select></label></div>{periodError && <p className="form-error" role="alert">{periodError}</p>}{normalized.error && <p className="form-error" role="alert">{normalized.error}</p>}
      <div className="report-source-info"><Icon name={source === 'local' ? 'help' : 'shield'} size={17}/><p>{source === 'local' ? 'La simulación local no cobra comisiones. Los importes corresponden exclusivamente a registros anteriores guardados en este navegador. Este modo no permite crear nuevas operaciones ni sustituye el mercado conectado.' : 'Se consulta el historial completo de ejecuciones y cargos de tu cuenta paper, incluyendo operaciones externas a Soltech. Los cargos usan la fecha contable informada por Alpaca y pueden publicarse después de la operación.'}</p>{source === 'paper' && <button className="small-button" disabled={loading} onClick={() => void loadHistory(true)}>Actualizar historial</button>}</div>
      {source === 'paper' && <div className={`report-loading ${complete ? 'completed' : ''}`} role="status"><span>{loading ? 'Consultando Alpaca…' : complete ? 'Historial cargado' : 'Historial pendiente o parcial'} · {activities.length} actividades{loadedAt && ` · ${new Intl.DateTimeFormat('es-CO', { timeZone: 'America/Bogota', hour: '2-digit', minute: '2-digit' }).format(new Date(loadedAt))}`}</span>{!complete && !loading && <button className="text-link" onClick={() => void loadHistory()}>{loadError ? 'Reintentar carga' : 'Continuar carga'} <Icon name="arrow" size={15}/></button>}</div>}
      {loadError && source === 'paper' && <p className="form-error" role="alert">{loadError} Revisa la configuración de Alpaca Paper en el README. Los reportes y exportaciones se habilitan al completar la carga.</p>}
      {normalized.data.excluded > 0 && <div className="demo-note">Se excluyeron {normalized.data.excluded} actividades con moneda distinta de USD. Este reporte no convierte divisas.</div>}
    </article>
    <div className="report-stats">{[
      { label: 'Transacciones ejecutadas', value: ready ? String(totals.count) : '—', detail: 'Cada ejecución parcial cuenta por separado', icon: 'orders' },
      { label: 'Volumen operado', value: ready ? formatUSD(totals.volume) : '—', detail: 'Compras + ventas, sin descontar cargos', icon: 'chart' },
      { label: 'Comisiones y cargos netos', value: ready ? formatUSD(totals.netFees) : '—', detail: source === 'local' ? 'Sin comisiones en la demostración' : `${data.fees.length} cargos o reembolsos reportados`, icon: 'wallet' },
      { label: 'Flujo neto del período', value: ready ? formatUSD(totals.netCash) : '—', detail: 'Ventas − compras − cargos + reembolsos', icon: 'arrow' },
    ].map(stat => <article className="stat-card" key={stat.label}><div><span>{stat.label}</span><Icon name={stat.icon} size={18}/></div><strong>{stat.value}</strong><p>{stat.detail}</p></article>)}</div>
    {source === 'paper' && complete && ready && data.fees.length === 0 && <div className="demo-note"><Icon name="help" size={17}/><span>No hay cargos reportados por Alpaca para este período. Esto no confirma ausencia de comisiones: pueden registrarse más tarde. No se estiman tarifas ni se asignan comisiones a ejecuciones sin respaldo del proveedor.</span></div>}
    <article className="card report-details"><div className="card-heading"><div><h3>El detalle detrás de cada movimiento</h3><p>{period.start || '—'} → {period.end || '—'} · {sourceName}</p></div><div className="report-actions"><button className="button button-primary" disabled={!ready} onClick={() => exportCSV(tab)}>Descargar CSV <Icon name="arrow" size={16}/></button></div></div>
      <div className="report-tabs" role="tablist" aria-label="Tipo de reporte">{tabs.map(([value, label]) => <button key={value} role="tab" aria-selected={tab === value} aria-controls="report-panel" id={`report-tab-${value}`} tabIndex={tab === value ? 0 : -1} onClick={() => setTab(value)} onKeyDown={e => { if (['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(e.key)) { e.preventDefault(); const index = tabs.findIndex(([t]) => t === tab); const next = e.key === 'Home' ? 0 : e.key === 'End' ? tabs.length - 1 : (index + (e.key === 'ArrowRight' ? 1 : -1) + tabs.length) % tabs.length; setTab(tabs[next][0]); document.getElementById(`report-tab-${tabs[next][0]}`)?.focus() } }}>{label}</button>)}</div>
      <div role="tabpanel" id="report-panel" aria-labelledby={`report-tab-${tab}`} tabIndex={0}>
        {!ready ? <div className="empty-state"><span className="empty-icon"><Icon name="orders" size={25}/></span><strong>{periodError ? 'Revisa el período seleccionado' : normalized.error ? 'Revisa el historial recibido' : loading ? 'Preparando tu reporte…' : 'Completa la carga del historial'}</strong><p>Solo se muestran totales y archivos de un historial cargado completamente.</p></div> : <>
          {tab === 'commissions' && <><div className="demo-note">RF-30 / RF-65: desglose de la regla del 2 % por ejecución, redondeada a dos decimales. Usuario de prototipo: {account.profile.email}. Estas comisiones son estimadas, no cobradas. Falta el libro transaccional de comisiones por orden y usuario; no se confunden con los cargos de Alpaca ni se ajusta su saldo.</div><div className="report-breakdown"><div><span>Comisión estimada del período</span><strong>{new Intl.NumberFormat('es-CO',{style:'currency',currency:'USD'}).format(commissionRows.reduce((sum,t)=>sum+Math.round(t.commission*100),0)/100)}</strong></div><div><span>Participación de Soltech sin comisionista</span><strong>100 %</strong></div></div><div className="table-scroll"><table><thead><tr><th>Fecha</th><th>Acción</th><th>Operación</th><th>Valor bruto</th><th>Tasa</th><th>Comisión estimada</th><th>Total / neto</th><th>Estado</th></tr></thead><tbody>{commissionRows.slice(sliceStart,sliceStart+PAGE_SIZE).map(t=><tr key={t.id}><td>{t.date}</td><td>{t.symbol}</td><td>{t.side}</td><td>{t.gross.toFixed(2)} USD</td><td>2 %</td><td>{t.commission.toFixed(2)} USD</td><td>{t.total.toFixed(2)} USD</td><td>No cobrada</td></tr>)}</tbody></table></div></>}
          {tab === 'summary' && <><div className="report-breakdown"><div><span>Compras</span><strong>{formatUSD(totals.purchases)}</strong></div><div><span>Ventas</span><strong>{formatUSD(totals.sales)}</strong></div><div><span>Cargos</span><strong>{formatUSD(totals.charges)}</strong></div><div><span>Reembolsos</span><strong>{formatUSD(totals.refunds)}</strong></div></div><div className="table-scroll"><table><thead><tr><th>{grouping === 'day' ? 'Día' : 'Mes'}</th><th>Ejecuciones</th><th>Compras</th><th>Ventas</th><th>Cargos</th><th>Reembolsos</th><th>Cargos netos</th><th>Flujo neto</th></tr></thead><tbody>{groups.slice(sliceStart, sliceStart + PAGE_SIZE).map(g => <tr key={g.period}><td><strong>{g.period}</strong></td><td>{g.count}</td><td>{formatUSD(g.purchases)}</td><td>{formatUSD(g.sales)}</td><td>{formatUSD(g.charges)}</td><td>{formatUSD(g.refunds)}</td><td>{formatUSD(g.netFees)}</td><td className={g.netCash < 0n ? 'negative' : 'positive'}>{formatUSD(g.netCash)}</td></tr>)}</tbody></table></div></>}
          {tab === 'transactions' && <><p className="report-table-note">Importes brutos de las ejecuciones. Los cargos se detallan en su pestaña; no se deducen ni se vinculan por aproximación.</p><div className="table-scroll"><table><thead><tr><th>Fecha (Colombia)</th><th>Activo</th><th>Operación</th><th>Cantidad</th><th>Precio USD</th><th>Importe bruto USD</th><th>ID ejecución / orden</th></tr></thead><tbody>{data.transactions.slice(sliceStart, sliceStart + PAGE_SIZE).map(t => <tr key={t.id}><td>{new Intl.DateTimeFormat('es-CO', { timeZone: 'America/Bogota', dateStyle: 'short', timeStyle: 'short' }).format(new Date(t.timestamp))}</td><td><strong>{t.symbol}</strong></td><td className={t.side === 'Compra' ? 'positive' : 'negative'}>{t.side}</td><td>{priceDisplay(t.quantity)}</td><td>{priceDisplay(t.price)}</td><td>{formatUSD(t.grossMicros)}</td><td><details><summary>Ver identificadores</summary><code>{t.id}</code><code>{t.orderId || 'Sin ID de orden'}</code></details></td></tr>)}</tbody></table></div></>}
          {tab === 'fees' && <><p className="report-table-note">Desglose de cargos y reembolsos por fecha contable del proveedor. {unlinked > 0 ? `${unlinked} registros no incluyen una orden asociada.` : source === 'local' ? 'La demostración no aplica cargos.' : 'No se estiman comisiones faltantes.'}</p>{data.fees.length > 0 && <div className="report-fee-categories">{['FEE', 'DIVFEE', 'PTC', 'PTR'].filter(type => data.fees.some(f => f.type === type)).map(type => <div key={type}><span>{feeLabel(type)}</span><strong>{formatUSD(-data.fees.filter(f => f.type === type).reduce((sum, f) => sum + f.netMicros, 0n))}</strong><small>Cargo neto · {type}</small></div>)}</div>}<div className="table-scroll"><table><thead><tr><th>Fecha contable</th><th>Tipo / descripción</th><th>Activo</th><th>Cargo USD</th><th>Reembolso USD</th><th>Orden asociada</th></tr></thead><tbody>{data.fees.slice(sliceStart, sliceStart + PAGE_SIZE).map(f => <tr key={f.id}><td>{f.date}</td><td><strong>{feeLabel(f.type)}</strong><small className="report-description">{f.description}</small></td><td>{f.symbol || 'De cuenta'}</td><td>{formatUSD(f.netMicros < 0n ? -f.netMicros : 0n)}</td><td>{formatUSD(f.netMicros > 0n ? f.netMicros : 0n)}</td><td>{f.orderId ? <details><summary>Ver orden</summary><code>{f.orderId}</code><code>Cargo: {f.id}</code></details> : <span className="muted">No informada</span>}</td></tr>)}</tbody></table></div></>}
          {itemsCount === 0 && <div className="empty-state"><span className="empty-icon"><Icon name={tab === 'fees' ? 'wallet' : 'orders'} size={26}/></span><strong>{tab === 'fees' ? source === 'local' ? 'Sin comisiones en la demo' : 'Sin cargos reportados en este período' : 'Sin movimientos en este período'}</strong><p>{tab === 'fees' ? 'Puedes descargar el CSV con los encabezados del reporte.' : 'Prueba otro intervalo de fechas o realiza una operación para consultar su detalle.'}</p></div>}
          <div className="report-pagination"><span>{itemsCount ? `${sliceStart + 1}–${Math.min(sliceStart + PAGE_SIZE, itemsCount)} de ${itemsCount}` : '0 registros'} · Exportación completa del período</span><div><button className="small-button" disabled={currentPage <= 1} onClick={() => setPage(currentPage - 1)}>Anterior</button><span>{currentPage} / {pages}</span><button className="small-button" disabled={currentPage >= pages} onClick={() => setPage(currentPage + 1)}>Siguiente</button></div></div>
        </>}
      </div>
    </article>
    <div className="report-footnote"><Icon name="help" size={15}/><p>El flujo neto refleja entradas y salidas por las operaciones y cargos incluidos; no representa rentabilidad, saldo final ni incluye fondeos, retiros, dividendos o intereses. Los reportes de Alpaca se limitan a ejecuciones y actividades FEE, DIVFEE, PTC y PTR en USD. Corte de consulta: {source === 'local' ? dateInBogota(new Date()) : loadedAt ? dateInBogota(loadedAt) : 'pendiente'}. El CSV conserva hasta seis decimales en los importes monetarios.</p></div>
  </section>
}
