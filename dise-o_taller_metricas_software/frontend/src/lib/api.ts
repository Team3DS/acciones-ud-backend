export type Rol = 'CLIENTE' | 'EMPLEADO';

export type Comida = {
	id: number;
	nombre: string;
	precio: number;
	imagenURL?: string | null;
};

export type Configuracion = {
	id: number;
	ingrediente: {
		id: number;
		nombreIngrediente: string;
		unidadMedida: string;
	};
	cantidadBase: number;
	incluidoPorDefecto: boolean;
	permiteAdicional: boolean;
	permiteRetirar: boolean;
	precioAdicional: number;
};

export type ComidaDetalle = Comida & { ingredientes: Configuracion[] };

export type Ingrediente = {
	id: number;
	nombreIngrediente: string;
	unidadMedida: string;
	stockActual: number;
};

export type PedidoResumen = {
	id: number;
	fecha: string;
	valorTotal: number;
	detalle: Array<{
		comida: string;
		cantidad: number;
		precioUnitario: number;
		subtotal: number;
		personalizaciones: Array<{
			ingrediente: string;
			tipoAdicion: 'ADICIONAL' | 'RETIRADO';
			cantidad: number;
			costoAdicional: number;
		}>;
	}>;
};

export type AuthResponse = {
	token: string;
	nombreUsuario: string;
	rol: Rol;
	idCliente: number | null;
	nombre: string | null;
	documento: string | null;
	email: string | null;
};

export type Pedido = {
	detalles: Array<{
		comidaId: number;
		cantidad: number;
		personalizaciones: Array<{
			configuracionId: number;
			tipoAdicion: 'ADICIONAL' | 'RETIRADO' | 'AGREGAR' | 'RETIRAR';
			cantidad: number;
		}>;
	}>;
};

export type ErrorResponse = {
	status?: number;
	error?: string;
	message?: string;
	mensaje?: string;
};

export type ComidaBody = {
	nombre: string;
	precio: number;
	imagenURL?: string | null;
	ingredientes?: Array<{
		ingredienteId: number;
		cantidadBase: number;
		incluidoPorDefecto: boolean;
		permiteAdicional: boolean;
		permiteRetirar: boolean;
		precioAdicional: number;
	}>;
};

export type IngredienteBody = {
	nombre: string;
	unidadMedida: 'UNIDAD' | 'GRAMO' | 'MILILITRO';
	stockActual?: number;
};

function normalizeTipoAdicion(tipo: Pedido['detalles'][number]['personalizaciones'][number]['tipoAdicion']) {
	if (tipo === 'AGREGAR') return 'ADICIONAL';
	if (tipo === 'RETIRAR') return 'RETIRADO';
	return tipo;
}

function normalizePedido(body: Pedido) {
	return {
		...body,
		detalles: body.detalles.map((detalle) => ({
			...detalle,
			personalizaciones: detalle.personalizaciones.map((personalizacion) => ({
				...personalizacion,
				tipoAdicion: normalizeTipoAdicion(personalizacion.tipoAdicion),
			})),
		})),
	};
}

const baseUrl = import.meta.env.PUBLIC_GFC_API_URL ?? 'http://localhost:8080';

function getToken() {
	return typeof localStorage === 'undefined' ? null : localStorage.getItem('gfc_token');
}

export function saveSession(response: AuthResponse) {
	localStorage.setItem('gfc_token', response.token);
	localStorage.setItem('gfc_role', response.rol);
	localStorage.setItem('gfc_user', JSON.stringify(response));
}

export function clearSession() {
	localStorage.removeItem('gfc_token');
	localStorage.removeItem('gfc_role');
	localStorage.removeItem('gfc_user');
}

export function getSession() {
	if (typeof localStorage === 'undefined') return null;
	const user = localStorage.getItem('gfc_user');
	return user ? (JSON.parse(user) as AuthResponse) : null;
}

async function request<T>(path: string, options: RequestInit = {}) {
	const headers = new Headers(options.headers);
	if (options.body != null) headers.set('Content-Type', 'application/json');
	const token = getToken();
	if (token) headers.set('Authorization', `Bearer ${token}`);
	const response = await fetch(`${baseUrl}${path}`, { ...options, headers });
	if (!response.ok) {
		const error = (await response.json().catch(() => ({}))) as ErrorResponse;
		if (response.status === 401) clearSession();
		throw new Error(error.mensaje || error.message || error.error || 'No pudimos completar la solicitud.');
	}
	if (response.status === 204) return undefined as T;
	return (await response.json()) as T;
}

export const api = {
	getComidas: () => request<Comida[]>('/api/comidas'),
	getComida: (id: number) => request<ComidaDetalle>(`/api/comidas/${id}`),
	login: (body: { identificador: string; contrasena: string }) => request<AuthResponse>('/api/auth/login', { method: 'POST', body: JSON.stringify(body) }),
	register: (body: Record<string, string>) => request<AuthResponse>('/api/auth/registro', { method: 'POST', body: JSON.stringify(body) }),
	createOrder: (body: Pedido) => request<{ valorTotal?: number; total?: number; id?: number }>('/api/pedidos', { method: 'POST', body: JSON.stringify(normalizePedido(body)) }),
	getMyOrders: () => request<PedidoResumen[]>('/api/pedidos'),
	getIngredientes: () => request<Ingrediente[]>('/api/ingredientes'),
	addInventory: (id: number, body: { cantidad: number; motivo: string; fecha?: string }) => request(`/api/ingredientes/${id}/entradas`, { method: 'POST', body: JSON.stringify(body) }),
	removeInventory: (id: number, body: { cantidad: number; motivo: string; fecha?: string }) => request(`/api/ingredientes/${id}/salidas`, { method: 'POST', body: JSON.stringify(body) }),
	createComida: (body: ComidaBody) => request<Comida>('/api/comidas', { method: 'POST', body: JSON.stringify(body) }),
	updateComida: (id: number, body: ComidaBody) => request<Comida>(`/api/comidas/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
	deleteComida: (id: number) => request<undefined>(`/api/comidas/${id}`, { method: 'DELETE' }),
	createIngrediente: (body: IngredienteBody) => request<Ingrediente>('/api/ingredientes', { method: 'POST', body: JSON.stringify(body) }),
	updateIngrediente: (id: number, body: Pick<IngredienteBody, 'nombre' | 'unidadMedida'>) => request<Ingrediente>(`/api/ingredientes/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
	deleteIngrediente: (id: number) => request<undefined>(`/api/ingredientes/${id}`, { method: 'DELETE' }),
	darseDeBaja: () => request<undefined>('/api/cuenta/baja', { method: 'POST' }),
};

export const topComidasDemo = [
	{ nombre: 'Combo GFC Crunch', pedidos: 38 },
	{ nombre: 'Alitas Picantes', pedidos: 29 },
	{ nombre: 'Papas Kimchi', pedidos: 21 },
];