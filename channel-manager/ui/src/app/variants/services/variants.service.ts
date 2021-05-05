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

import { Inject, Injectable } from '@angular/core';

import { ComponentProperties } from '../../models/component-properties.model';
import { Ng1TargetingService, NG1_TARGETING_SERVICE } from '../../services/ng1/targeting.ng1.service';
import { GroupedVariant } from '../models/grouped-variant.model';
import { Variant, VariantCharacteristicData, VariantExpressions, VariantExpressionType } from '../models/variant.model';

const DEFAULT_VARIANT_ID = 'hippo-default';

@Injectable({
  providedIn: 'root',
})
export class VariantsService {
  readonly defaultVariantId = DEFAULT_VARIANT_ID;
  private expressionsVisible = true;

  constructor(
    @Inject(NG1_TARGETING_SERVICE) private readonly targetingService: Ng1TargetingService,
  ) {}

  getExpressionsVisible(): boolean {
    return this.expressionsVisible;
  }

  setExpressionsVisible(value: boolean): void {
    this.expressionsVisible = value;
  }

  async getVariants(componentId: string): Promise<Variant[]> {
    const response = await this.targetingService.getVariants(componentId);

    if (!response.success) {
      return [];
    }

    return response.data;
  }

  async addVariant(
    componentId: string,
    formData: ComponentProperties,
    personaId?: string,
    characteristics?: VariantCharacteristicData[],
  ): Promise<any> {
    const response = await this.targetingService.addVariant(componentId, formData, personaId, characteristics);
    return response.data;
  }

  async deleteVariant(componentId: string, variantId: string): Promise<any> {
    const response = await this.targetingService.deleteVariant(componentId, variantId);
    return response.data;
  }

  extractExpressions(variant?: Variant): VariantExpressions {
    let persona = '';
    const characteristics: any[] = [];

    variant?.expressions.forEach(({ id, type }) => {
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

  groupVariants(variants: { id: string, name: string }[]): GroupedVariant[] {
    const suffixedVariantRegEx = /(.*)-[A-Z]$/;
    const groupedVariants = new Map<string, GroupedVariant>();

    const addGroupedVariant = (id: string, name: string) => {
      groupedVariants.set(name, { id, name, numberOfVariants: 1 });
    };

    for (const variant of variants) {
      const match = variant.name.match(suffixedVariantRegEx);

      if (!match) {
        addGroupedVariant(variant.id, variant.name);

        continue;
      }

      const [_, groupedName] = match;
      const groupedVariant = groupedVariants.get(groupedName);

      if (!groupedVariant) {
        addGroupedVariant(variant.id, groupedName);
      } else {
        groupedVariant.numberOfVariants++;
      }
    }

    return [...groupedVariants.values()]
      .filter(v => v.id !== DEFAULT_VARIANT_ID || v.numberOfVariants !== 1)
      .map(v => ({
        id: v.id,
        name: v.numberOfVariants > 1 ? `${v.name} (${v.numberOfVariants} variants)` : v.name,
        numberOfVariants: v.numberOfVariants,
      }));
  }
}
