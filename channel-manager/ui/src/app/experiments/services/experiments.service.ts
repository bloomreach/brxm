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
import { ExperimentWithStatusData } from '../models/experiment-with-status-data.model';

@Injectable({
  providedIn: 'root',
})
export class ExperimentsService {
  constructor(@Inject(NG1_TARGETING_SERVICE) private readonly ng1TargetingService: Ng1TargetingService) {}

  async getExperiment(componentId: string): Promise<ExperimentWithStatusData | undefined> {
    const response = await this.ng1TargetingService.getExperiment(componentId);

    if (!response.success) {
      return;
    }

    const experiment = response.data;

    const statusResponse = await this.ng1TargetingService.getExperimentStatus(experiment.id);

    if (!statusResponse.success) {
      return experiment;
    }

    const status = statusResponse.data;

    return {
      ...experiment,
      status,
    };
  }
}
