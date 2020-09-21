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

import { Component, Inject } from '@angular/core';

import { Ng1ComponentEditorService, NG1_COMPONENT_EDITOR_SERVICE } from '../../../services/ng1/component-editor.ng1.service';
import { Ng1StateService, NG1_STATE_SERVICE } from '../../../services/ng1/state.ng1.service';
import { VariantsService } from '../../services/variants.service';

@Component({
  selector: 'em-variants',
  templateUrl: './variants.component.html',
  styleUrls: ['./variants.component.scss'],
})
export class VariantsComponent {
  private readonly component = this.componentEditorService.getComponent();
  initialSelection = this.ng1StateService.params.variantId;
  variants$ = this.variantsService.getVariants(this.component.getId());

  constructor(
    @Inject(NG1_COMPONENT_EDITOR_SERVICE) private readonly componentEditorService: Ng1ComponentEditorService,
    @Inject(NG1_STATE_SERVICE) private readonly ng1StateService: Ng1StateService,
    private readonly variantsService: VariantsService,
  ) { }

  changeVariant(variantId: string): void {
    this.ng1StateService.go('hippo-cm.channel.edit-component', {
      componentId: this.component.getId(),
      variantId,
    });
  }
}
