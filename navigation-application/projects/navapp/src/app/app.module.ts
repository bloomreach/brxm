import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { ClientApplicationsManagerModule } from './client-applications-manager';
import { MainMenuModule } from './main-menu';
import { SharedModule } from './shared';

@NgModule({
  imports: [
    BrowserModule,
    SharedModule,
    AppRoutingModule,
    BrowserModule,
    ClientApplicationsManagerModule,
    HttpClientModule,
    MainMenuModule,
  ],
  declarations: [AppComponent],
  bootstrap: [AppComponent],
})
export class AppModule {}
