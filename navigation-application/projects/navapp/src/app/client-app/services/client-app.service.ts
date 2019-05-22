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

import { Injectable, Renderer2, RendererFactory2 } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { map } from 'rxjs/operators';

import { NavItem } from '../../models';
import { NavigationConfigurationService } from '../../services/navigation-configuration.service';
import {
  ClientApplicationConfiguration,
  ClientApplicationHandler,
} from '../models';

@Injectable()
export class ClientAppService {
  private renderer: Renderer2;
  private applicationsConfigurations: ClientApplicationConfiguration[];
  private applications$ = new Subject<ClientApplicationHandler>();
  private iframes = new Map<string, ClientApplicationHandler>();

  constructor(
    private navConfigService: NavigationConfigurationService,
    private rendererFactory: RendererFactory2,
  ) {
    this.renderer = this.rendererFactory.createRenderer(undefined, undefined);

    this.navConfigService.navItems$
      .pipe(map(navItems => this.buildClientAppConfigurations(navItems)))
      .subscribe(appConfigs => (this.applicationsConfigurations = appConfigs));
  }

  get applicationCreated$(): Observable<ClientApplicationHandler> {
    return this.applications$.asObservable();
  }

  getApplicationHandler(id: string): ClientApplicationHandler {
    let handler = this.iframes.get(id);

    if (!handler) {
      handler = this.tryToCreateAnIframe(id);
      this.iframes.set(id, handler);
      this.applications$.next(handler);
    }

    return handler;
  }

  activateApplication(id: string): void {
    if (!this.iframes.has(id)) {
      throw new Error(`An attempt to activate non existing iframe id = ${id}`);
    }

    Array.from(this.iframes.values()).forEach(handler => {
      if (handler.url === id) {
        handler.iframeEl.classList.remove('hidden');
        return;
      }

      handler.iframeEl.classList.add('hidden');
    });
  }

  private tryToCreateAnIframe(id: string): ClientApplicationHandler {
    if (!this.applicationsConfigurations) {
      throw new Error(
        'An attempt to access applications configuration before it has been initialized.',
      );
    }

    const clientAppConfig = this.applicationsConfigurations.find(
      config => config.url === id,
    );

    if (!clientAppConfig) {
      throw new Error(
        `There is no configuration for the client application with id = ${id}`,
      );
    }

    const iframeEl = this.createIframe(clientAppConfig.url);

    return new ClientApplicationHandler(clientAppConfig.url, iframeEl);
  }

  private buildClientAppConfigurations(
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

  private createIframe(url: string): HTMLIFrameElement {
    const iframe: HTMLIFrameElement = this.renderer.createElement('iframe');
    iframe.src = url;

    return iframe;
  }
}
