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

import { CommonModule } from '@angular/common';
import { Injector, NgModule } from '@angular/core';
import { createCustomElement } from '@angular/elements';

import { SharedModule } from '../shared/shared.module';
import { TranslationsModule } from '../translations/translations.module';

import { VariantsComponent } from './components/variants/variants.component';

@NgModule({
  declarations: [
    VariantsComponent,
  ],
  imports: [
    CommonModule,
    SharedModule,
    TranslationsModule,
  ],
  entryComponents: [
    VariantsComponent,
  ],
})
export class VariantsModule {
  constructor(readonly injector: Injector) {
    const variantsElement = createCustomElement(VariantsComponent, { injector });
    customElements.define('em-variants', variantsElement);
  }
}
