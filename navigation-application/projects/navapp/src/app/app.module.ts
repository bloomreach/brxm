import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { LeftMenuModule } from './left-menu';
import { NavigationConfigurationService } from './services';

import { AppComponent } from './app.component';
import { ClientApplicationsManagerModule } from './client-applications-manager';
import { CommunicationModule } from './communication';
import { MaterialModule } from './shared/material.module';

@NgModule({
  imports: [
    BrowserModule,
    MaterialModule,
    AppRoutingModule,
    LeftMenuModule,
    ClientApplicationsManagerModule,
    CommunicationModule,
  ],
  providers: [
    NavigationConfigurationService,
  ],
  declarations: [
    AppComponent,
  ],
  bootstrap: [AppComponent],
})
export class AppModule {}
