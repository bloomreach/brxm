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
import { ChildPromisedApi } from '@bloomreach/navapp-communication';
import { BehaviorSubject, Observable, of, Subject } from 'rxjs';
import { map } from 'rxjs/operators';

import { NavItem } from '../../models';
import { NavConfigService } from '../../services/nav-config.service';
import { ClientApp } from '../models/client-app.model';

@Injectable()
export class ClientAppService {
  get apps$(): Observable<ClientApp[]> {
    return this.apps.asObservable();
  }

  get activeAppId$(): Observable<string> {
    return this.activeAppId.asObservable();
  }

  get connectionsEstablished$(): Observable<boolean> {
    return this.connectionsEstablished.asObservable();
  }

  private apps = new BehaviorSubject<ClientApp[]>([]);
  private activeAppId = new BehaviorSubject<string>(undefined);
  private connectionsEstablished = new Subject<boolean>();

  constructor(private navConfigService: NavConfigService) {}

  init(): void {
    this.navConfigService.navItems$
      .pipe(
        map(navItems => this.filterUniqueURLs(navItems)),
        map(uniqueURLs => uniqueURLs.map(url => new ClientApp(url))),
      )
      .subscribe(apps => this.apps.next(apps));
  }

  activateApplication(id: string): void {
    this.activeAppId.next(id);
  }

  addConnection(app: ClientApp, api: ChildPromisedApi): void {
    app.api = api;
    this.updateApp(app);

    this.checkEstablishedConnections();
  }

  getApp(id: string): ClientApp {
    const apps = this.apps.getValue();
    const app = apps.find(a => a.id === id);
    if (!app) {
      throw new Error(`There is no connection to an ifrane with id = ${id}`);
    }

    return app;
  }

  private updateApp(app: ClientApp): void {
    const apps = this.apps.getValue();

    apps.map(a => {
      if (a.id === app.id) {
        a = app;
      }
    });

    this.apps.next(apps);
  }

  private checkEstablishedConnections(): void {
    const apps = this.apps.getValue();
    const allConnected = apps.every((a: ClientApp) => a.api !== undefined);

    if (allConnected) {
      this.connectionsEstablished.next(true);
      this.connectionsEstablished.complete();
    }
  }

  private filterUniqueURLs(navItems: NavItem[]): string[] {
    const uniqueUrlsSet = navItems.reduce((uniqueUrls, config) => {
      uniqueUrls.add(config.appIframeUrl);
      return uniqueUrls;
    }, new Set<string>());

    return Array.from(uniqueUrlsSet.values());
  }
}
