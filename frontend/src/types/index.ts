export type Step = 'registro' | 'correo' | 'mfa' | 'alpaca' | 'fondeo' | 'dashboard' | 'login'
export interface Profile { name: string; surname: string; email: string; country: string; document?: string; phone?: string; level?: 'Principiante' | 'Experimentado'; markets?: string[]; sectors?: string[]; premiumInterest?: boolean; monthlyReport?: boolean }
export interface Trade { id: string; symbol: string; name: string; quantity: number; price: number; side: 'Compra' | 'Venta'; date: string }
export interface Account { profile: Profile; step: Step; balance: number; deposit: number; trades: Trade[]; watchlist: string[]; accountId: string }
