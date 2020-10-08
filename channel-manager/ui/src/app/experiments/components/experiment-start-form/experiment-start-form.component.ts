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

import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';

import { Variant } from '../../../variants/models/variant.model';
import { ExperimentGoal } from '../../models/experiment-goal.model';

export interface SelectedVariantIdAndGoalId {
  variantId: string;
  goalId: string;
}

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

  @Input()
  disabled = false;

  @Output()
  selected = new EventEmitter<SelectedVariantIdAndGoalId>();

  groupedVariants: GroupedVariant[] = [];
  selectedVariant?: GroupedVariant;
  selectedGoalId: string | undefined;

  get againstDefault(): boolean {
    return this.selectedVariant ? this.selectedVariant.ids.length === 1 : true;
  }

  get isDiscardButtonDisabled(): boolean {
    return this.selectedVariant === this.defaultGroupedVariant && !this.selectedGoalId;
  }

  get isSaveButtonDisabled(): boolean {
    return !this.selectedVariant || !this.selectedGoalId;
  }

  private get defaultGroupedVariant(): GroupedVariant | undefined {
    return this.groupedVariants.length > 0 ? this.groupedVariants[0] : undefined;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if ('variants' in changes && this.variants) {
      const variantsWithoutDefault = this.filterOutDefault(this.variants);
      this.groupedVariants = this.groupVariants(variantsWithoutDefault);

      this.selectedVariant = this.selectedVariant || this.defaultGroupedVariant;
    }
  }

  onDiscard(): void {
    this.reset();
  }

  onSave(): void {
    if (!this.selectedVariant || !this.selectedGoalId) {
      return;
    }

    this.selected.emit({
      variantId: this.selectedVariant.ids[0],
      goalId: this.selectedGoalId,
    });
  }

  private reset(): void {
    this.selectedVariant = this.defaultGroupedVariant;
    this.selectedGoalId = undefined;
  }

  private filterOutDefault(variants: Variant[]): Variant[] {
    return variants.filter(v => !v.id.includes('default'));
  }

  private groupVariants(variants: Variant[]): GroupedVariant[] {
    return variants.map(v => ({ ids: [v.id], name: v.variantName }));
  }
}
