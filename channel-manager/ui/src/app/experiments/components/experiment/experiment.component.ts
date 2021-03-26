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

import { Component, Inject, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

import { Ng1ChannelService, NG1_CHANNEL_SERVICE } from '../../../services/ng1/channel.ng1service';
import { Ng1ComponentEditorService, NG1_COMPONENT_EDITOR_SERVICE } from '../../../services/ng1/component-editor.ng1.service';
import { NotificationService } from '../../../services/notification.service';
import { VariantsService } from '../../../variants/services/variants.service';
import { ExperimentState } from '../../models/experiment-state.enum';
import { ExperimentWithStatusData } from '../../models/experiment-with-status-data.model';
import { Experiment } from '../../models/experiment.model';
import { ExperimentsService } from '../../services/experiments.service';
import { SelectedVariantIdAndGoalId } from '../experiment-start-form/experiment-start-form.component';

@Component({
  templateUrl: 'experiment.component.html',
  styleUrls: ['experiment.component.scss'],
})
export class ExperimentComponent implements OnInit {
  private readonly component = this.componentEditorService.getComponent();
  private readonly componentId = this.component.getId();
  readonly isXPageComponent = this.component.isXPageComponent();
  readonly availableVariants$ = this.variantsService.getVariants(this.componentId);
  readonly availableGoals$ = this.experimentsService
  .getGoals()
  .then(goals => {
    const channel = this.channelService.getChannel();
    return goals.filter(goal => goal.mountId === channel.mountId);
  });

  experiment?: ExperimentWithStatusData;

  requestInProgress = false;

  constructor(
    @Inject(NG1_COMPONENT_EDITOR_SERVICE) private readonly componentEditorService: Ng1ComponentEditorService,
    @Inject(NG1_CHANNEL_SERVICE) private readonly channelService: Ng1ChannelService,
    private readonly experimentsService: ExperimentsService,
    private readonly variantsService: VariantsService,
    private readonly notificationService: NotificationService,
    private readonly translateService: TranslateService,
  ) { }

  async ngOnInit(): Promise<void> {
    this.experiment = await this.experimentsService.getExperiment(this.componentId);
    this.experiment?.variants.forEach(variant => {
      const mean = 100 * variant.mean;
      const sigma = 100 * Math.sqrt(variant.variance);

      const conversion = {
        mean : Math.max(mean, 0).toFixed(1),
        low : Math.max(mean - sigma, 0).toFixed(1),
        high : Math.max(mean + sigma, 0).toFixed(1),
      };

      variant.conversion = conversion;
    });
  }

  isExperimentCreated(experiment: Experiment): boolean {
    return experiment.state === ExperimentState.Created;
  }

  isExperimentRunning(experiment: Experiment): boolean {
    return experiment.state === ExperimentState.Running;
  }

  isExperimentRunningOrCompleted(experiment: Experiment): boolean {
    return this.isExperimentRunning(experiment) || experiment.state === ExperimentState.Completed;
  }

  async onCancelExperiment(): Promise<void> {
    try {
      this.requestInProgress = true;

      await this.experimentsService.saveExperiment(this.componentId);

      this.experiment = await this.experimentsService.getExperiment(this.componentId);

      this.notificationService.showNotification(this.translateService.instant('EXPERIMENT_CANCELED'));
    } catch (e) {
      this.notificationService.showErrorNotification(this.translateService.instant('EXPERIMENT_CANCEL_ERROR'));
    } finally {
      this.requestInProgress = false;
    }
  }

  async onVariantAndGoalSelected(value: SelectedVariantIdAndGoalId): Promise<void> {
    const { variantId, goalId } = value;

    try {
      this.requestInProgress = true;

      await this.experimentsService.saveExperiment(this.componentId, variantId, goalId);

      this.experiment = await this.experimentsService.getExperiment(this.componentId);

      this.notificationService.showNotification(this.translateService.instant('EXPERIMENT_SAVED'));
    } catch (e) {
      this.notificationService.showErrorNotification(this.translateService.instant('EXPERIMENT_SAVE_ERROR'));
    } finally {
      this.requestInProgress = false;
    }
  }

  async onCompleteExperiment(keepOnlyVariantId?: string): Promise<void> {
    try {
      this.requestInProgress = true;

      await this.experimentsService.completeExperiment(this.componentId, keepOnlyVariantId);

      this.experiment = await this.experimentsService.getExperiment(this.componentId);

      this.notificationService.showNotification(this.translateService.instant('EXPERIMENT_COMPLETED'));
    } catch {
      this.notificationService.showErrorNotification(this.translateService.instant('EXPERIMENT_COMPLETION_ERROR'));
    } finally {
      this.requestInProgress = false;
    }
  }
}
