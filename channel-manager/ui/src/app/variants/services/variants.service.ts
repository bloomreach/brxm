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

import { Inject, Injectable } from '@angular/core';

import { ComponentProperties } from '../../models/component-properties.model';
import { Ng1TargetingService, NG1_TARGETING_SERVICE } from '../../services/ng1/targeting.ng1service';
import { GroupedVariant } from '../models/grouped-variant.model';
import { Variant, VariantCharacteristicData } from '../models/variant.model';

const DEFAULT_VARIANT_ID = 'hippo-default';

@Injectable({
  providedIn: 'root',
})
export class VariantsService {
  readonly defaultVariantId = DEFAULT_VARIANT_ID;

  constructor(
    @Inject(NG1_TARGETING_SERVICE) private readonly targetingService: Ng1TargetingService,
  ) {}

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
