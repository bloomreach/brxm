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
import { Experiment } from '../../models/experiment.model';
import { ExperimentsService } from '../../services/experiments.service';

import { ExperimentsComponent } from './experiments.component';

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

describe('ExperimentsComponent', () => {
  let fixture: ComponentFixture<ExperimentsComponent>;
  let component: ExperimentsComponent;

  const mockExperiment = {
    id: 100,
    state: ExperimentState.Running,
    startTime: 1600868196063,
    goal: {
      name: 'Goal 1',
      targetPage: '/target-page',
    },
    variants: [
      { name: 'Default' },
      { name: 'Some variant' },
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
        ExperimentsComponent,
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
    }).createComponent(ExperimentsComponent);

    component = fixture.componentInstance;

    fixture.detectChanges();
    tick();
    fixture.detectChanges();
  }));

  it('should be shown', () => {
    expect(fixture.nativeElement).toMatchSnapshot();
  });
});
