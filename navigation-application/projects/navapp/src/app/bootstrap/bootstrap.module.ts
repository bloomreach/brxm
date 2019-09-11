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

import { APP_INITIALIZER, NgModule } from '@angular/core';

import { BusyIndicatorService } from '../services/busy-indicator.service';
import { NavConfigService } from '../services/nav-config.service';

import { APP_BOOTSTRAPPED } from './app-bootstrapped';
import { BootstrapService } from './bootstrap.service';

let bootstrapResolve: () => void;
let bootstrapReject: () => void;
const appBootstrappedPromise = new Promise((resolve, reject) => {
  bootstrapResolve = resolve;
  bootstrapReject = reject;
});

const loadNavItems = (navConfigService: NavConfigService) => navConfigService.init();
const appInitializer = (
  navConfigService: NavConfigService,
  bootstrapService: BootstrapService,
  busyIndicatorService: BusyIndicatorService,
) => {
  return () => {
    return loadNavItems(navConfigService)
      .then(() => setTimeout(() => {
        busyIndicatorService.show();

        bootstrapService.bootstrap().then(() => {
          busyIndicatorService.hide();
          bootstrapResolve();
        }).catch(bootstrapReject);
      }));
  };
};

@NgModule({
  providers: [
    {
      provide: APP_INITIALIZER,
      useFactory: appInitializer,
      deps: [NavConfigService, BootstrapService, BusyIndicatorService],
      multi: true,
    },
    {
      provide: APP_BOOTSTRAPPED,
      useValue: appBootstrappedPromise,
    },
  ],
})
export class BootstrapModule { }
