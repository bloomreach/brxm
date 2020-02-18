/*
 * Copyright 2019-2020 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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
import { NGXLogger } from 'ngx-logger';
import { skip } from 'rxjs/operators';

import { ClientAppService } from '../client-app/services/client-app.service';
import { AppError } from '../error-handling/models/app-error';
import { ErrorHandlingService } from '../error-handling/services/error-handling.service';
import { MenuStateService } from '../main-menu/services/menu-state.service';
import { NavItem } from '../models/nav-item.model';
import { AuthService } from '../services/auth.service';
import { BusyIndicatorService } from '../services/busy-indicator.service';
import { Configuration, NavConfigService } from '../services/nav-config.service';
import { NavItemService } from '../services/nav-item.service';
import { NavigationService } from '../services/navigation.service';
import { SiteService } from '../services/site.service';
import { WindowRef } from '../shared/services/window-ref.service';

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
    private readonly siteService: SiteService,
    private readonly errorHandlingService: ErrorHandlingService,
    private readonly windowRef: WindowRef,
    private readonly logger: NGXLogger,
  ) {
    this.clientAppService.appConnected$.subscribe(app => this.navItemService.activateNavItems(app.url));
    this.siteService.selectedSite$.pipe(
      skip(1),  // Skip initial value
    ).subscribe(() => this.reinitialize());
  }

  async bootstrap(): Promise<void> {
    this.logger.debug('Bootstrapping the application');

    this.busyIndicatorService.show();

    await this.performSilentLogin();

    const configuration = await this.fetchConfiguration();

    if (!configuration) {
      return;
    }

    this.siteService.init(configuration.sites, configuration.selectedSiteId);

    const navItems = this.navItemService.registerNavItemDtos(configuration.navItems);

    this.initializeServices(navItems).then(() => this.busyIndicatorService.hide());
  }

  async reinitialize(): Promise<void> {
    this.logger.debug('Reinitializing the application');

    this.busyIndicatorService.show();

    const navItemDtos = await this.navConfigService.refetchNavItems();
    this.logger.debug('Nav items', navItemDtos);

    this.windowRef.nativeWindow.location.reload();
  }

  private async performSilentLogin(): Promise<void> {
    this.logger.debug('Performing silent login');

    try {
      await this.authService.loginAllResources();

      this.logger.debug('Silent login has done successfully');
    } catch (error) {
      this.errorHandlingService.setCriticalError('ERROR_UNABLE_TO_PERFORM_SILENT_LOGIN', this.extractErrorMessage(error));
    }
  }

  private async fetchConfiguration(): Promise<Configuration> {
    this.logger.debug('Fetching the application\'s configuration');

    try {
      const configuration = await this.navConfigService.fetchNavigationConfiguration();

      this.logger.debug('The application configuration has been fetched successfully');

      return configuration;
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
