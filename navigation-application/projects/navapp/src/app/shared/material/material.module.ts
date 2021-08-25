/*
 * Copyright 2019-2020 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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
import { MatButtonModule } from '@angular/material/button';
import { MatRippleModule } from '@angular/material/core';
import { MatIconModule, MatIconRegistry } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTreeModule } from '@angular/material/tree';
import { DomSanitizer } from '@angular/platform-browser';

import { registerIcons as registerCommonIcons } from './common-icons';
import { registerIcons as registerMenuIcons } from './menu-icons';

@NgModule({
  exports: [
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatRippleModule,
    MatSidenavModule,
    MatSnackBarModule,
    MatTreeModule,
  ],
})
export class MaterialModule {
  constructor(
    private readonly iconRegistry: MatIconRegistry,
    private readonly donSanitizer: DomSanitizer,
  ) {
    this.registerSvgIcons();
  }

  private registerSvgIcons(): void {
    registerCommonIcons(this.iconRegistry, this.donSanitizer);
    registerMenuIcons(this.iconRegistry, this.donSanitizer);
  }
}
