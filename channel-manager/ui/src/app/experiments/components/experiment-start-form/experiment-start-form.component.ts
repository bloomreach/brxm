/*!
 * Copyright 2020 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';

import { Variant } from '../../../variants/models/variant.model';
import { ExperimentGoal } from '../../models/experiment-goal.model';

interface GroupedVariant {
  ids: string[];
  name: string;
}

@Component({
  selector: 'em-experiment-start-form',
  templateUrl: 'experiment-start-form.component.html',
  styleUrls: ['experiment-start-form.component.scss'],
})
export class ExperimentStartFormComponent implements OnChanges {
  @Input()
  variants: Variant[] = [];

  @Input()
  goals: ExperimentGoal[] = [];

  groupedVariants: GroupedVariant[] = [];
  selectedVariant?: GroupedVariant;
  selectedGoal: ExperimentGoal | undefined;

  get againstDefault(): boolean {
    return this.selectedVariant ? this.selectedVariant.ids.length === 1 : true;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if ('variants' in changes && this.variants) {
      const variantsWithoutDefault = this.filterOutDefault(this.variants);
      this.groupedVariants = this.groupVariants(variantsWithoutDefault);

      if (!this.selectedVariant && this.groupedVariants.length > 0) {
        this.selectedVariant = this.groupedVariants[0];
      }
    }
  }

  private filterOutDefault(variants: Variant[]): Variant[] {
    return variants.filter(v => !v.id.includes('default'));
  }

  private groupVariants(variants: Variant[]): GroupedVariant[] {
    return variants.map(v => ({ ids: [v.id], name: v.variantName }));
  }
}
