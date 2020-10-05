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

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ExperimentState } from '../../models/experiment-state.enum';
import { ExperimentWithStatusData } from '../../models/experiment-with-status-data.model';

import { ExperimentStatusChartComponent } from './experiment-status-chart.component';

// jsdom does not support SVG, the following test requires SVG functions mocking

describe('ExperimentStatusChartComponent', () => {
  // let fixture: ComponentFixture<ExperimentStatusChartComponent>;
  // let component: ExperimentStatusChartComponent;

  const mockExperiment: ExperimentWithStatusData = {
    id: 'experiment-1',
    state: ExperimentState.Running,
    startTime: 1600868196063,
    type: 'PAGE',
    winnerVariant: null,
    variants: [
      {
        variantName: 'Default',
        confidence: 0.4597540245975402,
        variantId: 'default',
        mean: 0.005696835789030305,
        variance: 0.000027686334493024227,
        visitorSegment: 'Default',
      },
      {
        variantName: 'Variant',
        confidence: 0.5402459754024598,
        variantId: 'variant-1',
        mean: 0.006271339704953255,
        variance: 0.000027705078610634725,
        visitorSegment: 'Variant1',
      },
    ],
    goal: {
      id: 'goal-1',
      name: 'Goal 1',
      type: 'PAGE',
      readOnly: true,
      targetPage: '/target-page',
      mountId: '1a1a1a1a-e880-4629-9e63-bc8ed8399d2a',
    },
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

  it('should contain some tests', () => {});

  /*
  beforeEach(() => {
    fixture = TestBed.configureTestingModule({
      declarations: [
        ExperimentStatusChartComponent,
      ],
    }).createComponent(ExperimentStatusChartComponent);

    component = fixture.componentInstance;

    component.experiment = mockExperiment;

    component.ngOnInit();
  });

  it('should be shown', () => {
    expect(fixture.nativeElement).toMatchSnapshot();
  });
  */
});
