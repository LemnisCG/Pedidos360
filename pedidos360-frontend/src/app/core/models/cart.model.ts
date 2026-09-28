import { Product } from './product.model';
export interface CartApiItem { id: number; usuarioId: string; productoId: number; cantidad: number; }
export interface CartLine { producto: Product; cantidad: number; }
