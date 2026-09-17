export interface OrderLine {
  productoId: number;
  nombre: string;
  precioUnitario: number;
  cantidad: number;
  codigoJuego?: string;
  imagenUrl?: string;
}

export interface Order {
  id: string;
  numeroOrden: string;
  fecha: string;
  estado: string;
  metodoPago: string;
  total: number;
  clienteEmail?: string;
  direccionEnvio?: string;
  confirmacionEmailEnviada?: boolean;
  items: OrderLine[];
}
