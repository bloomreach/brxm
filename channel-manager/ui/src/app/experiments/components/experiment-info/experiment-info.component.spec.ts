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

import { NO_ERRORS_SCHEMA, Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ExperimentState } from '../../models/experiment-state.enum';
import { Experiment } from '../../models/experiment.model';

import { ExperimentInfoComponent } from './experiment-info.component';

@Pipe({name: 'experimentName'})
export class ExperimentNameMockPipe implements PipeTransform {
  transform(value: Experiment): string {
    return value.id.toString();
  }
}

@Pipe({name: 'translate'})
export class TranslateMockPipe implements PipeTransform {
  transform(value: string): string {
    return value;
  }
}

@Pipe({name: 'moment'})
export class MomentMockPipe implements PipeTransform {
  transform(value: number | string | Date, format?: string): string {
    return `${value}`;
  }
}

describe('ExperimentInfoComponent', () => {
  let fixture: ComponentFixture<ExperimentInfoComponent>;
  let component: ExperimentInfoComponent;

  const mockExperiment: Experiment = {
    id: 'experiment-1',
    state: ExperimentState.Unknown,
    type: 'PAGE',
    startTime: 1600868196063,
    winnerVariant: null,
    goal: {
      id: 'goal-1',
      name: 'Goal 1',
      type: 'PAGE',
      readOnly: false,
      targetPage: '/target-page',
      mountId: 'some-mount-id',
    },
    variants: [
      {
        variantId: 'default',
        variantName: 'Default',
        confidence: 0.3,
        mean: 0.2,
        variance: 0.4,
        visitorSegment: 'segment-1',
      },
      {
        variantId: 'some-variant',
        variantName: 'Some variant',
        confidence: 0.3,
        mean: 0.2,
        variance: 0.4,
        visitorSegment: 'segment-1',
      },
    ],
  };

  beforeEach(() => {
    fixture = TestBed.configureTestingModule({
      declarations: [
        ExperimentInfoComponent,
        ExperimentNameMockPipe,
        TranslateMockPipe,
        MomentMockPipe,
      ],
      schemas: [
        NO_ERRORS_SCHEMA,
      ],
    }).createComponent(ExperimentInfoComponent);

    component = fixture.componentInstance;

    component.experiment = mockExperiment;
  });

  describe.each([
    ExperimentState.Created,
    ExperimentState.Running,
    ExperimentState.Completed,
    ExperimentState.Unknown,
  ])('%s experiment', state => {
    beforeEach(() => {
      component.experiment = {
        ...mockExperiment,
        state,
      };

      if (state === ExperimentState.Created) {
        component.experiment.startTime = -1;
      }

      fixture.detectChanges();
    });

    test('should be shown', () => {
      expect(fixture.nativeElement).toMatchSnapshot();
    });
  });
});
