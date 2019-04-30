/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { NgModule } from '@angular/core';

import { NavigationApplicationComponent } from './navigation-application.component';
import { LeftMenuModule } from './left-menu';
import { NavigationConfigurationService } from './services';

@NgModule({
  declarations: [
    NavigationApplicationComponent
  ],
  imports: [
    LeftMenuModule,
  ],
  providers: [
    NavigationConfigurationService,
  ],
  exports: [
    NavigationApplicationComponent
  ],
})
export class NavigationApplicationModule { }
