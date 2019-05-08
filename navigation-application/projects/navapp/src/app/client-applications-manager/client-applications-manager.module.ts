/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { IframesContainerComponent } from './components';
import {
  ClientApplicationsManagerService,
  ClientApplicationsRegistryService,
} from './services';

@NgModule({
  imports: [
    BrowserModule,
  ],
  declarations: [
    IframesContainerComponent,
  ],
  providers: [
    ClientApplicationsRegistryService,
    ClientApplicationsManagerService,
  ],
  exports: [
    IframesContainerComponent,
  ],
})
export class ClientApplicationsManagerModule {}
