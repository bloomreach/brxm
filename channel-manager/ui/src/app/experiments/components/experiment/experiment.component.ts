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
import { TranslateService } from '@ngx-translate/core';

import { Ng1ComponentEditorService, NG1_COMPONENT_EDITOR_SERVICE } from '../../../services/ng1/component-editor.ng1.service';
import { NotificationService } from '../../../services/notification.service';
import { VariantsService } from '../../../variants/services/variants.service';
import { ExperimentState } from '../../models/experiment-state.enum';
import { Experiment } from '../../models/experiment.model';
import { ExperimentsService } from '../../services/experiments.service';
import { SelectedVariantIdAndGoalId } from '../experiment-start-form/experiment-start-form.component';

@Component({
  templateUrl: 'experiment.component.html',
  styleUrls: ['experiment.component.scss'],
})
export class ExperimentComponent {
  private readonly component = this.componentEditorService.getComponent();
  private readonly componentId = this.component.getId();
  readonly availableVariants$ = this.variantsService.getVariants(this.componentId);
  readonly availableGoals$ = this.experimentsService.getGoals();

  experiment$ = this.experimentsService.getExperiment(this.componentId);

  requestInProgress = false;

  constructor(
    @Inject(NG1_COMPONENT_EDITOR_SERVICE) private readonly componentEditorService: Ng1ComponentEditorService,
    private readonly experimentsService: ExperimentsService,
    private readonly variantsService: VariantsService,
    private readonly notificationService: NotificationService,
    private readonly translateService: TranslateService,
  ) {}

  isExperimentRunning(experiment: Experiment): boolean {
    return experiment.state === ExperimentState.Running;
  }

  isExperimentRunningOrCompleted(experiment: Experiment): boolean {
    return this.isExperimentRunning(experiment) || experiment.state === ExperimentState.Completed;
  }

  async onVariantAndGoalSelected(value: SelectedVariantIdAndGoalId): Promise<void> {
    const { variantId, goalId } = value;

    try {
      this.requestInProgress = true;

      await this.experimentsService.saveExperiment(this.componentId, variantId, goalId);

      this.experiment$ = this.experimentsService.getExperiment(this.componentId);

      this.notificationService.showNotification(this.translateService.instant('EXPERIMENT_SAVED'));
    } catch (e) {
      this.notificationService.showErrorNotification(this.translateService.instant('EXPERIMENT_SAVE_ERROR'));
    } finally {
      this.requestInProgress = false;
    }
  }

  async onCompleteExperiment(experiment: Experiment): Promise<void> {
    const firstNonDefaultVariant = experiment.variants.find(v => v.variantId !== this.variantsService.defaultVariantId);

    if (!firstNonDefaultVariant) {
      throw new Error('Unable to find a non default variant');
    }

    try {
      this.requestInProgress = true;

      await this.experimentsService.completeExperiment(this.componentId, firstNonDefaultVariant.variantId);

      this.experiment$ = this.experimentsService.getExperiment(this.componentId);

      this.notificationService.showNotification(this.translateService.instant('EXPERIMENT_COMPLETED'));
    } catch {
      this.notificationService.showErrorNotification(this.translateService.instant('EXPERIMENT_COMPLETION_ERROR'));
    } finally {
      this.requestInProgress = false;
    }
  }
}
