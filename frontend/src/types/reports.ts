export interface ReportTransaction { id: string; orderId: string; symbol: string; date: string; timestamp: string; side: 'Compra' | 'Venta'; quantity: string; price: string; grossMicros: bigint }
export interface ReportFee { id: string; orderId: string; symbol: string; date: string; type: string; description: string; netMicros: bigint }
export interface ReportData { transactions: ReportTransaction[]; fees: ReportFee[]; excluded: number }
export interface PaperActivity { id: string; activity_type: string; transaction_time?: string; date?: string; symbol?: string; side?: string; qty?: string; price?: string; order_id?: string; net_amount?: string; description?: string; currency?: string }
export interface ActivityPage { activities: PaperActivity[]; nextPageToken: string | null }
