import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { MainMenuModule } from './main-menu';
import { NavigationConfigurationService } from './services';

import { AppComponent } from './app.component';
import { ClientApplicationsManagerModule } from './client-applications-manager';
import { CommunicationModule } from './communication';

@NgModule({
  declarations: [AppComponent],
  imports: [
    BrowserModule,
    AppRoutingModule,
    MainMenuModule,
    ClientApplicationsManagerModule,
    CommunicationModule,
  ],
  providers: [NavigationConfigurationService],
  bootstrap: [AppComponent],
})
export class AppModule {}
