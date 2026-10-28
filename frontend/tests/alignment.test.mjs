import test from 'node:test'
import assert from 'node:assert/strict'
import { tradeAmounts } from '../src/utils/commission.mjs'
import { platformCommissions, commissionCsv, activityReport } from '../src/utils/reports.ts'

test('RF-30: compra suma 2 % y venta lo resta, con dos decimales',()=>{
  assert.deepEqual(tradeAmounts('227.63',3,200,'buy'),{gross:682.89,commission:13.66,total:696.55,rateBps:200})
  assert.equal(tradeAmounts('227.63',3,200,'sell').total,669.23)
})
test('RNF-14: half-up y porcentaje parametrizable sin errores de centavos',()=>{
  assert.equal(tradeAmounts('0.25',1).commission,.01)
  assert.equal(tradeAmounts('100',2,150).commission,3)
  assert.throws(()=>tradeAmounts('100',1.5));assert.throws(()=>tradeAmounts('100',1,-5))
})
test('RF-65: el reporte distingue una estimación de una comisión efectivamente cobrada',()=>{
  const data=activityReport([{id:'fill',activity_type:'FILL',symbol:'AAPL',side:'buy',qty:'3',price:'227.63',order_id:'order',transaction_time:'2026-10-17T16:00:00Z'}])
  assert.equal(platformCommissions(data)[0].commission,13.66)
  const csv=commissionCsv(data,'2026-10-01','2026-10-31','Alpaca Paper','usuario@ejemplo.com')
  assert.ok(csv.includes('13.66'));assert.ok(csv.includes('no cobrada'));assert.ok(csv.includes('usuario@ejemplo.com'))
})
