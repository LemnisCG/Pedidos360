import { Routes } from '@angular/router';
import { StoreComponent } from './features/store/store.component';
import { ProductDetailComponent } from './features/product-detail/product-detail.component';
import { CartComponent } from './features/cart/cart.component';
import { CheckoutComponent } from './features/checkout/checkout.component';
import { OrderCreatedComponent } from './features/order-created/order-created.component';
import { OrdersComponent } from './features/orders/orders.component';
import { OrderDetailsComponent } from './features/order-details/order-details.component';
import { OAuthCallbackComponent } from './features/oauth-callback/oauth-callback.component';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: '', component: StoreComponent },
  { path: 'auth/callback', component: OAuthCallbackComponent },
  { path: 'game/:id', component: ProductDetailComponent, canActivate: [authGuard] },
  { path: 'cart', component: CartComponent, canActivate: [authGuard] },
  { path: 'checkout', component: CheckoutComponent, canActivate: [authGuard] },
  { path: 'orders', component: OrdersComponent, canActivate: [authGuard] },
  { path: 'orders/:id/created', component: OrderCreatedComponent, canActivate: [authGuard] },
  { path: 'orders/:id', component: OrderDetailsComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: '' }
];
