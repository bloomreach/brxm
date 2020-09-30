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

import { Component, Inject, OnInit } from '@angular/core';

import { Ng1ComponentEditorService, NG1_COMPONENT_EDITOR_SERVICE } from '../../../services/ng1/component-editor.ng1.service';
import { Ng1StateService, NG1_STATE_SERVICE } from '../../../services/ng1/state.ng1.service';
import { Variant, VariantExpressionType, VariantRules } from '../../models/variant.model';
import { VariantsService } from '../../services/variants.service';

@Component({
  selector: 'em-variants',
  templateUrl: './variants.component.html',
  styleUrls: ['./variants.component.scss'],
})
export class VariantsComponent implements OnInit {
  private readonly component = this.componentEditorService.getComponent();
  private readonly componentId = this.component.getId();
  variantIdParam = this.ng1StateService.params.variantId;
  variants?: Variant[];

  constructor(
    @Inject(NG1_COMPONENT_EDITOR_SERVICE) private readonly componentEditorService: Ng1ComponentEditorService,
    @Inject(NG1_STATE_SERVICE) private readonly ng1StateService: Ng1StateService,
    private readonly variantsService: VariantsService,
  ) {
  }

  async ngOnInit(): Promise<void> {
    this.variants = await this.variantsService.getVariants(this.componentId);
  }

  get selectedVariant(): Variant | undefined {
    return this.variants?.find(v => v.id === this.variantIdParam);
  }

  async addVariant(): Promise<void> {
    const formData = this.componentEditorService.propertiesAsFormData();
    const { persona, characteristics } = this.extractRules(this.selectedVariant);

    await this.variantsService.addVariant(this.componentId, formData, persona, characteristics);

    const newVariants = await this.variantsService.getVariants(this.componentId);
    const newVariant = newVariants.find(variant => !this.variants?.find(v => v.id === variant.id));

    this.variants = newVariants;
    this.selectVariant(newVariant?.id || '');
  }

  selectVariant(variantId: string): void {
    this.ng1StateService.go('hippo-cm.channel.edit-component', {
      componentId: this.componentId,
      variantId,
    });
  }

  private extractRules(variant?: Variant): VariantRules {
    let persona = '';
    const characteristics: any[] = [];

    variant?.expressions.map(({ id, type }) => {
      if (type === VariantExpressionType.Persona) {
        persona = id;
      } else {
        const parts = id.split('/');
        const characteristic = parts[0];
        const targetGroupId = parts[1];

        characteristics.push({
          [characteristic]: targetGroupId,
        });
      }
    });

    return {
      persona,
      characteristics,
    };
  }
}
