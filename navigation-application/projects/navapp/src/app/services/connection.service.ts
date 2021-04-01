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

import { DOCUMENT } from '@angular/common';
import { Inject, Injectable, Renderer2, RendererFactory2 } from '@angular/core';
import {
  ChildApi,
  ClientError,
  connectToChild,
  getVersion as getCommunicationLibraryVersion,
  NavLocation,
  ParentApi,
} from '@bloomreach/navapp-communication';
import { NGXLogger } from 'ngx-logger';
import { Subject } from 'rxjs';

import { AppSettings } from '../models/dto/app-settings.dto';
import { UserSettings } from '../models/dto/user-settings.dto';

import { APP_SETTINGS } from './app-settings';
import { BusyIndicatorService } from './busy-indicator.service';
import { USER_SETTINGS } from './user-settings';

interface ChildConnection {
  iframe: HTMLIFrameElement;
  apiPromise: Promise<ChildApi>;
}

@Injectable({
  providedIn: 'root',
})
export class ConnectionService {
  private readonly connections = new Map<string, ChildConnection>();
  private readonly renderer: Renderer2 = this.rendererFactory.createRenderer(undefined, undefined);

  showMask$ = new Subject<void>();
  hideMask$ = new Subject<void>();
  navigate$ = new Subject<NavLocation>();
  updateNavLocation$ = new Subject<NavLocation>();
  onError$ = new Subject<ClientError>();
  onUserActivity$ = new Subject<void>();
  onSessionExpired$ = new Subject<void>();

  constructor(
    @Inject(APP_SETTINGS) private readonly appSettings: AppSettings,
    @Inject(USER_SETTINGS) private readonly userSettings: UserSettings,
    @Inject(DOCUMENT) private readonly document: Document,
    private readonly rendererFactory: RendererFactory2,
    private readonly busyIndicatorService: BusyIndicatorService,
    private readonly logger: NGXLogger,
  ) { }

  connect(url: string): Promise<ChildApi> {
    if (this.connections.has(url)) {
      return this.connections.get(url).apiPromise;
    }

    const iframe = this.createHiddenIframe(url);
    this.renderer.appendChild(this.document.body, iframe);

    const childApiPromise = this.connectToIframe(iframe);

    this.connections.set(url, {
      iframe,
      apiPromise: childApiPromise,
    });

    return childApiPromise;
  }

  disconnect(url: string): void {
    const connection = this.connections.get(url);

    this.connections.delete(url);

    if (connection && connection.iframe) {
      this.renderer.removeChild(this.document.body, connection.iframe);
    }
  }

  async connectToIframe(iframe: HTMLIFrameElement): Promise<ChildApi> {
    const url = iframe.src;
    const config = {
      iframe,
      methods: this.getParentApiMethods(url),
      connectionTimeout: this.appSettings.iframesConnectionTimeout,
      methodInvocationTimeout: this.appSettings.iframesConnectionTimeout,
    };

    this.logger.debug(`Initiating a connection to an iframe '${url}'`);

    try {
      return await connectToChild(config);
    } catch (error) {
      throw new Error(`Could not create a connection for '${url}': ${error}`);
    }
  }

  private getParentApiMethods(appUrl: string): ParentApi {
    return {
      getConfig: async () => ({
        apiVersion: getCommunicationLibraryVersion(),
        usageStatisticsEnabled: this.appSettings.usageStatisticsEnabled,
        userSettings: this.userSettings,
      }),
      showMask: async () => {
        this.logger.debug(`app '${appUrl}' called showMask()`);
        this.showMask$.next();
      },
      hideMask: async () => {
        this.logger.debug(`app '${appUrl}' called hideMask()`);
        this.hideMask$.next();
      },
      showBusyIndicator: async () => {
        this.logger.debug(`app '${appUrl}' called showBusyIndicator()`);
        this.busyIndicatorService.show();
      },
      hideBusyIndicator: async () => {
        this.logger.debug(`app '${appUrl}' called hideBusyIndicator()`);
        this.busyIndicatorService.hide();
      },
      navigate: async (location: NavLocation) => {
        this.logger.debug(`app '${appUrl}' called navigate()`, location);
        this.navigate$.next(location);
      },
      updateNavLocation: async (location: NavLocation) => {
        this.logger.debug(`app '${appUrl}' called updateNavLocation()`, location);
        this.updateNavLocation$.next(location);
      },
      onError: async (clientError: ClientError) => {
        this.logger.debug(`app '${appUrl}' called onError()`, clientError);
        this.onError$.next(clientError);
      },
      onSessionExpired: async () => {
        this.logger.debug(`app '${appUrl}' called onSessionExpired()`);
        this.onSessionExpired$.next();
      },
      onUserActivity: async () => this.onUserActivity$.next(),
    };
  }

  private createHiddenIframe(url: string): HTMLIFrameElement {
    const iframe = this.document.createElement('iframe');
    iframe.src = url;
    iframe.style.visibility = 'hidden';
    iframe.style.position = 'absolute';
    iframe.style.width = '1px';
    iframe.style.height = '1px';

    return iframe;
  }
}
