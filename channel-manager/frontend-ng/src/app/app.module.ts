/*
 * Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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

import { NgModule } from '@angular/core';
import { UpgradeModule  } from '@angular/upgrade/static';

import { RightSidePanelModule } from './channel/sidePanels/rightSidePanel/right-side-panel.module';
import { SharedModule } from './shared/shared.module';

import ng1Module from './hippo-cm.ng1.module.js';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { TranslateHttpLoader } from '@ngx-translate/http-loader';
import { TranslateModule, TranslateLoader } from '@ngx-translate/core';
import { DomSanitizer } from "@angular/platform-browser";
import { MdIconRegistry } from "@angular/material";
import { HttpModule } from '@angular/http';

declare global {
  interface Window { Hippo: any; }
}

function getAntiCache() {
  const results = new RegExp('[?&]antiCache=([^&#]*)').exec(document.location.href);
  const now = new Date().toLocaleString();
  return results ? results[1] : now;
}

export function HttpLoaderFactory(http: HttpClient) {
  return new TranslateHttpLoader(http, 'i18n/', `.json?antiCache=${getAntiCache()}`);
}

@NgModule({
  imports: [
    HttpModule,
    SharedModule,
    UpgradeModule,
    RightSidePanelModule,
    HttpClientModule,
    TranslateModule.forRoot({
      loader: {
        provide: TranslateLoader,
        useFactory: HttpLoaderFactory,
        deps: [HttpClient]
      }
    })

  ]
})
export class AppModule {
  constructor(private upgrade: UpgradeModule, private iconRegistry: MdIconRegistry, sanitizer: DomSanitizer) {
    const HippoGlobal = window.parent.Hippo || {};
    const antiCache = HippoGlobal.antiCache ? `?antiCache=${window.top.Hippo.antiCache}` : '';

    const svgIconsList = [
      'any-device',
      'attention',
      'back',
      'close',
      'desktop',
      'document-status-changed',
      'document-status-live',
      'document-status-new',
      'document',
      'folder-closed',
      'folder-open',
      'left-side-panel-arrow-right',
      'left-side-panel-arrow-left',
      'maximize-sidepanel',
      'phone',
      'publish',
      'resize-handle',
      'switch-to-content-editor',
      'tablet',
      'toggle_components_overlay',
      'un-maximize-sidepanel',
    ];

    svgIconsList.forEach((icon) => {
      const iconUrl = `images/${icon}.svg${antiCache}`;
      iconRegistry.addSvgIcon(icon ,sanitizer.bypassSecurityTrustResourceUrl(iconUrl));
    });
  }

  ngDoBootstrap() {
    this.upgrade.bootstrap(document.body, [ng1Module], { strictDi: true });
  }
}

