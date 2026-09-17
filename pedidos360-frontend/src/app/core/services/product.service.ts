import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of } from 'rxjs';
import { Product } from '../models/product.model';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly api = 'http://localhost:8082/api/productos';

  // Catálogo de respaldo. Se usa solamente si el microservicio Producto no responde.
  // imagenUrl queda vacía a propósito para que agregues tus PNG posteriormente.
  private readonly demo: Product[] = [
    { id: 1, nombre: 'ASTRO BOT', descripcion: 'Aventura de plataformas con mundos coloridos, desafíos y exploración.', precio: 59990, stock: 20, categoria: 'Aventura', fechaLanzamiento: '2024-09-06', imagenUrl: '' },
    { id: 2, nombre: 'Mario Kart 8 Deluxe', descripcion: 'Carreras arcade con circuitos, personajes y modos multijugador.', precio: 49990, stock: 25, categoria: 'Carreras', fechaLanzamiento: '2017-04-28', imagenUrl: '' },
    { id: 3, nombre: 'Minecraft', descripcion: 'Construye, explora y sobrevive en un mundo abierto creado con bloques.', precio: 29990, stock: 35, categoria: 'Sandbox', fechaLanzamiento: '2018-06-21', imagenUrl: '' },
    { id: 4, nombre: 'Street Fighter II Ultra', descripcion: 'Combate arcade clásico con luchadores, combos y enfrentamientos competitivos.', precio: 9990, stock: 18, categoria: 'Lucha', fechaLanzamiento: '2017-05-26', imagenUrl: '' },
    { id: 5, nombre: 'Super Mario Bros. 3', descripcion: 'Plataformas clásicas con mundos, poderes y niveles llenos de desafíos.', precio: 19990, stock: 22, categoria: 'Plataformas', fechaLanzamiento: '1988-10-23', imagenUrl: '' }
  ];

  constructor(private readonly http: HttpClient) {}

  list(): Observable<Product[]> {
    return this.http.get<Product[]>(this.api).pipe(catchError(() => of(this.demo)));
  }

  get(id: number): Observable<Product | undefined> {
    return this.http.get<Product>(`${this.api}/${id}`).pipe(
      catchError(() => of(this.demo.find((p) => p.id === id)))
    );
  }
}
