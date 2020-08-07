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

// tslint:disable-next-line:match-default-export-name
import xpageIcon from '!!raw-loader!./icons/xpage.svg';
// tslint:disable-next-line:match-default-export-name
import mdiIcons from '!!raw-loader!@mdi/angular-material/mdi.svg';
import { NgModule } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule, MatIconRegistry } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { DomSanitizer } from '@angular/platform-browser';

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
    this.iconRegistry.addSvgIconSetLiteral(this.domSanitizer.bypassSecurityTrustHtml(mdiIcons));
    this.iconRegistry.addSvgIconLiteral('xpage', this.domSanitizer.bypassSecurityTrustHtml(xpageIcon));
  }
}
