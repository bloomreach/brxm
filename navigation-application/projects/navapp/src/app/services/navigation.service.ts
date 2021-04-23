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
import { NavigationTrigger, NavLocation } from '@bloomreach/navapp-communication';
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
import { NavItem } from '../models/nav-item.model';
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
  navItem: NavItem;
  appPathAddOn: string;
  queryStringAndHash: string;
  state: { [key: string]: string };
  source: NavigationTrigger;
  app: ClientApp;
  replaceState: boolean;
  resolve: () => void;
  reject: (reason?: any) => void;
}

type Transition = Partial<Navigation>;

@Injectable({
  providedIn: 'root',
})
export class NavigationService implements OnDestroy {

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
  private readonly transitions = new Subject<Transition | Error>();
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

    this.setupNavigations();
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

  private setupNavigations(): void {
    this.transitions.pipe(
      switchMap((t: Transition) => this.processTransition(t).pipe(
        catchError(error => {
          if (typeof error === 'string') {
            error = new InternalError(undefined, error);
          }

          return of(error);
        }),
        finalize(() => {
          // Always resolve a promise (for now) to overcome consequent problems of handling promise rejection
          // t.reject(error);
          t.resolve();

          this.busyIndicatorService.hide();
          this.navigating.next(false);
        }),
      )),
    ).subscribe((t: Navigation | Error) => {
      if (t instanceof AppError) {
        this.errorHandlingService.setError(t);

        return;
      }

      if (t instanceof Error) {
        this.errorHandlingService.setInternalError(undefined, t.message);

        return;
      }
    });
  }

  private scheduleNavigation(
    url: string,
    source: NavigationTrigger,
    state: { [key: string]: string } = {},
    replaceState = false,
  ): Promise<void> {
    let resolve: () => void;
    let reject: () => void;

    const promise = new Promise<void>((res, rej) => {
      resolve = res;
      reject = rej;
    });

    const normalizedUrl = this.location.normalize(url);

    this.transitions.next({
      url: normalizedUrl,
      state,
      source,
      replaceState,
      resolve,
      reject,
    });

    return promise;
  }

  private processTransition(transition: Transition): Observable<Navigation> {
    return of(transition).pipe(
      // Redirect all empty urls to the home url
      tap(t => {
        this.logger.debug(`Navigation: initiated to the url '${t.url}'`);
        const url = stripOffQueryStringAndHash(t.url);

        if (url === '' || url === '/') {
          t.url = this.homeUrl;
          this.logger.debug(`Navigation: redirected to home url '${t.url}'`);
        }
      }),
      // Eagerly update the browser url
      tap(t => {
        if (t.source !== NavigationTrigger.PopState) {
          this.setBrowserUrl(t.url, t.state, t.replaceState);
        }
      }),
      // Resolving the url
      switchMap((t: Transition) => {
        const route = this.matchRoute(t.url);

        if (!route) {
          this.menuStateService.deactivateMenuItem();
          this.currentNavItem = undefined;

          const publicDescription = this.translateService.instant('ERROR_UNKNOWN_URL', { url: t.url });
          return throwError(new NotFoundError(publicDescription));
        }

        const appPathAddOn = t.url.slice(route.path.length);
        const [
          appPathAddOnWithoutQueryStringAndHash,
          queryStringAndHash,
        ] = this.urlMapperService.extractPathAndQueryStringAndHash(appPathAddOn);

        return of({ ...t, navItem: route.navItem, appPathAddOn: appPathAddOnWithoutQueryStringAndHash, queryStringAndHash });
      }),
      // Wait for the nav app to be ready
      switchMap(t => t.navItem.active$.pipe(
        filter(x => x),
        mapTo(t),
        take(1),
      )),
      // Ensure the app with the found id exists and it has the connected API
      switchMap(t => {
        const appId = t.navItem.appIframeUrl;
        const app = this.clientAppService.getApp(appId);

        if (!app) {
          return throwError(new NotFoundError(
            undefined,
            `There is no app with id="${appId}"`,
          ));
        }

        if (!app.api) {
          return throwError(new InternalError(
            undefined,
            `The app with id="${appId}" is not connected to the nav app`,
          ));
        }

        return of({ ...t, app });
      }),
      // Process beforeNavigation
      switchMap(t => {
        if (!t.app.api.beforeNavigation) {
          return of(t);
        }

        this.logger.debug(`Navigation: beforeNavigation() is called for '${t.app.url}'`);

        return from(t.app.api.beforeNavigation()).pipe(
          tap(allowedToContinue => {
            if (allowedToContinue) {
              this.logger.debug(`Navigation: beforeNavigation() call is succeeded for '${t.app.url}'`);
              return;
            }

            this.logger.debug(`Navigation: beforeNavigation() call is cancelled for '${t.app.url}'`);
          }),
          switchMap(allowedToContinue => allowedToContinue ? of(t) : EMPTY),
        );
      }),
      tap(() => {
        this.busyIndicatorService.show();
        this.navigating.next(true);
      }),
      // Eagerly update the menu and the breadcrumb label
      tap(t => {
        const { breadcrumbLabel } = t.state;

        this.currentNavItem = t.navItem;
        this.menuStateService.activateMenuItem(t.navItem.appIframeUrl, t.navItem.appPath);
        this.breadcrumbsService.setSuffix(breadcrumbLabel);
      }),
      // Activate the app
      tap(t => {
        const appId = t.navItem.appIframeUrl;

        this.clientAppService.activateApplication(appId);
      }),
      // Process navigation
      switchMap((t: Transition) => {
        const appPath = Location.joinWithSlash(t.navItem.appPath, t.appPathAddOn) + t.queryStringAndHash;
        const appPathWithoutLeadingSlash = this.urlMapperService.trimLeadingSlash(appPath);
        const appPathPrefix = new URL(t.navItem.appIframeUrl).pathname;
        const location = {
          pathPrefix: appPathPrefix,
          path: appPathWithoutLeadingSlash,
        };

        this.logger.debug(`Navigation: navigate() is called for '${t.app.url}'`, {
          location,
          source: t.source,
        });

        const navigationPromise = t.app.api.navigate(location, t.source);

        return from(navigationPromise).pipe(
          mapTo(t as Navigation),
          tap(x => this.logger.debug(`Navigation: navigate() call is succeeded for '${x.app.url}'`)),
        );
      }),
    ) as Observable<Navigation>;
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
