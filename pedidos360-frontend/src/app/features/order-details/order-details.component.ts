import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { OrderService } from '../../core/services/order.service';
import { Order } from '../../core/models/order.model';

@Component({
  selector: 'app-order-details',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './order-details.component.html',
  styleUrl: './order-details.component.css'
})
export class OrderDetailsComponent {
  readonly order = signal<Order | null>(null);
  readonly loading = signal(false);

  constructor(route: ActivatedRoute, service: OrderService) {
    const id = route.snapshot.paramMap.get('id')!;
    if (id !== 'demo') {
      this.loading.set(true);
      service.get(id).subscribe((order) => {
        this.order.set(order);
        this.loading.set(false);
      });
    }
  }
}
