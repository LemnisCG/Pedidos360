import { Routes } from '@angular/router';
import { StoreComponent } from './features/store/store.component';
import { ProductDetailComponent } from './features/product-detail/product-detail.component';
import { CartComponent } from './features/cart/cart.component';
import { CheckoutComponent } from './features/checkout/checkout.component';
import { OrderCreatedComponent } from './features/order-created/order-created.component';
import { OrderDetailsComponent } from './features/order-details/order-details.component';

export const routes: Routes = [
  { path: '', component: StoreComponent },
  { path: 'game/:id', component: ProductDetailComponent },
  { path: 'cart', component: CartComponent },
  { path: 'checkout', component: CheckoutComponent },
  { path: 'order-created/:id', component: OrderCreatedComponent },
  { path: 'order-details/:id', component: OrderDetailsComponent },
  { path: '**', redirectTo: '' }
];
