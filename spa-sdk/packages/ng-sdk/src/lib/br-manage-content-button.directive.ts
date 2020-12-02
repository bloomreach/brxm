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

import { Directive, Input, OnChanges, Optional, SimpleChanges, TemplateRef, ViewContainerRef } from '@angular/core';
import { ManageContentButton, TYPE_MANAGE_CONTENT_BUTTON } from '@bloomreach/spa-sdk';
import { BrMetaDirective } from './br-meta.directive';
import { BrPageComponent } from './br-page/br-page.component';

/**
 * The button direcrtive that opens for editing a content.
 */
@Directive({ selector: '[brManageContentButton]' })
export class BrManageContentButtonDirective extends BrMetaDirective implements OnChanges, ManageContentButton {
  @Input('brManageContentButton') content?: ManageContentButton['content'];

  @Input() documentTemplateQuery?: ManageContentButton['documentTemplateQuery'];

  @Input() folderTemplateQuery?: ManageContentButton['folderTemplateQuery'];

  @Input() path?: ManageContentButton['path'];

  @Input() parameter?: ManageContentButton['parameter'];

  @Input() relative?: ManageContentButton['relative'];

  @Input() root?: ManageContentButton['root'];

  constructor(
    container: ViewContainerRef,
    @Optional() template?: TemplateRef<never>,
    @Optional() private page?: BrPageComponent,
  ) {
    super(container, template);
  }

  ngOnChanges(changes: SimpleChanges): void {
    this.meta = this.page?.state.getValue()?.getButton(TYPE_MANAGE_CONTENT_BUTTON, this);

    super.ngOnChanges(changes);
  }
}
