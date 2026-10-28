export interface PaperQuote { symbol: string; exchange: 'NYSE' | 'NASDAQ'; ask: number | null; bid: number | null; last: number | null; timestamp: string; source: string; stale: boolean; indicative: boolean; spread?: number | null; volume?: number | null; dailyChange?: number | null; marketCap?: number | null }
export interface PaperOrder { id: string; clientOrderId: string; symbol: string; side: 'buy' | 'sell'; quantity: number; filledQuantity: number; filledPrice: number | null; status: string; submittedAt: string | null; filledAt: string | null; type?: string; limitPrice?: number | null; createdAt?: string | null; canceledAt?: string | null; expiredAt?: string | null }
export interface PaperSnapshot {
  account: { id: string; number: string; cash: number; buyingPower: number; equity: number; blocked: boolean; status: string }
  positions: { symbol: string; quantity: number; available: number; averagePrice: number; currentPrice: number; marketValue: number; unrealizedPL: number }[]
  orders: PaperOrder[]
  clock: { isOpen: boolean; timestamp: string; nextOpen: string; nextClose: string }
  updatedAt: string
}
export interface PaperResult { order: PaperOrder | null; clientOrderId: string; elapsedMs: number; uncertain: boolean }
