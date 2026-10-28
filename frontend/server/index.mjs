import './env.mjs'
import { createServer } from 'node:http'
import { readFile } from 'node:fs/promises'
import { resolve, extname, sep } from 'node:path'
import { createApiHandler } from './api.mjs'

const api = createApiHandler()
const root = resolve('dist')
const mime = { '.html': 'text/html; charset=utf-8', '.js': 'text/javascript', '.css': 'text/css', '.svg': 'image/svg+xml', '.png': 'image/png' }
const port = Number(process.env.PORT || 3001)
createServer(async (req, res) => {
  if (req.url?.startsWith('/api/')) return api(req, res)
  try {
    const pathname = decodeURIComponent(new URL(req.url, 'http://localhost').pathname)
    const file = resolve(root, `.${pathname === '/' ? '/index.html' : pathname}`)
    if (!file.startsWith(root + sep)) { res.writeHead(403); res.end(); return }
    const content = await readFile(file)
    res.writeHead(200, { 'Content-Type': mime[extname(file)] || 'application/octet-stream', 'X-Content-Type-Options': 'nosniff' }); res.end(content)
  } catch { res.writeHead(404); res.end('Archivo no encontrado. Ejecuta npm run build primero.') }
}).listen(port, '127.0.0.1', () => console.log(`Soltech local: http://127.0.0.1:${port}`))
