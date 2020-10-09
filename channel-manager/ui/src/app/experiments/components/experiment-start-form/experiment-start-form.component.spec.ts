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

import { NO_ERRORS_SCHEMA, Pipe, PipeTransform, SimpleChange } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { first } from 'rxjs/operators';

import { Variant } from '../../../variants/models/variant.model';
import { ExperimentGoal } from '../../models/experiment-goal.model';

import { ExperimentStartFormComponent, SelectedVariantIdAndGoalId } from './experiment-start-form.component';

@Pipe({name: 'translate'})
export class TranslateMockPipe implements PipeTransform {
  transform(value: string): string {
    return value;
  }
}

describe('ExperimentInfoBarComponent', () => {
  let fixture: ComponentFixture<ExperimentStartFormComponent>;
  let component: ExperimentStartFormComponent;

  const mockVariants = [
    {
      id: 'variant-1',
      variantName: 'Variant 1',
    },
    {
      id: 'variant-2',
      variantName: 'Variant 2',
    },
  ] as Variant[];
  const mockGoals: ExperimentGoal[] = [
    {
      id: 'goal-1',
      name: 'Goal 1',
      type: 'PAGE',
      readOnly: false,
      targetPage: '/target-page',
      mountId: 'some-mount-id',
    },
    {
      id: 'goal-2',
      name: 'Goal 2',
      type: 'PAGE',
      readOnly: false,
      targetPage: '/target-page',
      mountId: 'some-mount-id',
    },
  ];

  beforeEach(async () => {
    fixture = TestBed.configureTestingModule({
      imports: [
        FormsModule,
        MatFormFieldModule,
        MatSelectModule,
        MatButtonModule,
        MatDividerModule,
        NoopAnimationsModule,
      ],
      declarations: [
        ExperimentStartFormComponent,
        TranslateMockPipe,
      ],
      schemas: [
        NO_ERRORS_SCHEMA,
      ],
    }).createComponent(ExperimentStartFormComponent);

    component = fixture.componentInstance;

    component.variants = mockVariants;
    component.goals = mockGoals;

    component.ngOnChanges({ variants: new SimpleChange(undefined, component.variants, true) });

    fixture.detectChanges();
  });

  it('should be shown', () => {
    expect(fixture.nativeElement).toMatchSnapshot();
  });

  it('should be shown in the enabled mode', () => {
    component.disabled = true;

    fixture.detectChanges();

    expect(fixture.nativeElement).toMatchSnapshot();
  });

  it('should be shown in the disabled mode', () => {
    component.disabled = true;

    fixture.detectChanges();

    expect(fixture.nativeElement).toMatchSnapshot();
  });

  it('should have the first variant preselected', async () => {
    expect(component.selectedVariant).toBe(component.groupedVariants[0]);
  });

  describe('if a goal is selected', () => {
    beforeEach(() => {
      component.selectedGoalId = 'goal-1';

      fixture.detectChanges();
    });

    it('should be shown', () => {
      expect(fixture.nativeElement).toMatchSnapshot();
    });

    it('should emit the output', () => {
      let result!: SelectedVariantIdAndGoalId;

      component.selected.pipe(first()).subscribe(x => result = x);

      component.onSave();

      expect(result).toEqual({
        variantId: 'variant-1',
        goalId: 'goal-1',
      });
    });
  });
});
