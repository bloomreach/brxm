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

import { Ng1TargetingService, NG1_TARGETING_SERVICE } from '../../services/ng1/targeting.ng1service';
import { GroupedVariant } from '../models/grouped-variant.model';
import { Variant } from '../models/variant.model';

@Injectable({
  providedIn: 'root',
})
export class VariantsService {
  readonly defaultVariantId = 'hippo-default';

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

  async addVariant(componentId: string, formData: any, persona?: any, characteristics?: any[]): Promise<any> {
    const response = await this.targetingService.addVariant(componentId, formData, persona, characteristics);
    return response.data;
  }

  groupVariants(variants: Variant[]): GroupedVariant[] {
    const suffixedVariantRegEx = /(.*)-[A-Z]$/;
    const groupedVariants = new Map<string, GroupedVariant>();

    const addGroupedVariant = (id: string, name: string) => {
      groupedVariants.set(name, { id, name, numberOfVariants: 1 });
    };

    for (const variant of variants) {
      const match = variant.variantName.match(suffixedVariantRegEx);

      if (!match) {
        addGroupedVariant(variant.id, variant.variantName);

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

    return [...groupedVariants.values()];
  }
}
