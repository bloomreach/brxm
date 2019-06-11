/* Copyright 2017-2019 Hippo B.V. (http://www.onehippo.com) */

import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import {
  MatButtonModule,
  MatIconModule,
  MatIconRegistry,
  MatRippleModule,
  MatTreeModule,
} from '@angular/material';
import { DomSanitizer } from '@angular/platform-browser';

@NgModule({
  exports: [
    MatRippleModule,
    MatIconModule,
    MatTreeModule,
    MatButtonModule,
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
      ],
    };

    Object.keys(pathToIconsMap).forEach(path => {
      const icons = pathToIconsMap[path];

      icons.forEach(icon => iconRegistry.addSvgIcon(
        icon,
        donSanitizer.bypassSecurityTrustResourceUrl(`navapp/assets/${path}/${icon}.svg`)),
      );
    });
  }
}
