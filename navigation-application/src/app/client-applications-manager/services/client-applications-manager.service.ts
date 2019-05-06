/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { Injectable, Renderer2, RendererFactory2 } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { map } from 'rxjs/operators';

import { NavigationConfigurationService } from '../../services';
import { ClientApplicationConfiguration, ClientApplicationHandler } from '../models';

import { ClientApplicationsRegistryService } from './client-applications-registry.service';
import { NavItem } from '../../models';

@Injectable()
export class ClientApplicationsManagerService {
  private renderer: Renderer2;
  private applicationsConfigurations: ClientApplicationConfiguration[];
  private applications$ = new Subject<ClientApplicationHandler>();

  get applicationCreated$(): Observable<ClientApplicationHandler> {
    return this.applications$.asObservable();
  }

  constructor(
    private registry: ClientApplicationsRegistryService,
    navConfigService: NavigationConfigurationService,
    rendererFactory: RendererFactory2,
  ) {
    this.renderer = rendererFactory.createRenderer(undefined, undefined);

    navConfigService.navigationConfiguration$.pipe(
      map(navConfig => this.buildClientApplicationsConfigurations(navConfig.values())),
    ).subscribe(appConfigs => this.applicationsConfigurations = appConfigs);
  }

  getApplicationHandler(id: string): ClientApplicationHandler {
    let handler = this.registry.get(id);

    if (!handler) {
      handler = this.tryToCreateAnIframe(id);
      this.registry.set(id, handler);
      this.applications$.next(handler);
    }

    return handler;
  }

  activateApplication(id: string): void {
    if (!this.registry.has(id)) {
      throw new Error(`An attempt to activate non existing iframe id = ${id}`);
    }

    this.registry.getAll().forEach(handler => {
      if (handler.url === id) {
        handler.iframeEl.classList.remove('hidden');
        return;
      }

      handler.iframeEl.classList.add('hidden');
    });
  }

  private tryToCreateAnIframe(id: string): ClientApplicationHandler {
    if (!this.applicationsConfigurations) {
      throw new Error('An attempt to access applications configuration before it has been initialized.');
    }

    const clientAppConfig = this.applicationsConfigurations.find(config => config.url === id);

    if (!clientAppConfig) {
      throw new Error(`There is no configuration for the client application with id = ${id}`);
    }

    const iframeEl = this.createIframe(clientAppConfig.url);

    return new ClientApplicationHandler(clientAppConfig.url, iframeEl);
  }

  private buildClientApplicationsConfigurations(navConfig: IterableIterator<NavItem>): ClientApplicationConfiguration[] {
    const uniqueUrlsSet = Array.from(navConfig).reduce(
      (uniqueUrls, config) => {
        uniqueUrls.add(config.appIframeUrl);
        return uniqueUrls;
      },
      new Set<string>(),
    );

    return Array.from(uniqueUrlsSet.values()).map(url => new ClientApplicationConfiguration(url, url));
  }

  private createIframe(url: string): HTMLIFrameElement {
    const iframe: HTMLIFrameElement = this.renderer.createElement('iframe');
    iframe.src = url;

    return iframe;
  }
}
