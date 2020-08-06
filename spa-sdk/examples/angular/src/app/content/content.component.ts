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

import { Component, Input } from '@angular/core';
import { Component as BrComponent, Page } from '@bloomreach/spa-sdk';

@Component({
  selector: 'br-content',
  templateUrl: './content.component.html',
})
export class ContentComponent {
  @Input() component!: BrComponent;
  @Input() page!: Page;

  get document() {
    const { document } = this.component.getModels<DocumentModels>();

    return document && this.page.getContent(document);
  }

  get data() {
    return this.document?.getData<DocumentData>();
  }

  get date(){
    return this.data?.date ?? this.data?.publicationDate;
  }

  get image() {
    return this.data?.image && this.page.getContent(this.data.image);
  }
}
