/*!
 * Copyright 2020 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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
import { MatIconModule, MatIconRegistry } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { DomSanitizer } from '@angular/platform-browser';

import { registerSvgIcons } from './register-svg-icons';

@NgModule({
  exports: [
    MatButtonModule,
    MatListModule,
    MatIconModule,
  ],
})
export class MaterialModule {
  constructor(private readonly iconRegistry: MatIconRegistry, private readonly domSanitizer: DomSanitizer) {
    this.registerCustomIcons();
  }

  private registerCustomIcons(): void {
    registerSvgIcons(this.iconRegistry, this.domSanitizer);
  }
}
