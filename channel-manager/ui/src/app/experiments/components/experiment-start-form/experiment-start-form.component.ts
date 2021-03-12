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

import { GroupedVariant } from '../../../variants/models/grouped-variant.model';
import { Variant } from '../../../variants/models/variant.model';
import { VariantsService } from '../../../variants/services/variants.service';
import { ExperimentGoal } from '../../models/experiment-goal.model';

export interface SelectedVariantIdAndGoalId {
  variantId: string;
  goalId: string;
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

  constructor(private readonly variantsService: VariantsService) {}

  get againstDefault(): boolean {
    return (this.selectedVariant?.numberOfVariants || 1) === 1;
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
      this.groupedVariants = this.groupVariants(this.variants);

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
      variantId: this.selectedVariant.id,
      goalId: this.selectedGoalId,
    });
  }

  getOriginalVariant(groupedVariant: GroupedVariant): Variant | undefined {
    return this.variants.find(v => v.id === groupedVariant.id);
  }

  private reset(): void {
    this.selectedVariant = this.defaultGroupedVariant;
    this.selectedGoalId = undefined;
  }

  private groupVariants(variants: Variant[]): GroupedVariant[] {
    const preparedVariants = variants.map(v => ({ id: v.id, name: v.variantName }));

    return this.variantsService.groupVariants(preparedVariants);
  }
}
