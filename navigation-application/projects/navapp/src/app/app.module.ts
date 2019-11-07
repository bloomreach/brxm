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
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { LoggerModule, NGXLogger, NgxLoggerLevel } from 'ngx-logger';

import { AppComponent } from './app.component';
import { BootstrapModule } from './bootstrap/bootstrap.module';
import { ClientAppModule } from './client-app/client-app.module';
import { ErrorHandlingModule } from './error-handling/error-handling.module';
import { MainMenuModule } from './main-menu/main-menu.module';
import { APP_SETTINGS } from './services/app-settings';
import { appSettingsFactory } from './services/app-settings.factory';
import { translateHttpLoaderFactory } from './services/translate-http-loader.factory';
import { USER_ACTIVITY_DEBOUNCE_TIME } from './services/user-activity-debounce-time';
import { USER_SETTINGS } from './services/user-settings';
import { userSettingsFactory } from './services/user-settings.factory';
import { CustomWindow, WindowRef } from './shared/services/window-ref.service';
import { SharedModule } from './shared/shared.module';
import { TopPanelModule } from './top-panel/top-panel.module';

const logLevelString: string = (window as CustomWindow).NavAppSettings.appSettings.logLevel;
const loglevel = logLevelString || 'WARN';

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
    TranslateModule.forRoot({
      loader: {
        provide: TranslateLoader,
        useFactory: translateHttpLoaderFactory,
        deps: [HttpClient, Location],
      },
    }),
    LoggerModule.forRoot({
      // Set the desired log level here to avoid missing log messages due to not properly configured log messages level
      level: NgxLoggerLevel[loglevel],
      enableSourceMaps: true,
    }),
  ],
  providers: [
    Location,
    { provide: LocationStrategy, useClass: PathLocationStrategy },
    { provide: APP_BASE_HREF, useValue: window.location.origin },
    { provide: APP_SETTINGS, useFactory: appSettingsFactory, deps: [WindowRef, Location, NGXLogger] },
    { provide: USER_SETTINGS, useFactory: userSettingsFactory, deps: [WindowRef, NGXLogger] },
    { provide: USER_ACTIVITY_DEBOUNCE_TIME, useValue: 30000 },
  ],
  declarations: [AppComponent],
  bootstrap: [AppComponent],
})
export class AppModule {}
