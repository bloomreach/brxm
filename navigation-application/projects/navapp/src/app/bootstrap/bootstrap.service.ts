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
import { NavigationTrigger } from '@bloomreach/navapp-communication';
import { NGXLogger } from 'ngx-logger';
import { skip } from 'rxjs/operators';

import { ClientAppService } from '../client-app/services/client-app.service';
import { AppError } from '../error-handling/models/app-error';
import { CriticalError } from '../error-handling/models/critical-error';
import { ErrorHandlingService } from '../error-handling/services/error-handling.service';
import { MenuStateService } from '../main-menu/services/menu-state.service';
import { AuthService } from '../services/auth.service';
import { BusyIndicatorService } from '../services/busy-indicator.service';
import { MainLoaderService } from '../services/main-loader.service';
import { Configuration, NavConfigService } from '../services/nav-config.service';
import { NavItemService } from '../services/nav-item.service';
import { NavigationService } from '../services/navigation.service';
import { SiteService } from '../services/site.service';

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
    private readonly mainLoaderService: MainLoaderService,
    private readonly busyIndicatorService: BusyIndicatorService,
    private readonly siteService: SiteService,
    private readonly errorHandlingService: ErrorHandlingService,
    private readonly logger: NGXLogger,
  ) {
    this.siteService.selectedSite$.pipe(
      skip(1),  // Skip initial value
    ).subscribe(() => this.reinitialize());
  }

  async bootstrap(): Promise<void> {
    try {
      this.logger.debug('Bootstrapping the application');

      this.showLoader();

      await this.performSilentLogin();

      const configuration = await this.fetchConfiguration();

      this.initializeServices(configuration);

      // this method is intended to be used as app initializer promise, that mean app doesn't start until that promise is resolved.
      // Initial navigation can be finished only after iframes are loaded which happens only after the app has started. To prevent
      // app waiting to start forever initialNavigation() result is process separately and without await keyword.
      this.navigationService.initialNavigation().then(
        () => this.hideLoader(),
        error => this.handleInitializationError(error),
      );
    } catch (error) {
      this.handleInitializationError(error);
    }
  }

  async reinitialize(): Promise<void> {
    try {
      this.logger.debug('Reinitializing the application');

      this.showLoader();

      const navItems = await this.navConfigService.refetchNavItems();

      const configuration: Configuration = {
        navItems,
        sites: undefined,
        selectedSiteId: undefined,
      };

      this.initializeServices(configuration);

      await this.refreshPageIfPossibleOrNavigateHome();
    } catch (error) {
      this.handleInitializationError(error);
    } finally {
      this.hideLoader();
    }
  }

  private async performSilentLogin(): Promise<void> {
    this.logger.debug('Performing silent login');

    try {
      await this.authService.loginAllResources();

      this.logger.debug('Silent login has done successfully');
    } catch (error) {
      throw new CriticalError('ERROR_UNABLE_TO_PERFORM_SILENT_LOGIN', this.extractErrorMessage(error));
    }
  }

  private async fetchConfiguration(): Promise<Configuration> {
    this.logger.debug('Fetching the application\'s configuration');

    try {
      const configuration = await this.navConfigService.fetchNavigationConfiguration();

      if (configuration.navItems.length === 0) {
        throw new Error('There are no nav items to process. Either the configuration is wrong or all config resources failed.');
      }

      this.logger.debug('The application configuration has been fetched successfully');

      return configuration;
    } catch (error) {
      throw new CriticalError('ERROR_UNABLE_TO_LOAD_CONFIGURATION', this.extractErrorMessage(error));
    }
  }

  private initializeServices(configuration: Configuration): void {
    if (configuration.sites && configuration.selectedSiteId) {
      this.siteService.init(configuration.sites, configuration.selectedSiteId);
    }

    const navItems = this.navItemService.registerNavItems(configuration.navItems);

    this.menuStateService.init(navItems);
    this.navigationService.init(navItems);
    this.clientAppService.init(navItems).catch(error => this.handleInitializationError(error));
  }

  private async refreshPageIfPossibleOrNavigateHome(): Promise<void> {
    try {
      await this.navigationService.reload();
    } catch {
      await this.navigationService.navigateToHome(NavigationTrigger.InitialNavigation);
    }
  }

  private handleInitializationError(error: any): void {
    this.hideLoader();

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

  private showLoader(): void {
    this.mainLoaderService.show();
    this.busyIndicatorService.show();
  }

  private hideLoader(): void {
    this.mainLoaderService.hide();
    this.busyIndicatorService.hide();
  }
}
