export interface OrderItem { productoId: number; nombre: string; cantidad: number; precio: number; imagenUrl: string; codigoDigital: string; }
export interface Order {
  id: string; numeroPedido: string; usuarioId: string; email: string; nombreUsuario: string; direccion: string;
  total: number; metodoPago: string; tarjetaUltimos4: string; estado: string; emailEnviado: boolean;
  creadoEn: string; items: OrderItem[];
}
