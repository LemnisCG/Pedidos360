import { Component } from '@angular/core'; import { Location } from '@angular/common';
@Component({selector:'app-back-button',standalone:true,template:`<button class="back-btn" type="button" (click)="back()" aria-label="Volver">← <span>Volver</span></button>`,styles:[`.back-btn{border:0;background:transparent;color:#526071;font-weight:700;cursor:pointer;padding:0;display:inline-flex;gap:8px;align-items:center}.back-btn:hover{color:#0d6efd}`]})
export class BackButtonComponent{constructor(private location:Location){} back(){this.location.back();}}
