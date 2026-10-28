// RF-30 / RNF-14: importes en centavos y redondeo half-up.
export function tradeAmounts(price, quantity, rateBps = 200, side = 'buy') {
  const raw = String(price)
  if (!/^\d+(\.\d{1,12})?$/.test(raw) || !Number.isSafeInteger(quantity) || quantity < 1 || !Number.isInteger(rateBps) || rateBps < 0 || rateBps > 10000 || !['buy', 'sell'].includes(side)) throw new Error('Precio, cantidad o comisión inválidos.')
  const [whole, fraction = ''] = raw.split('.')
  const divisor = 10n ** BigInt(fraction.length)
  const grossCents = (BigInt(whole + fraction) * BigInt(quantity) * 100n + divisor / 2n) / divisor
  const feeCents = (grossCents * BigInt(rateBps) + 5000n) / 10000n
  const totalCents = side === 'buy' ? grossCents + feeCents : grossCents - feeCents
  if (grossCents > BigInt(Number.MAX_SAFE_INTEGER) || totalCents > BigInt(Number.MAX_SAFE_INTEGER)) throw new Error('El importe supera el máximo admitido.')
  return { gross: Number(grossCents) / 100, commission: Number(feeCents) / 100, total: Number(totalCents) / 100, rateBps }
}
