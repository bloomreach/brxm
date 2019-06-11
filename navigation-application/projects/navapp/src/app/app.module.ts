import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { ClientAppModule } from './client-app';
import { MainMenuModule } from './main-menu';
import { SharedModule } from './shared';
import { TopPanelModule } from './top-panel';

@NgModule({
  imports: [
    BrowserModule,
    SharedModule,
    AppRoutingModule,
    BrowserModule,
    ClientAppModule,
    HttpClientModule,
    MainMenuModule,
    TopPanelModule,
  ],
  declarations: [AppComponent],
  bootstrap: [AppComponent],
})
export class AppModule {}
