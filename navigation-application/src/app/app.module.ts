import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { LeftMenuModule } from './left-menu';
import { NavigationConfigurationService } from './services';

import { AppComponent } from './app.component';

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    LeftMenuModule,
  ],
  providers: [
    NavigationConfigurationService,
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
