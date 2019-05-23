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
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { NavItem } from '../../models';
import { NavigationConfigurationService } from '../../services/navigation-configuration.service';
import { ClientApplicationConfiguration } from '../models';

@Injectable()
export class ClientAppService {
  private appConfigs = new BehaviorSubject<ClientApplicationConfiguration[]>(
    [],
  );

  private activeAppId = new BehaviorSubject<string>(undefined);

  constructor(private navConfigService: NavigationConfigurationService) {
    this.navConfigService.navItems$
      .pipe(map(navItems => this.buildAppConfigs(navItems)))
      .subscribe(appConfigs => {
        this.appConfigs.next(appConfigs);
      });
  }

  get appConfigs$(): Observable<ClientApplicationConfiguration[]> {
    return this.appConfigs.asObservable();
  }

  get activeAppId$(): Observable<string> {
    return this.activeAppId.asObservable();
  }

  activateApplication(id: string): void {
    this.activeAppId.next(id);
  }

  private buildAppConfigs(
    navItems: NavItem[],
  ): ClientApplicationConfiguration[] {
    const uniqueUrlsSet = navItems.reduce((uniqueUrls, config) => {
      uniqueUrls.add(config.appIframeUrl);
      return uniqueUrls;
    }, new Set<string>());

    return Array.from(uniqueUrlsSet.values()).map(
      url => new ClientApplicationConfiguration(url, url),
    );
  }
}
