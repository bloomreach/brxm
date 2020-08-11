/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { ChangeDetectionStrategy, Component, HostBinding, Input } from '@angular/core';
import { Container, Page } from '@bloomreach/spa-sdk';

@Component({
  selector: 'div.br-container-box', // tslint:disable-line: component-selector
  templateUrl: './br-container-box.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BrContainerBoxComponent {
  @Input() component!: Container;
  @Input() page!: Page;

  @HostBinding('class.hst-container')
  get isPreview() {
    return this.page?.isPreview();
  }
}
