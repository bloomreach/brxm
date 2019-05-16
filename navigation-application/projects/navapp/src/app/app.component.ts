import { HttpClient } from '@angular/common/http';
import { Component, ElementRef, OnInit, Renderer2, RendererFactory2 } from '@angular/core';
import { ChildConnectConfig, connectToChild } from '@bloomreach/navapp-communication';
import { NavItem } from '@bloomreach/navapp-communication/lib/api';

import { NavConfigResource } from './models';
import { NavAppSettingsService, NavigationConfigurationService } from './services';

import { MenuStateService } from './main-menu/services';

@Component({
  selector: 'brna-root',
  templateUrl: './app.component.html',
  styleUrls: ['app.component.scss'],
})
export class AppComponent implements OnInit {
  private renderer: Renderer2;
  constructor(
    private menuStateService: MenuStateService,
    private navAppSettings: NavAppSettingsService,
    private navigationConfigurationService: NavigationConfigurationService,
    private http: HttpClient,
    private elRef: ElementRef,
    private rendererFactory: RendererFactory2,
  ) {}

  ngOnInit(): void {
    this.renderer = this.rendererFactory.createRenderer(undefined, undefined);

    const resourcePromises = this.navAppSettings.appSettings.navConfigResources.map(resource =>
      this.getNavConfig(resource),
    );

    Promise.all(resourcePromises)
      .then(navItemArrays => [].concat(...navItemArrays))
      .then(navItems => this.navigationConfigurationService.setNavigationConfiguration(navItems));
  }

  get isMenuCollapsed(): boolean {
    return this.menuStateService.isMenuCollapsed;
  }

  private getNavConfig(resource: NavConfigResource): Promise<NavItem[]> {
    if (resource.resourceType === 'iframe') {
      return this.getConfigFromIframe(resource.url);
    } else if (resource.resourceType === 'REST') {
      return this.getConfigFromREST(resource.url);
    }
  }

  private getConfigFromREST(url: string): any {
    return this.http.get(url);
  }

  private getConfigFromIframe(url: string): Promise<NavItem[]> {
    const iframe = document.createElement('iframe');
    iframe.src = url;
    iframe.style.visibility = 'hidden';
    iframe.style.position = 'absolute';
    this.renderer.appendChild(this.elRef.nativeElement, iframe);

    const config: ChildConnectConfig = {
      iframe,
    };

    return connectToChild(config)
      .then(child => child.getNavItems())
      .finally(() => this.renderer.removeChild(this.elRef.nativeElement, iframe));
  }
}
