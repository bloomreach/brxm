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

import { Location } from '@angular/common';
import { Injectable, OnDestroy } from '@angular/core';
import { NavigateFlags, NavItem, NavLocation } from '@bloomreach/navapp-communication';
import { BehaviorSubject, Observable, of, Subject, Subscription, throwError } from 'rxjs';
import { fromPromise } from 'rxjs/internal-compatibility';
import { catchError, skip, switchMap, tap } from 'rxjs/operators';

import { ClientAppService } from '../client-app/services/client-app.service';
import { AppError } from '../error-handling/models/app-error';
import { CriticalError } from '../error-handling/models/critical-error';
import { InternalError } from '../error-handling/models/internal-error';
import { NotFoundError } from '../error-handling/models/not-found-error';
import { ErrorHandlingService } from '../error-handling/services/error-handling.service';
import { MenuStateService } from '../main-menu/services/menu-state.service';
import { BreadcrumbsService } from '../top-panel/services/breadcrumbs.service';

import { NavigationStartEvent } from './events/navigation-start.event';
import { NavigationStopEvent } from './events/navigation-stop.event';
import { NavigationEvent } from './events/navigation.event';
import { GlobalSettingsService } from './global-settings.service';
import { NavConfigService } from './nav-config.service';
import { UrlMapperService } from './url-mapper.service';

interface Route {
  path: string;
  navItem: NavItem;
}

enum NavigationTrigger {
  Imperative,
  PopState,
}

interface Navigation {
  url: string;
  navItem: NavItem;
  appPathAddOn: string;
  clientAppFlags: NavigateFlags;
  state: { [key: string]: string };
  source: NavigationTrigger;
  replaceState: boolean;
  resolve: () => void;
  reject: (reason?: any) => void;
}

type Transition = Partial<Navigation>;

@Injectable({
  providedIn: 'root',
})
export class NavigationService implements OnDestroy {
  private routes: Route[];
  private locationSubscription: Subscription;
  private readonly transitions = new Subject<Transition | Error>();
  private readonly navigations = new BehaviorSubject<Navigation>({
    url: undefined,
    navItem: undefined,
    appPathAddOn: '',
    clientAppFlags: {},
    state: {},
    source: NavigationTrigger.Imperative,
    replaceState: false,
    resolve: undefined,
    reject: undefined,
  });
  private events = new Subject<NavigationEvent>();

  constructor(
    private location: Location,
    private navConfigService: NavConfigService,
    private clientAppService: ClientAppService,
    private menuStateService: MenuStateService,
    private breadcrumbsService: BreadcrumbsService,
    private urlMapperService: UrlMapperService,
    private settings: GlobalSettingsService,
    private errorHandlingService: ErrorHandlingService,
  ) {
    this.setupNavigations();
    this.processNavigations();
  }

  get events$(): Observable<NavigationEvent> {
    return this.events.asObservable();
  }

  private get basePath(): string {
    return this.urlMapperService.basePath;
  }

  private get homeUrl(): string {
    const homeMenuItem = this.menuStateService.homeMenuItem;

    if (!homeMenuItem) {
      throw new CriticalError('Configuration error', 'Unable to find home item');
    }

    return this.urlMapperService.mapNavItemToBrowserUrl(homeMenuItem.navItem);
  }

  initialNavigation(): Promise<void> {
    const navItems = this.navConfigService.navItems;
    this.routes = this.generateRoutes(navItems);

    this.setUpLocationChangeListener();

    const url = `${this.basePath}${this.settings.appSettings.initialPath}`;

    return this.scheduleNavigation(url, NavigationTrigger.Imperative, {}, {}, true);
  }

  ngOnDestroy(): void {
    if (this.locationSubscription) {
      this.locationSubscription.unsubscribe();
    }
  }

  navigateByNavItem(navItem: NavItem, breadcrumbLabel?: string, flags?: NavigateFlags): Promise<void> {
    const browserUrl = this.urlMapperService.mapNavItemToBrowserUrl(navItem);
    return this.navigateByUrl(browserUrl, breadcrumbLabel, flags);
  }

  navigateByNavLocation(navLocation: NavLocation): Promise<void> {
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

    return this.navigateByUrl(browserUrl, navLocation.breadcrumbLabel);
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

    this.menuStateService.activateMenuItem(navItem.appIframeUrl, navItem.appPath);
    this.breadcrumbsService.setSuffix(navLocation.breadcrumbLabel);

    this.setBrowserUrl(browserUrl, { breadcrumbLabel: navLocation.breadcrumbLabel });
  }

  navigateToDefaultCurrentAppPage(): Promise<void> {
    const lastNavigation = this.getLastNavigation();

    return this.navigateByNavItem(lastNavigation.navItem, '', { forceRefresh: true });
  }

  navigateToHome(): Promise<void> {
    return this.navigateByUrl(this.homeUrl);
  }

  private navigateByUrl(url: string, breadcrumbLabel?: string, flags?: NavigateFlags): Promise<void> {
    this.errorHandlingService.clearError();

    return this.scheduleNavigation(url, NavigationTrigger.Imperative, { breadcrumbLabel }, { ...flags });
  }

  private setUpLocationChangeListener(): void {
    if (this.locationSubscription) {
      return;
    }

    this.locationSubscription = this.location.subscribe(change => {
      this.errorHandlingService.clearError();

      let flags = {};
      try {
        flags = JSON.parse(change.state.flags);
        delete change.state.flags;
      } catch { }

      this.scheduleNavigation(change.url, NavigationTrigger.PopState, change.state || {}, flags);
    }) as any;
  }

  private setupNavigations(): void {
    this.transitions.pipe(
      tap(() => this.events.next(new NavigationStartEvent())),
      switchMap((t: Transition) => this.processTransition(t).pipe(
        catchError(error => {
          // Always resolve a promise (for now) to overcome consequent problems of handling promise rejection
          // t.reject(error);
          t.resolve();
          if (typeof error === 'string') {
            error = new InternalError(undefined, error);
          }

          return of(error);
        }),
      )),
      tap(() => this.events.next(new NavigationStopEvent())),
    ).subscribe((t: Navigation | Error) => {
      if (t instanceof AppError) {
        this.errorHandlingService.setError(t);

        return;
      }

      if (t instanceof Error) {
        this.errorHandlingService.setInternalError(undefined, t.message);

        return;
      }

      this.navigations.next(t as Navigation);
    });
  }

  private processNavigations(): void {
    this.navigations.pipe(
      skip(1),
    ).subscribe(
      n => {
        n.resolve();
        this.events.next(new NavigationStopEvent());
      },
      e => console.error(`Unhandled Navigation Error: ${e}`),
    );
  }

  private scheduleNavigation(
    url: string,
    source: NavigationTrigger,
    state: { [key: string]: string },
    flags: NavigateFlags,
    replaceState = false,
  ): Promise<void> {
    let resolve: () => void;
    let reject: () => void;

    const promise = new Promise<void>((res, rej) => {
      resolve = res;
      reject = rej;
    });

    this.transitions.next({
      url,
      state,
      source,
      clientAppFlags: flags,
      replaceState,
      resolve,
      reject,
    });

    return promise;
  }

  private processTransition(transition: Transition): Observable<Navigation> {
    return of(transition).pipe(
      // Eagerly update the browser url
      tap(t => {
        if (t.source === NavigationTrigger.Imperative) {
          t.state.flags = JSON.stringify(t.clientAppFlags);
          this.setBrowserUrl(t.url, t.state, t.replaceState);
        }
      }),
      // Resolving the url
      switchMap((t: Transition) => {
        const route = this.matchRoute(t.url);

        if (!route) {
          this.menuStateService.deactivateMenuItem();

          return throwError(new NotFoundError(`Unknown url: ${t.url}`));
        }

        if (!t.url.startsWith(route.path)) {
          t.url = route.path;
          t.state = {};
        }

        const appPathAddOn = t.url.slice(route.path.length);

        return of({ ...t, navItem: route.navItem, appPathAddOn });
      }),
      // Eagerly update the menu and teh breadcrumb label
      tap(t => {
        const { breadcrumbLabel } = t.state;

        this.menuStateService.activateMenuItem(t.navItem.appIframeUrl, t.navItem.appPath);
        this.breadcrumbsService.setSuffix(breadcrumbLabel);
      }),
      // Client navigation
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

        this.clientAppService.activateApplication(appId);

        const appPath = Location.joinWithSlash(t.navItem.appPath, t.appPathAddOn);
        const appPathWithoutLeadingSlash = this.urlMapperService.trimLeadingSlash(appPath);
        const appPathPrefix = new URL(t.navItem.appIframeUrl).pathname;

        const navigatePromise = app.api.navigate(
          { pathPrefix: appPathPrefix, path: appPathWithoutLeadingSlash },
          t.clientAppFlags,
        ).then(() => t as Navigation);

        return fromPromise(navigatePromise);
      }),
    );
  }

  private getLastNavigation(): Navigation {
    return this.navigations.value;
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
      path: this.urlMapperService.mapNavItemToBrowserUrl(navItem),
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
