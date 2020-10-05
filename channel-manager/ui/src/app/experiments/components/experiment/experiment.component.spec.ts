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
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';

import { NG1_COMPONENT_EDITOR_SERVICE } from '../../../services/ng1/component-editor.ng1.service';
import { ExperimentState } from '../../models/experiment-state.enum';
import { ExperimentWithStatusData } from '../../models/experiment-with-status-data.model';
import { Experiment } from '../../models/experiment.model';
import { ExperimentsService } from '../../services/experiments.service';

import { ExperimentComponent } from './experiment.component';

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

describe('ExperimentComponent', () => {
  let fixture: ComponentFixture<ExperimentComponent>;
  let component: ExperimentComponent;

  const mockExperiment: ExperimentWithStatusData = {
    id: 'experiment-1',
    state: ExperimentState.Running,
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
    statusWithVisits: [
      { 'variant-1': 3, default: 1, timestamp: 1600948800000, visits: 4 },
      { 'variant-1': 100, default: 10, timestamp: 1600959600000, visits: 110 },
      { 'variant-1': 40, default: 1000, timestamp: 1600970400000, visits: 1040 },
      { 'variant-1': 20, default: 5, timestamp: 1600981200000, visits: 25 },
      { 'variant-1': 80, default: 80, timestamp: 1600992000000, visits: 160 },
      { 'variant-1': 11, default: 12, timestamp: 1601002800000, visits: 23 },
      { 'variant-1': 0, default: 0, timestamp: 1601013600000, visits: 0 },
      { 'variant-1': 3, default: 4, timestamp: 1601024400000, visits: 7 },
      { 'variant-1': 1000, default: 500, timestamp: 1601035200000, visits: 1500 },
      { 'variant-1': 123, default: 456, timestamp: 1601046000000, visits: 579 },
      { 'variant-1': 234, default: 546, timestamp: 1601056800000, visits: 779 },
      { 'variant-1': 4635, default: 4365, timestamp: 1601067600000, visits: 9000 },
      { 'variant-1': 345, default: 465, timestamp: 1601078400000, visits: 810 },
      { 'variant-1': 465, default: 467, timestamp: 1601089200000, visits: 922 },
    ],
  };

  const mockComponent = {
    getId: () => 'mockComponentId',
  };

  beforeEach(fakeAsync(() => {
    const componentEditorServiceMock = {
      getComponent: () => mockComponent,
    };

    const experimentsServiceMock = {
      getExperiment: jest.fn(() => Promise.resolve(mockExperiment)),
    };

    fixture = TestBed.configureTestingModule({
      declarations: [
        ExperimentComponent,
        ExperimentNameMockPipe,
        TranslateMockPipe,
        MomentMockPipe,
      ],
      providers: [
        { provide: NG1_COMPONENT_EDITOR_SERVICE, useValue: componentEditorServiceMock },
        { provide: ExperimentsService, useValue: experimentsServiceMock },
      ],
      schemas: [
        NO_ERRORS_SCHEMA,
      ],
    }).createComponent(ExperimentComponent);

    component = fixture.componentInstance;

    fixture.detectChanges();
    tick();
    fixture.detectChanges();
  }));

  it('should be shown', () => {
    expect(fixture.nativeElement).toMatchSnapshot();
  });
});
