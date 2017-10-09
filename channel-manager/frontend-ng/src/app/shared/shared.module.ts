import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { MaterialModule } from '../material/material.module';

@NgModule({
  imports: [
    BrowserModule,
    MaterialModule
  ],
  exports:[
    BrowserModule,
    MaterialModule
  ]
})
export class SharedModule {}
