/*
 * Copyright 2019-2021 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import { Location } from '@angular/common';
import { Inject, Injectable, OnDestroy } from '@angular/core';
import { NavigationTrigger, NavItem, NavLocation } from '@bloomreach/navapp-communication';
import { TranslateService } from '@ngx-translate/core';
import { NGXLogger } from 'ngx-logger';
import { BehaviorSubject, EMPTY, from, Observable, of, Subject, Subscription, throwError } from 'rxjs';
import { catchError, filter, finalize, mapTo, switchMap, take, tap } from 'rxjs/operators';

import { ClientApp } from '../client-app/models/client-app.model';
import { ClientAppService } from '../client-app/services/client-app.service';
import { AppError } from '../error-handling/models/app-error';
import { CriticalError } from '../error-handling/models/critical-error';
import { InternalError } from '../error-handling/models/internal-error';
import { NotFoundError } from '../error-handling/models/not-found-error';
import { ErrorHandlingService } from '../error-handling/services/error-handling.service';
import { distinctUntilAccumulatorIsEmpty } from '../helpers/distinct-until-equal-number-of-values';
import { stripOffQueryStringAndHash } from '../helpers/strip-off-query-string-and-hash';
import { MenuStateService } from '../main-menu/services/menu-state.service';
import { AppSettings } from '../models/dto/app-settings.dto';
import { BreadcrumbsService } from '../top-panel/services/breadcrumbs.service';

import { APP_SETTINGS } from './app-settings';
import { BusyIndicatorService } from './busy-indicator.service';
import { ConnectionService } from './connection.service';
import { UrlMapperService } from './url-mapper.service';

interface Route {
  path: string;
  navItem: NavItem;
}

interface Navigation {
  url: string;
  state: Record<string, unknown>;
  source: NavigationTrigger;
  replaceState: boolean;
}

interface NavigationSetup {
  url: string;
  route: Route;
  appId: string;
  source: NavigationTrigger;
}

@Injectable({
  providedIn: 'root',
})
export class NavigationService implements OnDestroy {
  private readonly navigation$ = new Subject<Navigation>();
  private readonly navigationCompleted$ = new Subject<void>();

  get navigating$(): Observable<boolean> {
    return this.navigatingFiltered;
  }

  private get basePath(): string {
    return this.appSettings.basePath;
  }

  private get homeUrl(): string {
    const homeMenuItem = this.menuStateService.currentHomeMenuItem;

    if (!homeMenuItem) {
      throw new CriticalError('ERROR_CONFIGURATION', 'Unable to find home item');
    }

    return this.urlMapperService.mapNavItemToBrowserUrl(homeMenuItem.navItem);
  }
  private routes: Route[];
  private locationSubscription: Subscription;
  private currentNavItem: NavItem;
  private readonly navigating = new BehaviorSubject(false);
  private readonly navigatingFiltered: Observable<boolean>;

  constructor(
    @Inject(APP_SETTINGS) private readonly appSettings: AppSettings,
    private readonly breadcrumbsService: BreadcrumbsService,
    private readonly busyIndicatorService: BusyIndicatorService,
    private readonly clientAppService: ClientAppService,
    private readonly connectionService: ConnectionService,
    private readonly errorHandlingService: ErrorHandlingService,
    private readonly location: Location,
    private readonly menuStateService: MenuStateService,
    private readonly urlMapperService: UrlMapperService,
    private readonly translateService: TranslateService,
    private readonly logger: NGXLogger,
  ) {
    this.connectionService
      .navigate$
      .subscribe(navLocation => this.navigateByNavLocation(navLocation, NavigationTrigger.AnotherApp));

    this.connectionService
      .updateNavLocation$
      .subscribe(navLocation => this.updateByNavLocation(navLocation));

    this.navigatingFiltered = this.navigating.pipe(
      distinctUntilAccumulatorIsEmpty(),
    );

    this.navigation$
    .pipe(
      switchMap(navigation => from(this.setupNavigation(navigation))),
      switchMap(navigationSetup => from(this.processNavigation(navigationSetup))),
    )
    .subscribe({
      next: () => this.navigationCompleted$.next(),
      error: error => {
        if (error instanceof AppError) {
          this.errorHandlingService.setError(error);
        }

        if (error instanceof Error) {
          this.errorHandlingService.setInternalError(undefined, error.message);
        }

        if (typeof error === 'string') {
          error = new InternalError(undefined, error);
        }
      },
    });
  }

  init(navItems: NavItem[]): void {
    this.routes = this.generateRoutes(navItems);
  }

  initialNavigation(): Promise<void> {
    this.setUpLocationChangeListener();

    const url = this.appSettings.initialPath ?
      Location.joinWithSlash(this.basePath, this.appSettings.initialPath) :
      this.location.path(true);

    return this.scheduleNavigation(url, NavigationTrigger.InitialNavigation, {}, true);
  }

  ngOnDestroy(): void {
    if (this.locationSubscription) {
      this.locationSubscription.unsubscribe();
    }
  }

  navigateByNavItem(navItem: NavItem, triggeredBy: NavigationTrigger, breadcrumbLabel?: string): Promise<void> {
    const browserUrl = this.urlMapperService.mapNavItemToBrowserUrl(navItem);
    return this.navigateByUrl(browserUrl, triggeredBy, breadcrumbLabel);
  }

  navigateByNavLocation(navLocation: NavLocation, triggeredBy: NavigationTrigger): Promise<void> {
    let browserUrl: string;

    try {
      browserUrl = this.urlMapperService.mapNavLocationToBrowserUrl(navLocation, false)[0];
    } catch (e) {
      this.errorHandlingService.setNotFoundError(
        undefined,
        `An attempt to navigate was failed due to app path is not allowable: '${navLocation.path}'`,
      );

      return;
    }

    return this.navigateByUrl(browserUrl, triggeredBy, navLocation.breadcrumbLabel);
  }

  updateByNavLocation(navLocation: NavLocation): void {
    let browserUrl: string;
    let navItem: NavItem;

    try {
      [browserUrl, navItem] = this.urlMapperService.mapNavLocationToBrowserUrl(navLocation, true);
    } catch (e) {
      this.errorHandlingService.setNotFoundError(
        undefined,
        `An attempt to update the app url was failed due to app path is not allowable: '${navLocation.path}'`,
      );

      return;
    }

    this.currentNavItem = navItem;
    this.menuStateService.activateMenuItem(navItem.appIframeUrl, navItem.appPath);
    this.breadcrumbsService.setSuffix(navLocation.breadcrumbLabel);

    const replaceState = !navLocation.addHistory;

    this.setBrowserUrl(browserUrl, { breadcrumbLabel: navLocation.breadcrumbLabel }, replaceState);
  }

  navigateToDefaultAppPage(triggeredBy: NavigationTrigger): Promise<void> {
    if (!this.currentNavItem) {
      return Promise.resolve();
    }

    return this.navigateByNavItem(this.currentNavItem, triggeredBy, '');
  }

  navigateToHome(triggeredBy: NavigationTrigger): Promise<void> {
    return this.navigateByUrl(this.homeUrl, triggeredBy);
  }

  async reload(): Promise<void> {
    const currentUrl = this.location.path(true);

    if (!this.isNavigationPossible(currentUrl)) {
      throw new Error('Navigation impossible');
    }

    return this.navigateByUrl(currentUrl, NavigationTrigger.InitialNavigation);
  }

  navigateByUrl(url: string, triggeredBy: NavigationTrigger, breadcrumbLabel?: string): Promise<void> {
    this.errorHandlingService.clearError();

    return this.scheduleNavigation(url, triggeredBy, { breadcrumbLabel });
  }

  private isNavigationPossible(url: string): boolean {
    return !!this.matchRoute(url);
  }

  private setUpLocationChangeListener(): void {
    if (this.locationSubscription) {
      return;
    }

    this.locationSubscription = this.location.subscribe(change => {
      this.errorHandlingService.clearError();

      this.scheduleNavigation(change.url, NavigationTrigger.PopState, change.state);
    }) as any;
  }

  private async scheduleNavigation(
    url: string,
    source: NavigationTrigger,
    state: Record<string, unknown> = {},
    replaceState = false,
  ): Promise<void> {
    const normalizedUrl = this.location.normalize(url);

    const navigation: Navigation = {
      url: normalizedUrl,
      state,
      source,
      replaceState,
    };

    this.navigation$.next(navigation);
    return this.navigationCompleted$.pipe(take(1)).toPromise();
  }

  private async setupNavigation({
      url,
      state,
      source,
      replaceState,
  }: Navigation): Promise<NavigationSetup> {
    this.logger.debug(`Navigation: initiated to the url '${url}'`);

    const baseUrl = stripOffQueryStringAndHash(url);

    if (baseUrl === '' || baseUrl === '/') {
      url = this.homeUrl;
      this.logger.debug(`Navigation: redirected to home url '${url}'`);
    }

    if (source !== NavigationTrigger.PopState) {
      this.setBrowserUrl(url, state, replaceState);
    }

    const route = this.resolveRoute(url);
    const appId = route.navItem.appIframeUrl;

    try {
      await this.handleBeforeNavigation();
      this.setNavUIState(route, state);
      await this.clientAppService.initiateClientApp(appId);
    } finally {
      this.busyIndicatorService.hide();
    }


    return {
      url,
      route,
      appId,
      source,
    };
  }

  private async processNavigation({
      url,
      route,
      appId,
      source,
  }: NavigationSetup): Promise<void> {
    try {
      this.clientAppService.activateApplication(appId);
      await this.navigate(url, route, source);
    } finally {
      this.navigating.next(false);
    }
  }

  private resolveRoute(url: string): Route {
    const route = this.matchRoute(url);

    if (!route) {
      this.menuStateService.deactivateMenuItem();
      this.currentNavItem = undefined;

      const publicDescription = this.translateService.instant('ERROR_UNKNOWN_URL', { url });
      throw new NotFoundError(publicDescription);
    }

    return route;
  }

  private async getClientApp(url: string): Promise<ClientApp> {
    // Ensure the app with the found id exists and it has the connected API
    const app = this.clientAppService.getApp(url);

    if (!app) {
      throw new NotFoundError(
        undefined,
        `There is no app with id="${url}"`,
      );
    }

    if (!app.api) {
      throw new InternalError(
        undefined,
        `The app with id="${url}" is not connected to the nav app`,
      );
    }

    return app;
  }

  private async handleBeforeNavigation(): Promise<void> {
    const activeApp = this.clientAppService.activeApp;

    if (activeApp && activeApp.api.beforeNavigation) {
      this.logger.debug(`Navigation: beforeNavigation() is called for '${activeApp.url}'`);

      const allowedToContinue = await activeApp.api.beforeNavigation();

      if (!allowedToContinue) {
        this.logger.debug(`Navigation: beforeNavigation() call is cancelled for '${activeApp.url}'`);
        throw new Error('Navigation is cancelled');
      }

      this.logger.debug(`Navigation: beforeNavigation() call has succeeded for '${activeApp.url}'`);
    }
  }

  private setNavUIState(route: Route, state: Record<string, unknown>): void {
    this.busyIndicatorService.show();
    this.navigating.next(true);
    this.currentNavItem = route.navItem;
    this.menuStateService.activateMenuItem(this.currentNavItem.appIframeUrl, this.currentNavItem.appPath);
    this.breadcrumbsService.setSuffix(state.breadcrumbLabel as string);
  }

  private async navigate(url: string, route: Route, source: NavigationTrigger): Promise<void> {
    const appBasePath = url.slice(route.path.length);

    const [
      appPathAddOn,
      queryStringAndHash,
    ] = this.urlMapperService.extractPathAndQueryStringAndHash(appBasePath);

    const navItem = route.navItem;

    const appPath = Location.joinWithSlash(navItem.appPath, appPathAddOn) + queryStringAndHash;
    const appPathWithoutLeadingSlash = this.urlMapperService.trimLeadingSlash(appPath);
    const appPathPrefix = new URL(navItem.appIframeUrl).pathname;
    const location = {
      pathPrefix: appPathPrefix,
      path: appPathWithoutLeadingSlash,
    };

    const app = await this.getClientApp(route.navItem.appIframeUrl);

    this.logger.debug(`Navigation: navigate() is called for '${app.url}'`, {
      location,
      source,
    });

    await app.api.navigate(location, source);

    this.logger.debug(`Navigation: navigate() call has succeeded for '${app.url}'`);
  }

  private setBrowserUrl(url: string, state: { [key: string]: any }, replaceState = false): void {
    if (!state.breadcrumbLabel) {
      delete state.breadcrumbLabel;
    }

    if (this.location.isCurrentPathEqualTo(url) || replaceState) {
      this.location.replaceState(url, '', state);
    } else {
      this.location.go(url, '', state);
    }
  }

  private generateRoutes(navItems: NavItem[]): Route[] {
    return navItems.map(navItem => ({
      path: this.location.normalize(this.urlMapperService.mapNavItemToBrowserUrl(navItem)),
      navItem,
    }));
  }

  private matchRoute(url: string): Route {
    if (Location.stripTrailingSlash(url) === Location.stripTrailingSlash(this.basePath)) {
      url = this.homeUrl;
    }

    return this.routes.find(x => url.startsWith(x.path));
  }
}
