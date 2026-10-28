import { ApiError, createBroker } from './alpaca.mjs'

export function createApiHandler(broker = createBroker()) {
  return async function api(req, res) {
    const send = (status, data) => { res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff' }); res.end(JSON.stringify(data)) }
    try {
      const host = req.headers.host || ''
      if (!/^(localhost|127\.0\.0\.1)(:\d+)?$/.test(host)) throw new ApiError(403, 'Esta API es de uso local. No la expongas a la red.')
      const origin = req.headers.origin
      if ((origin && origin !== `http://${host}`) || req.headers['sec-fetch-site'] === 'cross-site') throw new ApiError(403, 'Origen de solicitud no permitido.')
      const url = new URL(req.url, `http://${host}`)
      if (req.method === 'GET' && url.pathname === '/api/paper/status') return send(200, { configured: broker.configured, feed: broker.feed, mode: 'paper' })
      if (req.method === 'GET' && url.pathname === '/api/paper/snapshot') return send(200, await broker.snapshot())
      if (req.method === 'GET' && url.pathname === '/api/paper/activities') return send(200, await broker.activities(url.searchParams.get('pageToken') || ''))
      if (req.method === 'GET' && url.pathname === '/api/paper/quote') return send(200, await broker.quote(url.searchParams.get('symbol') || '',1400,true))
      if (req.method === 'GET' && url.pathname === '/api/paper/order') {
        const order = await broker.findOrder(url.searchParams.get('clientOrderId') || '')
        return send(order ? 200 : 404, order ? { order } : { message: 'Aún no hay confirmación de esa orden. No la envíes de nuevo.', code: 'ORDER_NOT_FOUND' })
      }
      if (req.method === 'POST' && url.pathname === '/api/paper/cancel') {
        if (origin !== `http://${host}` || !req.headers['content-type']?.startsWith('application/json')) throw new ApiError(403, 'Se requiere una solicitud JSON del mismo origen.')
        let body = ''
        for await (const chunk of req) { body += chunk; if (body.length > 2048) throw new ApiError(413, 'Solicitud demasiado grande.') }
        let input
        try { input = JSON.parse(body) } catch { throw new ApiError(400, 'Solicitud JSON inválida.') }
        return send(202, await broker.cancel(input.clientOrderId))
      }
      if (req.method === 'POST' && url.pathname === '/api/paper/orders') {
        if (origin !== `http://${host}` || !req.headers['content-type']?.startsWith('application/json')) throw new ApiError(403, 'Se requiere una solicitud JSON del mismo origen.')
        let body = ''
        for await (const chunk of req) { body += chunk; if (body.length > 2048) throw new ApiError(413, 'La solicitud es demasiado grande.') }
        let input
        try { input = JSON.parse(body) } catch { throw new ApiError(400, 'La solicitud JSON es inválida.') }
        const result = await broker.submit(input)
        return send(result.order?.status === 'filled' ? 200 : 202, result)
      }
      return send(404, { message: 'Ruta de API no encontrada.' })
    } catch (err) { return send(err instanceof ApiError ? err.status : 500, { message: err instanceof ApiError ? err.message : 'No se pudo completar la solicitud.', code: err.code || 'INTERNAL_ERROR' }) }
  }
}
