/*!
 * Copyright 2020-2021 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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

import { CommonModule } from '@angular/common';
import { Injector, NgModule } from '@angular/core';
import { createCustomElement } from '@angular/elements';

import { SharedModule } from '../shared/shared.module';
import { TranslationsModule } from '../translations/translations.module';

import { LatestVersionComponent } from './components/latest-version/latest-version.component';
import { VersionComponent } from './components/version/version.component';
import { VersionsInfoComponent } from './components/versions-info/versions-info.component';

@NgModule({
  declarations: [
    LatestVersionComponent,
    VersionComponent,
    VersionsInfoComponent,
  ],
  imports: [
    CommonModule,
    SharedModule,
    TranslationsModule,
  ],
  entryComponents: [
    VersionsInfoComponent,
  ],
})
export class VersionsModule {
  constructor(readonly injector: Injector) {
    const versionsInfoElement = createCustomElement(VersionsInfoComponent, { injector });
    customElements.define('em-versions-info', versionsInfoElement);
  }
}
