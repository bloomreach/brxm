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

import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import {
  MatButtonModule,
  MatIconModule,
  MatIconRegistry,
  MatProgressBarModule,
  MatRippleModule,
  MatSidenavModule,
  MatTreeModule,
} from '@angular/material';
import { DomSanitizer } from '@angular/platform-browser';

@NgModule({
  exports: [
    MatRippleModule,
    MatIconModule,
    MatButtonModule,
    MatTreeModule,
    MatSidenavModule,
    MatProgressBarModule,
    HttpClientModule,
  ],
})
export class MaterialModule {
  constructor(
    iconRegistry: MatIconRegistry,
    donSanitizer: DomSanitizer,
  ) {
    const pathToIconsMap = {
      'icons/menu': [
        'br-logo',
        'audiences',
        'audiences.active',
        'categories',
        'categories.active',
        'default',
        'default.active',
        'documents',
        'documents.active',
        'document-search',
        'document-search.active',
        'experience-manager',
        'experience-manager.active',
        'fast-travel',
        'fast-travel.active',
        'insights',
        'insights.active',
        'projects',
        'projects.active',
        'seo',
        'seo.active',
        'settings',
        'settings.active',
        'site-search',
        'site-search.active',
        'widget',
        'widget.active',
        'extensions',
        'extensions.active',
        'help',
        'user',
      ],
      icons: [
        'nav-collapse',
        'nav-expand',
        'expand_less',
        'expand_more',
        'remove',
        'chevron_right',
        'search',
        'arrow_drop_down',
        'arrow_right',
      ],
    };

    Object.keys(pathToIconsMap).forEach(path => {
      const icons = pathToIconsMap[path];

      icons.forEach(icon => iconRegistry.addSvgIcon(
        icon,
        donSanitizer.bypassSecurityTrustResourceUrl(`navapp-assets/${path}/${icon}.svg`)),
      );
    });
  }
}
