import { Component, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { OrderService } from '../../core/services/order.service';
import { Order } from '../../core/models/order.model';

@Component({
  selector: 'app-order-created',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './order-created.component.html',
  styleUrl: './order-created.component.css'
})
export class OrderCreatedComponent {
  readonly order = signal<Order | null>(null);
  readonly loading = signal(true);

  constructor(route: ActivatedRoute, service: OrderService) {
    service.get(route.snapshot.paramMap.get('id')!).subscribe((order) => {
      this.order.set(order);
      this.loading.set(false);
    });
  }
}
