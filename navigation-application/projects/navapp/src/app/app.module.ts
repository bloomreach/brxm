import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { ClientApplicationsManagerModule } from './client-applications-manager';
import { CommunicationModule } from './communication';
import { MainMenuModule } from './main-menu';
import { NavigationConfigurationService } from './services';
import { MaterialModule } from './shared';

@NgModule({
  imports: [
    BrowserModule,
    MaterialModule,
    AppRoutingModule,
    BrowserModule,
    ClientApplicationsManagerModule,
    CommunicationModule,
    HttpClientModule,
    MainMenuModule,
  ],
  providers: [NavigationConfigurationService],
  declarations: [AppComponent],
  bootstrap: [AppComponent],
})
export class AppModule {}
