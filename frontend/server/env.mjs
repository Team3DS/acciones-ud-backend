import { existsSync } from 'node:fs'
// Node carga las claves exclusivamente en el proceso del servidor.
if (existsSync('.env')) process.loadEnvFile('.env')
