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
import { NavigateFlags, NavItem } from '@bloomreach/navapp-communication';
import { BehaviorSubject, Observable, of, Subject, Subscription, throwError } from 'rxjs';
import { fromPromise } from 'rxjs/internal-compatibility';
import { finalize, map, skip, switchMap, tap } from 'rxjs/operators';

import { ClientAppService } from '../client-app/services/client-app.service';
import { MenuStateService } from '../main-menu/services/menu-state.service';
import { GlobalSettingsService } from '../services/global-settings.service';
import { NavConfigService } from '../services/nav-config.service';
import { BreadcrumbsService } from '../top-panel/services/breadcrumbs.service';

import { NavigationStartEvent } from './events/navigation-start.event';
import { NavigationStopEvent } from './events/navigation-stop.event';
import { NavigationEvent } from './events/navigation.event';

const trimLeadingSlash = (value: string) => value.replace(/^\//, '');
const trimSlashes = (value: string) => trimLeadingSlash(value).replace(/\/$/, '');
const combinePathParts = (...parts) => parts.filter(x => x.length > 0).join('/');

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
  appUrl: string;
  clientAppFlags: NavigateFlags;
  state: { [key: string]: string };
  source: NavigationTrigger;
}

@Injectable({
  providedIn: 'root',
})
export class DeepLinkingService implements OnDestroy {
  private routes: Route[] = [];
  private locationSubscription: Subscription;
  private readonly transitions = new Subject<Partial<Navigation>>();
  private readonly navigations = new BehaviorSubject<Navigation>({
    url: undefined,
    navItem: undefined,
    appUrl: undefined,
    clientAppFlags: {},
    state: {},
    source: NavigationTrigger.Imperative,
  });
  private events = new Subject<NavigationEvent>();

  constructor(
    private location: Location,
    private navConfigService: NavConfigService,
    private clientAppService: ClientAppService,
    private menuStateService: MenuStateService,
    private breadcrumbsService: BreadcrumbsService,
    private settings: GlobalSettingsService,
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
    return this.settings.appSettings.homeUrl ? trimSlashes(this.settings.appSettings.homeUrl) : this.routes.length && this.routes[0].path;
  }

  initialNavigation(): void {
    this.setUpLocationChangeListener();
    this.navigateByUrl(this.location.path(true));
  }

  ngOnDestroy(): void {
    if (this.locationSubscription) {
      this.locationSubscription.unsubscribe();
    }
  }

  navigateByAppUrl(appIframeUrl: string, appUrl: string, breadcrumbLabel?: string, flags?: NavigateFlags): void {
    const browserUrl = this.convertAppUrlBrowserUrl(appIframeUrl, appUrl);
    this.navigateByUrl(browserUrl, breadcrumbLabel, flags);
  }

  updateByAppUrl(appIframeUrl: string, appUrl: string, breadcrumbLabel?: string): void {
    const browserUrl = this.convertAppUrlBrowserUrl(appIframeUrl, appUrl);

    this.menuStateService.activateMenuItem(appIframeUrl, appUrl);
    this.breadcrumbsService.setSuffix(breadcrumbLabel);

    this.setBrowserUrl(browserUrl, { breadcrumbLabel });
  }

  navigateToDefaultCurrentAppPage(): void {
    const navigation = this.getLastNavigation();

    this.navigateByAppUrl(navigation.navItem.appIframeUrl, navigation.navItem.appPath);
  }

  navigateByUrl(url: string, breadcrumbLabel?: string, flags?: NavigateFlags): void {
    this.scheduleNavigation(url, NavigationTrigger.Imperative, { breadcrumbLabel }, { ...flags });
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

      this.scheduleNavigation(change.url, NavigationTrigger.PopState, change.state, flags);
    }) as any;
  }

  private setupNavigations(): void {
    this.transitions.pipe(
      tap(() => this.events.next(new NavigationStartEvent())),
      map(t => ({ ...t, url: trimLeadingSlash(t.url) })),
      switchMap(t => {
        const [matchedUrl, route] = this.matchRoute(t.url, this.routes);

        if (!route) {
          return throwError(`Unknown url: ${t.url}`);
        }

        return of({ ...t, url: matchedUrl, navItem: route.navItem });
      }),
      map(t => ({ ...t, appUrl: this.convertBrowserUrlToAppUrl(t.url, t.navItem) })),
      switchMap(t => {
        const appId = t.navItem.appIframeUrl;
        const app = this.clientAppService.getApp(appId);

        if (!app) {
          throwError(`There is no app with id="${appId}"`);
        }

        if (!app.api) {
          throwError(`The app with id="${appId}" is not connected to the nav app`);
        }

        return fromPromise(app.api.navigate({ path: t.appUrl }, t.clientAppFlags)).pipe(
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
          this.setBrowserUrl(n.url, n.state);
        }

        this.events.next(new NavigationStopEvent());
      },
      e => console.warn(`Unhandled Navigation Error: ${e}`),
    );
  }

  private generateRoutes(navItems: NavItem[]): Route[] {
    const routes: Route[] = navItems.map(navItem => {

      return {
        path: this.convertAppUrlBrowserUrl(navItem.appIframeUrl, navItem.appPath),
        navItem,
      };
    });

    const defaultRoute: Route = {
      path: '**',
      redirectTo: this.homeUrl,
    };

    return routes.concat([defaultRoute]);
  }

  private matchRoute(url: string, routes: Route[]): [string, Route] {
    const route = routes.find(x => url.startsWith(x.path) || x.path === '**');

    if (!route) {
      return [undefined, undefined];
    }

    if (route.redirectTo) {
      const routesExceptCurrent = routes.filter(x => x.path !== route.path);
      return this.matchRoute(route.redirectTo, routesExceptCurrent);
    }

    return [url, route];
  }

  private scheduleNavigation(url: string, source: NavigationTrigger, state: { [key: string]: string }, flags: NavigateFlags): void {
    this.transitions.next({
      url,
      state,
      source,
      clientAppFlags: flags,
    });
  }

  private getLastNavigation(): Navigation {
    return this.navigations.value;
  }

  private setBrowserUrl(url: string, state: { [key: string]: any }): void {
    url = `/${trimLeadingSlash(url)}`;

    if (this.location.isCurrentPathEqualTo(url)) {
      this.location.replaceState(url, '', state);
    } else {
      this.location.go(url, '', state);
    }
  }

  private convertBrowserUrlToAppUrl(browserUrl: string, matchedKnownNavItem: NavItem): string {
    const knownBrowserUrl = this.convertAppUrlBrowserUrl(matchedKnownNavItem.appIframeUrl, matchedKnownNavItem.appPath);

    if (!browserUrl.startsWith(knownBrowserUrl)) {
      throw new Error(`Browser user ${browserUrl} and current nav item's url ${knownBrowserUrl} do not match`);
    }

    const addon = browserUrl.slice(knownBrowserUrl.length + 1);

    return combinePathParts(matchedKnownNavItem.appPath, addon);
  }

  private convertAppUrlBrowserUrl(appIframeUrl: string, appUrl: string): string {
    const contextPath = trimSlashes(this.settings.appSettings.contextPath);
    const appBasePath = trimSlashes(new URL(appIframeUrl).pathname);

    return combinePathParts(contextPath, appBasePath, appUrl);
  }
}
