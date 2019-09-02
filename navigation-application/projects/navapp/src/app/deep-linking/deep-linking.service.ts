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
import { finalize, map, skip, switchMap, tap } from 'rxjs/operators';

import { ClientAppService } from '../client-app/services/client-app.service';
import { MenuStateService } from '../main-menu/services/menu-state.service';
import { NavConfigService } from '../services/nav-config.service';
import { BreadcrumbsService } from '../top-panel/services/breadcrumbs.service';

import { NavigationStartEvent } from './events/navigation-start.event';
import { NavigationStopEvent } from './events/navigation-stop.event';
import { NavigationEvent } from './events/navigation.event';
import { UrlMapperService } from './url-mapper.service';

interface Route {
  path: string;
  navItem?: NavItem;
  redirectTo?: string;
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
}

@Injectable({
  providedIn: 'root',
})
export class DeepLinkingService implements OnDestroy {
  private readonly routes: Route[];
  private locationSubscription: Subscription;
  private readonly transitions = new Subject<Partial<Navigation>>();
  private readonly navigations = new BehaviorSubject<Navigation>({
    url: undefined,
    navItem: undefined,
    appPathAddOn: '',
    clientAppFlags: {},
    state: {},
    source: NavigationTrigger.Imperative,
    replaceState: false,
  });
  private events = new Subject<NavigationEvent>();

  constructor(
    private location: Location,
    private navConfigService: NavConfigService,
    private clientAppService: ClientAppService,
    private menuStateService: MenuStateService,
    private breadcrumbsService: BreadcrumbsService,
    private urlMapperService: UrlMapperService,
  ) {
    this.setupNavigations();
    this.processNavigations();

    const navItems = this.navConfigService.navItems;
    this.routes = this.generateRoutes(navItems);
  }

  get events$(): Observable<NavigationEvent> {
    return this.events.asObservable();
  }

  private get homeUrl(): string {
    const homeMenuItem = this.menuStateService.homeMenuItem;
    const homeUrl = homeMenuItem ?
      this.convertAppUrlToBrowserUrl(homeMenuItem.navItem.appIframeUrl, homeMenuItem.navItem.appPath) :
      '';

    return homeUrl;
  }

  initialNavigation(): void {
    this.setUpLocationChangeListener();

    const url = this.location.path(true);

    this.scheduleNavigation(url, NavigationTrigger.Imperative, {}, {}, true);
  }

  ngOnDestroy(): void {
    if (this.locationSubscription) {
      this.locationSubscription.unsubscribe();
    }
  }

  navigateByNavItem(navItem: NavItem, breadcrumbLabel?: string, flags?: NavigateFlags): void {
    const browserUrl = this.urlMapperService.mapNavItemToBrowserUrl(navItem);
    this.navigateByUrl(browserUrl, breadcrumbLabel, flags);
  }

  navigateByNavLocation(navLocation: NavLocation): void {
    let browserUrl: string;

    try {
      browserUrl = this.urlMapperService.mapNavLocationToBrowserUrl(navLocation, true)[0];
    } catch (e) {
      console.error(`An attempt to navigate was failed due to app path is not allowable: '${navLocation.path}'`);
      return;
    }

    this.navigateByUrl(browserUrl, navLocation.breadcrumbLabel);
  }

  updateByNavLocation(navLocation: NavLocation): void {
    let browserUrl: string;
    let navItem: NavItem;

    try {
      [browserUrl, navItem] = this.urlMapperService.mapNavLocationToBrowserUrl(navLocation, true);
    } catch (e) {
      console.error(`An attempt to update the app url was failed due to app path is not allowable: '${navLocation.path}'`);
      return;
    }

    this.menuStateService.activateMenuItem(navItem.appIframeUrl, navItem.appPath);
    this.breadcrumbsService.setSuffix(navLocation.breadcrumbLabel);

    this.setBrowserUrl(browserUrl, { breadcrumbLabel: navLocation.breadcrumbLabel });
  }

  navigateToDefaultCurrentAppPage(): void {
    const lastNavigation = this.getLastNavigation();

    this.navigateByNavItem(lastNavigation.navItem, '', { forceRefresh: true });
  }

  private navigateByUrl(url: string, breadcrumbLabel?: string, flags?: NavigateFlags): void {
    this.scheduleNavigation(url, NavigationTrigger.Imperative, { breadcrumbLabel }, { ...flags });
  }

  navigateToHome(): void {
    this.navigateByUrl(this.homeUrl);
  }

  private setUpLocationChangeListener(): void {
    if (this.locationSubscription) {
      return;
    }

    this.locationSubscription = this.location.subscribe(change => {
      let flags = {};
      try {
        flags = JSON.parse(change.state.flags);
        delete change.state.flags;
      } catch {}

      this.scheduleNavigation(change.url, NavigationTrigger.PopState, change.state || {}, flags);
    }) as any;
  }

  private setupNavigations(): void {
    this.transitions.pipe(
      tap(() => this.events.next(new NavigationStartEvent())),
      switchMap(t => {
        const route = this.matchRoute(t.url, this.routes);

        if (!route) {
          return throwError(`Unknown url: ${t.url}`);
        }

        if (!t.url.startsWith(route.path)) {
          t.url = route.path;
          t.state = {};
        }

        const appPathAddOn = t.url.slice(route.path.length);

        return of({ ...t, navItem: route.navItem, appPathAddOn });
      }),
      switchMap(t => {
        const appId = t.navItem.appIframeUrl;
        const app = this.clientAppService.getApp(appId);

        if (!app) {
          throwError(`There is no app with id="${appId}"`);
        }

        if (!app.api) {
          throwError(`The app with id="${appId}" is not connected to the nav app`);
        }

        const appPath = this.urlMapperService.combinePathParts(t.navItem.appPath, t.appPathAddOn);
        const appPathWithoutLeadingSlash = this.urlMapperService.trimLeadingSlash(appPath);

        return fromPromise(app.api.navigate({ path: appPathWithoutLeadingSlash }, t.clientAppFlags)).pipe(
          map(() => t),
        );
      }),
      finalize(() => this.events.next(new NavigationStopEvent())),
    ).subscribe(t => this.navigations.next(t as Navigation));
  }

  private processNavigations(): void {
    this.navigations.pipe(
      skip(1),
    ).subscribe(
      n => {
        const appId = n.navItem.appIframeUrl;
        this.clientAppService.activateApplication(appId);

        const { breadcrumbLabel } = n.state;

        this.menuStateService.activateMenuItem(n.navItem.appIframeUrl, n.navItem.appPath);
        this.breadcrumbsService.setSuffix(breadcrumbLabel);

        if (n.source === NavigationTrigger.Imperative) {
          n.state.flags = JSON.stringify(n.clientAppFlags);
          this.setBrowserUrl(n.url, n.state, n.replaceState);
        }

        this.events.next(new NavigationStopEvent());
      },
      e => console.warn(`Unhandled Navigation Error: ${e}`),
    );
  }

  private scheduleNavigation(
    url: string,
    source: NavigationTrigger,
    state: { [key: string]: string },
    flags: NavigateFlags,
    replaceState = false,
  ): void {
    this.transitions.next({
      url,
      state,
      source,
      clientAppFlags: flags,
      replaceState,
    });
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
    const routes: Route[] = navItems.map(navItem => ({
      path: this.urlMapperService.mapNavItemToBrowserUrl(navItem),
      navItem,
    }));

    const homeMenuItem = this.menuStateService.homeMenuItem;
    const homeUrl = homeMenuItem ? this.urlMapperService.mapNavItemToBrowserUrl(homeMenuItem.navItem) : '';

    const defaultRoute: Route = {
      path: '**',
      redirectTo: homeUrl,
    };

    return routes.concat([defaultRoute]);
  }

  private matchRoute(url: string, routes: Route[]): Route {
    const route = routes.find(x => url.startsWith(x.path) || x.path === '**');

    if (!route) {
      return;
    }

    if (route.hasOwnProperty('redirectTo')) {
      const routesExceptCurrent = routes.filter(x => x.path !== route.path);
      return this.matchRoute(route.redirectTo || '', routesExceptCurrent);
    }

    return route;
  }
}
