import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { ClientApplicationsManagerModule } from './client-applications-manager';
import { CommunicationModule } from './communication';
import { MainMenuModule } from './main-menu';
import { NavigationConfigurationService } from './services';

@NgModule({
  declarations: [AppComponent],
  imports: [
    AppRoutingModule,
    BrowserModule,
    ClientApplicationsManagerModule,
    CommunicationModule,
    HttpClientModule,
    MainMenuModule,
  ],
  providers: [NavigationConfigurationService],
  bootstrap: [AppComponent],
})
export class AppModule {}
