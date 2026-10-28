import type { Account } from '@/types'
export const initialAccount: Account = { profile: { name: '', surname: '', email: '', country: 'Colombia' }, step: 'registro', balance: 0, deposit: 0, trades: [], watchlist: ['AAPL', 'NVDA', 'MSFT'], accountId: '' }
export function readAccount(): Account {
  try { const saved = JSON.parse(localStorage.getItem('soltech.account') || 'null'); return saved && saved.profile && Array.isArray(saved.trades) && ['registro', 'correo', 'mfa', 'alpaca', 'fondeo', 'dashboard', 'login'].includes(saved.step) ? { ...initialAccount, ...saved } : initialAccount } catch { return initialAccount }
}
