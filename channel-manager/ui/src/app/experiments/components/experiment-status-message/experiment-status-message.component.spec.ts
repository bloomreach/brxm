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

import { Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ExperimentVariant } from '../../models/experiment-variant.model';

import { ExperimentStatusMessageComponent } from './experiment-status-message.component';

@Pipe({name: 'translate'})
export class TranslateMockPipe implements PipeTransform {
  transform(value: string, params: { [key: string]: string | number }): string {
    return value + JSON.stringify(params);
  }
}

describe('ExperimentStatusMessageComponent', () => {
  let fixture: ComponentFixture<ExperimentStatusMessageComponent>;
  let component: ExperimentStatusMessageComponent;

  const mockWinnerVariant: ExperimentVariant = {
    variantId: 'variant-1',
    variantName: 'Variant1',
    confidence: 0.7,
    mean: 0.7,
    variance: 0.3,
    visitorSegment: 'segment1',
  };

  beforeEach(() => {
    fixture = TestBed.configureTestingModule({
      declarations: [
        ExperimentStatusMessageComponent,
        TranslateMockPipe,
      ],
    }).createComponent(ExperimentStatusMessageComponent);

    component = fixture.componentInstance;

    component.visits = 100;

    fixture.detectChanges();
  });

  describe('if the winner variant is not present', () => {
    it('should be shown', () => {
      expect(fixture.nativeElement).toMatchSnapshot();
    });
  });

  describe('if the winner variant is present', () => {
    beforeEach(() => {
      component.winnerVariant = mockWinnerVariant;

      fixture.detectChanges();
    });

    it('should be shown', () => {
      expect(fixture.nativeElement).toMatchSnapshot();
    });
  });
});
