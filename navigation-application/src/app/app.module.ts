import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';

import { AppRoutingModule } from './app-routing.module';
import { LeftMenuModule } from './left-menu';
import { NavigationConfigurationService } from './services';

import { AppComponent } from './app.component';
import { ClientApplicationsManagerModule } from './client-applications-manager';
import { CommunicationModule } from './communication';

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    LeftMenuModule,
    ClientApplicationsManagerModule,
    CommunicationModule,
  ],
  providers: [
    NavigationConfigurationService,
  ],
  bootstrap: [AppComponent]
})
export class AppModule {}
