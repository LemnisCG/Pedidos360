import { Routes } from '@angular/router';
import { MsalGuard } from '@azure/msal-angular';
import { StoreComponent } from './features/store/store.component';
import { ProductDetailComponent } from './features/product-detail/product-detail.component';
import { CartComponent } from './features/cart/cart.component';
import { CheckoutComponent } from './features/checkout/checkout.component';
import { OrderCreatedComponent } from './features/order-created/order-created.component';
import { OrdersComponent } from './features/orders/orders.component';
import { OrderDetailsComponent } from './features/order-details/order-details.component';
import { CatalogBannerAdministrationComponent } from './features/catalog-banner-administration/catalog-banner-administration.component';
import { administradorRoleGuard } from './core/auth/administrador-role.guard';

export const routes: Routes = [
  { path: '', component: StoreComponent },
  { path: 'game/:id', component: ProductDetailComponent, canActivate: [MsalGuard] },
  { path: 'cart', component: CartComponent, canActivate: [MsalGuard] },
  { path: 'checkout', component: CheckoutComponent, canActivate: [MsalGuard] },
  { path: 'orders', component: OrdersComponent, canActivate: [MsalGuard] },
  { path: 'orders/:id/created', component: OrderCreatedComponent, canActivate: [MsalGuard] },
  { path: 'orders/:id', component: OrderDetailsComponent, canActivate: [MsalGuard] },
  { path: 'admin/catalog-banner', component: CatalogBannerAdministrationComponent, canActivate: [MsalGuard, administradorRoleGuard] },
  { path: '**', redirectTo: '' }
];
