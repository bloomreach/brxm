/*
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { APP_BASE_HREF, Location, LocationStrategy, PathLocationStrategy } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppComponent } from './app.component';
import { BootstrapModule } from './bootstrap/bootstrap.module';
import { ClientAppModule } from './client-app/client-app.module';
import { ErrorHandlingModule } from './error-handling/error-handling.module';
import { MainMenuModule } from './main-menu/main-menu.module';
import { APP_SETTINGS } from './services/app-settings';
import { appSettingsFactory } from './services/app-settings.factory';
import { USER_SETTINGS } from './services/user-settings';
import { userSettingsFactory } from './services/user-settings.factory';
import { WindowRef } from './shared/services/window-ref.service';
import { SharedModule } from './shared/shared.module';
import { TopPanelModule } from './top-panel/top-panel.module';

@NgModule({
  imports: [
    BrowserModule,
    SharedModule,
    ClientAppModule,
    HttpClientModule,
    MainMenuModule,
    TopPanelModule,
    ErrorHandlingModule,
    BootstrapModule,
  ],
  providers: [
    Location,
    { provide: LocationStrategy, useClass: PathLocationStrategy },
    { provide: APP_BASE_HREF, useValue: window.location.origin },
    { provide: APP_SETTINGS, useFactory: appSettingsFactory, deps: [WindowRef, Location] },
    { provide: USER_SETTINGS, useFactory: userSettingsFactory, deps: [WindowRef] },
  ],
  declarations: [AppComponent],
  bootstrap: [AppComponent],
})
export class AppModule {}
