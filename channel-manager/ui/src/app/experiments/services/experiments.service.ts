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
import { ExperimentStatusWithVisits } from '../models/experiment-status-with-visits.model';
import { ExperimentStatusAtTimestamp } from '../models/experiment-status.model';
import { ExperimentWithStatusData } from '../models/experiment-with-status-data.model';
import { ExperimentGoal } from '../models/experiment-goal.model';

@Injectable({
  providedIn: 'root',
})
export class ExperimentsService {
  constructor(@Inject(NG1_TARGETING_SERVICE) private readonly ng1TargetingService: Ng1TargetingService) {}

  async getExperiment(componentId: string): Promise<ExperimentWithStatusData | undefined> {
    const response = await this.ng1TargetingService.getExperiment(componentId);

    if (!response.success || !response.data) {
      return;
    }

    const experiment = response.data;

    const experimentStatus = await this.getExperimentStatus(experiment.id);

    if (!experimentStatus) {
      return experiment;
    }

    return {
      ...experiment,
      statusWithVisits: experimentStatus.statusWithVisits,
      totalVisits: experimentStatus.totalVisits,
    };
  }

  async getExperimentStatus(experimentId: string): Promise<ExperimentStatusWithVisits  | undefined> {
    const response = await this.ng1TargetingService.getExperimentStatus(experimentId);

    if (!response.success || !response.data) {
      return;
    }

    const status = response.data;

    let totalVisits = 0;
    const statusWithVisits = status.map(pointAtTimestamp => {
      const visits = this.calculateVisitsPerTimestamp(pointAtTimestamp);

      totalVisits += visits;

      return {
        ...pointAtTimestamp,
        visits,
      };
    });

    return {
      statusWithVisits,
      totalVisits,
    };
  }

  async getGoals(): Promise<ExperimentGoal[]> {
    return Promise.resolve([
      {
        id: 'goal-3',
        name: 'Goal 3',
        type: 'PAGE',
        readOnly: false,
        targetPage: '/faq',
        mountId: '64fc383e-0dc0-40c2-a657-115cde69cd67',
      },
      {
        id: 'goal-2',
        name: 'Goal 2',
        type: 'PAGE',
        readOnly: true,
        targetPage: '/events',
        mountId: '1a1a1a1a-e880-4629-9e63-bc8ed8399d2a',
      },
      {
        id: 'goal-1',
        name: 'Goal 1',
        type: 'PAGE',
        readOnly: true,
        targetPage: '/news',
        mountId: '1a1a1a1a-e880-4629-9e63-bc8ed8399d2a',
      },
    ]);
  }

  calculateVisitsPerTimestamp(point: ExperimentStatusAtTimestamp): number {
    return Object.keys(point).reduce((sum, key) => {
      return key !== 'timestamp' ? sum + point[key] : sum;
    }, 0);
  }
}
