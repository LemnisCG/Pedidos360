import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { OrderService } from '../../core/services/order.service';
import { Order } from '../../core/models/order.model';
import { BackButtonComponent } from '../../shared/components/back-button/back-button.component';
@Component({selector:'app-order-created',standalone:true,imports:[CurrencyPipe,DatePipe,RouterLink,BackButtonComponent],templateUrl:'./order-created.component.html',styleUrl:'./order-created.component.css'})
export class OrderCreatedComponent implements OnInit {
  readonly order = signal<Order|null>(null);
  readonly failed = signal(false);
  constructor(private route: ActivatedRoute, private orders: OrderService){}
  async ngOnInit(){ try{this.order.set(await this.orders.get(this.route.snapshot.paramMap.get('id')!));}catch{this.failed.set(true);} }
}
