import { Component, computed, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { Router } from '@angular/router';
import { ProductService } from '../../core/services/product.service';
import { AuthService } from '../../core/auth/auth.service';
import { Product } from '../../core/models/product.model';

@Component({selector:'app-store',standalone:true,imports:[CurrencyPipe],templateUrl:'./store.component.html',styleUrl:'./store.component.css'})
export class StoreComponent{
 search=signal(''); filtered=computed(()=>{const q=this.search().trim().toLowerCase();return !q?this.products.products():this.products.products().filter(p=>(p.nombre+' '+p.categoria).toLowerCase().includes(q));});
 constructor(public products:ProductService,public auth:AuthService,private router:Router){}
 open(p:Product){if(!this.auth.isLoggedIn()){this.auth.open('login');return;}this.router.navigate(['/game',p.id]);}
}
