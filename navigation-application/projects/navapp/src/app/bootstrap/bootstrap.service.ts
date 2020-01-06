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
import { AppError } from '../error-handling/models/app-error';
import { ErrorHandlingService } from '../error-handling/services/error-handling.service';
import { MenuStateService } from '../main-menu/services/menu-state.service';
import { NavItem } from '../models/nav-item.model';
import { AuthService } from '../services/auth.service';
import { BusyIndicatorService } from '../services/busy-indicator.service';
import { NavConfigService } from '../services/nav-config.service';
import { NavItemService } from '../services/nav-item.service';
import { NavigationService } from '../services/navigation.service';

let bootstrapResolve: () => void;
let bootstrapReject: () => void;
export const appBootstrappedPromise = new Promise((resolve, reject) => {
  bootstrapResolve = resolve;
  bootstrapReject = reject;
});

@Injectable({
  providedIn: 'root',
})
export class BootstrapService {
  constructor(
    private readonly authService: AuthService,
    private readonly navConfigService: NavConfigService,
    private readonly navItemService: NavItemService,
    private readonly clientAppService: ClientAppService,
    private readonly menuStateService: MenuStateService,
    private readonly navigationService: NavigationService,
    private readonly busyIndicatorService: BusyIndicatorService,
    private readonly errorHandlingService: ErrorHandlingService,
  ) { }

  async bootstrap(): Promise<void> {
    this.busyIndicatorService.show();

    await this.performSilentLogin();

    const navItems = await this.fetchNavItems();

    this.clientAppService.appConnected$.subscribe(app => {
      this.navItemService.activateNavItems(app.url);
    });

    this.initializeServices(navItems).then(() => this.busyIndicatorService.hide());
  }

  private async performSilentLogin(): Promise<void> {
    try {
      await this.authService.loginAllResources();
    } catch (error) {
      this.errorHandlingService.setCriticalError('ERROR_UNABLE_TO_PERFORM_SILENT_LOGIN', this.extractErrorMessage(error));
    }
  }

  private async fetchNavItems(): Promise<NavItem[]> {
    try {
      const navItemDtos = await this.navConfigService.init();

      return this.navItemService.registerNavItemDtos(navItemDtos);
    } catch (error) {
      this.errorHandlingService.setCriticalError('ERROR_UNABLE_TO_LOAD_CONFIGURATION', this.extractErrorMessage(error));
    }
  }

  private initializeServices(navItems: NavItem[]): Promise<void> {
    try {
      this.clientAppService.init(navItems).catch(error => this.handleInitializationError(error));
      this.menuStateService.init(navItems);
      this.navigationService.init(navItems);

      return this.navigationService.initialNavigation()
        .then(bootstrapResolve, error => this.handleInitializationError(error));
    } catch (error) {
      this.handleInitializationError(error);

      return Promise.resolve();
    }
  }

  private handleInitializationError(error: any): void {
    if (error instanceof AppError) {
      this.errorHandlingService.setError(error);

      return;
    }

    this.errorHandlingService.setInternalError(
      'ERROR_INITIALIZATION',
      this.extractErrorMessage(error),
    );
  }

  private extractErrorMessage(error: any): string {
    return error ? (error.message ? error.message : error) : '';
  }
}
