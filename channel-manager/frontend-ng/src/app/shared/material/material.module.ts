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
import {
  MATERIAL_SANITY_CHECKS,
  NoConflictStyleCompatibilityMode,
  MdListModule,
  MdButtonModule,
  MdInputModule,
  MdSelectModule,
  MdDialogModule, MdIconRegistry, MdIconModule
} from '@angular/material';
import './material.scss';
import { DomSanitizer } from '@angular/platform-browser';

@NgModule({
  exports: [
    NoConflictStyleCompatibilityMode,
    MdListModule,
    MdButtonModule,
    MdInputModule,
    MdButtonModule,
    MdSelectModule,
    MdDialogModule,
    MdIconModule
  ],
  providers: [
    {provide: MATERIAL_SANITY_CHECKS, useValue: false},
  ]
})
export class MaterialModule {
  constructor (private iconRegistry: MdIconRegistry, sanitizer: DomSanitizer) {
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
      iconRegistry.addSvgIcon(icon, sanitizer.bypassSecurityTrustResourceUrl(iconUrl));
    });
  }
}
