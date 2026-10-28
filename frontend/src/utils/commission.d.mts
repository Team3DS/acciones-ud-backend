export function tradeAmounts(price: number | string, quantity: number, rateBps?: number, side?: 'buy' | 'sell'): { gross: number; commission: number; total: number; rateBps: number }
