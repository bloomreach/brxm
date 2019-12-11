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

import { Injectable } from '@angular/core';

import { ClientAppService } from '../client-app/services/client-app.service';
import { MenuStateService } from '../main-menu/services/menu-state.service';
import { NavigationService } from '../services/navigation.service';

@Injectable({
  providedIn: 'root',
})
export class BootstrapService {
  constructor(
    private readonly clientAppService: ClientAppService,
    private readonly menuStateService: MenuStateService,
    private readonly navigationService: NavigationService,
  ) { }

  bootstrap(): Promise<void> {
    return this.clientAppService.init()
      .then(() => this.menuStateService.init())
      .then(() => this.navigationService.initialNavigation());
  }
}
