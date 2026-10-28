import { useState } from 'react'
import { Icon } from '@/components/Icon'
import PreviewDialog from '@/components/PreviewDialog'
import { money } from '@/services/market'
import { paperAssets } from '@/services/paper'
import { tradeAmounts } from '@/utils/commission.mjs'

export function FundingPreview({ cash, onClose }: { cash?: number; onClose: () => void }) {
  const [value, setValue] = useState('10000')
  const [review, setReview] = useState(false)
  const amount = Number(value)
  const valid = Number.isFinite(amount) && amount >= 1 && amount <= 1000000 && /^\d+(\.\d{1,2})?$/.test(value)
  return <PreviewDialog title={review ? 'Revisa tu abono de prueba' : 'Más espacio para practicar'} onClose={onClose}>
    <p className="preview-intro">Prepara un fondeo adicional en USD. Este recorrido muestra el diseño del abono.</p>
    {!review ? <form onSubmit={event => { event.preventDefault(); if (valid) setReview(true) }}>
      <label>Monto en USD<input autoFocus type="number" min="1" max="1000000" step="0.01" required value={value} onChange={event => setValue(event.target.value)}/></label>
      <div className="amount-options">{[1000, 5000, 10000].map(preset => <button type="button" key={preset} className={amount === preset ? 'selected' : ''} onClick={() => setValue(String(preset))}>{money(preset)}</button>)}</div>
      <p className="preview-caption">Entre USD 1 y USD 1.000.000, con hasta dos decimales.</p>
      <div className="demo-note">Vista de diseño: no se modifican saldos ni se envían solicitudes a Alpaca.</div>
      <button className="button button-primary full" disabled={!valid}>Revisar abono <Icon name="arrow" size={17}/></button>
    </form> : <>
      <dl className="preview-breakdown"><div><dt>Efectivo consultado</dt><dd>{cash === undefined ? 'Por consultar' : money(cash)}</dd></div><div><dt>Abono propuesto</dt><dd>{money(amount)}</dd></div><div><dt>Saldo proyectado</dt><dd>{cash === undefined ? 'Por consultar' : money(cash + amount)}</dd></div></dl>
      <div className="demo-note">El abono requiere la integración Broker sandbox. Esta proyección no representa un movimiento registrado.</div>
      <button className="button button-primary full" disabled>Confirmación de abono pendiente</button>
      <div className="preview-actions"><button className="text-link" onClick={() => setReview(false)}>Editar monto</button><button className="text-link" onClick={onClose}>Cerrar vista previa</button></div>
    </>}
  </PreviewDialog>
}

export function FundsScreen({ cash, onAdd }: { cash?: number; onAdd: () => void }) {
  return <div className="basic-screen">
    <div className="preview-hero"><div><span className="section-tag">TU CUENTA DE PRÁCTICA</span><h2>Dale impulso a tu próximo paso.</h2><p>Consulta el efectivo de tu cuenta y explora cómo sería un abono adicional.</p><button className="button button-primary" onClick={onAdd}><Icon name="plus" size={17}/> Preparar abono</button></div><Icon name="wallet" size={72}/></div>
    <div className="workspace-overview"><article className="card"><div className="card-heading"><h3>Efectivo en Alpaca Paper</h3><span className="pill">USD</span></div><strong className="preview-balance">{cash === undefined ? 'Por consultar' : money(cash)}</strong><p className="workspace-copy">{cash === undefined ? 'Conecta Alpaca para consultar el efectivo de la cuenta.' : 'Importe consultado en el proveedor. El abono de diseño no cambia este saldo.'}</p></article><article className="card"><h3>Así será el recorrido</h3><ol className="preview-steps"><li>Elige un monto de prueba.</li><li>Revisa el abono y el saldo proyectado.</li><li>Confirma cuando la integración esté disponible.</li></ol></article></div>
    <article className="card"><div className="card-heading"><div><h3>Historial de movimientos</h3><p>Abonos y saldos de la cuenta sandbox.</p></div><span className="pill">Diseño</span></div><div className="table-scroll"><table><thead><tr><th>Fecha</th><th>Movimiento</th><th>Monto USD</th><th>Estado</th></tr></thead><tbody><tr><td colSpan={4}><div className="empty-state"><Icon name="wallet" size={27}/><p>Aún no hay movimientos de fondeo integrados.</p></div></td></tr></tbody></table></div><p className="report-table-note">El registro y la conciliación de abonos están pendientes. Las vistas previas no generan movimientos.</p></article>
  </div>
}

export function HelpScreen({ navigate }: { navigate: (section: string) => void }) {
  const [search, setSearch] = useState('')
  const guides = [
    { icon: 'shield', title: 'Accede con seguridad', body: 'Registra tus datos, verifica tu correo y completa el código MFA. La autenticación y el envío de correo actuales son demostrativos.', section: 'Mi perfil', action: 'Ver mi perfil' },
    { icon: 'chart', title: 'Prepara tu primera operación', body: 'Consulta una acción de NYSE o NASDAQ. Elige compra o venta, cantidad y orden al mercado o límite. Revisa los importes antes de confirmar.', section: 'Mercado y órdenes', action: 'Explorar el mercado' },
    { icon: 'orders', title: 'Sigue cada paso', body: 'Revisa la cantidad ejecutada y el estado reportado por Alpaca. Una orden recibida puede seguir pendiente. La cancelación también necesita confirmación.', section: 'Mis órdenes', action: 'Consultar mis órdenes' },
    { icon: 'wallet', title: 'Entiende tus resultados', body: 'Consulta posiciones y reportes por período. La comisión Soltech estimada del 2 % se presenta separada de los cargos del proveedor.', section: 'Reportes', action: 'Abrir reportes' },
  ]
  const questions = [
    ['¿Estoy usando dinero real?', 'Las operaciones se dirigen a Alpaca Paper con fondos virtuales. El prototipo no habilita operaciones live.'],
    ['¿Por qué no aparecen precios o saldos?', 'Es necesario configurar una cuenta Alpaca Paper en el servidor. Sin conexión se muestran datos por consultar. Tu equipo encontrará los pasos en el README del proyecto.'],
    ['¿Qué pasa si el mercado está cerrado?', 'Puedes aceptar expresamente el envío de una orden para la próxima apertura. El horario regular es de 9:30 a 16:00 de Nueva York; el calendario del proveedor considera festivos y cambios de horario.'],
    ['¿El fondeo y las órdenes de protección funcionan?', 'Son vistas de frontend. Puedes preparar y revisar sus formularios, pero no se envían abonos ni órdenes Stop Loss o Take Profit.'],
    ['¿Ya se cobran las comisiones o Premium?', 'La comisión del 2 % es una estimación. El cobro, las suscripciones y la evaluación de alertas están pendientes de integración.'],
  ].filter(([question, answer]) => `${question} ${answer}`.toLocaleLowerCase('es').includes(search.trim().toLocaleLowerCase('es')))
  return <div className="basic-screen"><div className="preview-hero"><div><span className="section-tag">CENTRO DE AYUDA</span><h2>Estamos contigo en cada paso.</h2><p>Encuentra una guía para explorar Soltech y entender lo que ves en tu cuenta.</p></div><Icon name="sun" size={75}/></div><div className="help-guide-grid">{guides.map(guide => <article className="card help-guide" key={guide.title}><span className="preview-icon"><Icon name={guide.icon} size={23}/></span><h3>{guide.title}</h3><p>{guide.body}</p><button className="text-link" onClick={() => navigate(guide.section)}>{guide.action} <Icon name="arrow" size={15}/></button></article>)}</div><article className="card"><div className="card-heading"><h3>Preguntas frecuentes</h3></div><label className="help-search">Busca una respuesta<input type="search" placeholder="Fondos, mercado, comisiones…" value={search} onChange={event => setSearch(event.target.value)}/></label><div className="help-questions">{questions.map(([question, answer]) => <details key={question}><summary>{question}</summary><p>{answer}</p></details>)}</div>{!questions.length && <div className="empty-state">No encontramos una respuesta. Prueba con otra palabra.</div>}</article></div>
}

export function ProtectionScreen() {
  const [kind, setKind] = useState('Stop Loss')
  const [symbol, setSymbol] = useState('AAPL')
  const [quantity, setQuantity] = useState('1')
  const [price, setPrice] = useState('220')
  const [validity, setValidity] = useState('Hasta cancelación (GTC)')
  const [review, setReview] = useState(false)
  const count = Number(quantity), threshold = Number(price)
  const valid = Number.isSafeInteger(count) && count > 0 && count <= 1000000 && /^\d+(\.\d{1,2})?$/.test(price) && threshold > 0 && threshold <= 1000000
  const amounts = valid ? tradeAmounts(threshold, count) : null
  return <div className="basic-screen"><div className="preview-hero"><div><span className="section-tag">ÓRDENES DE PROTECCIÓN · DISEÑO</span><h2>Planea cuándo dar el siguiente paso.</h2><p>Explora una venta Stop Loss o Take Profit. Los formularios son una vista previa y no envían órdenes.</p></div><Icon name="shield" size={75}/></div><div className="workspace-overview"><article className="card"><div className="trade-tabs">{['Stop Loss', 'Take Profit'].map(value => <button key={value} className={kind === value ? 'selected' : ''} onClick={() => { setKind(value); setPrice(value === 'Stop Loss' ? '220' : '260') }}>{value}</button>)}</div><p className="workspace-copy">{kind === 'Stop Loss' ? 'Diseña una venta que se active si el precio cae al umbral elegido.' : 'Diseña una venta que se active si el precio sube al objetivo elegido.'}</p><form onSubmit={event => { event.preventDefault(); if (valid) setReview(true) }}><label>Acción<select value={symbol} onChange={event => setSymbol(event.target.value)}>{paperAssets.map(asset => <option key={asset.symbol} value={asset.symbol}>{asset.symbol} · {asset.name}</option>)}</select></label><div className="field-row"><label>Cantidad a vender<input type="number" min="1" max="1000000" step="1" required value={quantity} onChange={event => setQuantity(event.target.value)}/></label><label>Precio de activación USD<input type="number" min="0.01" max="1000000" step="0.01" required value={price} onChange={event => setPrice(event.target.value)}/></label></div><label>Vigencia<select value={validity} onChange={event => setValidity(event.target.value)}><option>Día (DAY)</option><option>Hasta cancelación (GTC)</option></select></label><button className="button button-primary full" disabled={!valid}>Revisar diseño de orden <Icon name="arrow" size={17}/></button></form></article><article className="card"><span className="preview-icon"><Icon name="help" size={25}/></span><h3>Antes de activar una protección</h3><ol className="preview-steps"><li>Verificar que tienes acciones disponibles.</li><li>Validar el umbral y la cotización actual.</li><li>Revisar el precio de ejecución y la comisión.</li></ol><div className="demo-note">La tenencia y el umbral no se validan contra el proveedor en esta maqueta. El precio de activación no garantiza el precio de ejecución.</div></article></div>{review && amounts && <PreviewDialog title={`Revisar ${kind}`} onClose={() => setReview(false)}><p className="preview-intro">Venta de {quantity} {symbol} · {validity}</p><dl className="preview-breakdown"><div><dt>Activación</dt><dd>{kind === 'Stop Loss' ? 'Al caer a' : 'Al subir a'} {money(threshold)}</dd></div><div><dt>Valor al umbral</dt><dd>{money(amounts.gross)}</dd></div><div><dt>Comisión estimada 2 %</dt><dd>{money(amounts.commission)}</dd></div><div><dt>Neto de referencia</dt><dd>{money(amounts.gross - amounts.commission)}</dd></div></dl><div className="demo-note">Cálculo de diseño al precio elegido. No es una cotización ni una orden enviada. La comisión no se cobra.</div><button className="button button-primary full" disabled>Activación pendiente de integración</button><button className="text-link preview-back" onClick={() => setReview(false)}>Volver a editar</button></PreviewDialog>}</div>
}
