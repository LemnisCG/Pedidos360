export interface Product {
  id: number; nombre: string; descripcion: string; precio: number; stock: number;
  categoria: string; imagenUrl: string; fechaLanzamiento: string;
}
export interface CatalogBanner { id?: number; etiqueta: string; titulo: string; subtitulo: string; }
export interface CatalogResponse { banner: CatalogBanner; productos: Product[]; }
