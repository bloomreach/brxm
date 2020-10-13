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

import { TestBed } from '@angular/core/testing';

import { NG1_TARGETING_SERVICE } from '../../services/ng1/targeting.ng1service';
import { Variant } from '../models/variant.model';

import { VariantsService } from './variants.service';

describe('VariantsService', () => {
  let service: VariantsService;

  beforeEach(() => {
    const targetingServiceMock = {
      getVariants: jest.fn(),
      addVariant: jest.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        VariantsService,
        { provide: NG1_TARGETING_SERVICE, useValue: targetingServiceMock },
      ],
    });

    service = TestBed.inject(VariantsService);
  });

  describe.each([
    [
      'one variant against default',
      [
        { id: 'default', name: 'Default' },
        { id: 'variant-1', name: 'Variant 1' },
      ] as Variant[],
      [
        { id: 'default', name: 'Default', numberOfVariants: 1 },
        { id: 'variant-1', name: 'Variant 1', numberOfVariants: 1 },
      ],
    ],
    [
      'multiple defaults and one custom variant',
      [
        { id: 'default-a', name: 'Default-A' },
        { id: 'default-b', name: 'Default-B' },
        { id: 'default-c', name: 'Default-C' },
        { id: 'variant-1', name: 'Variant 1' },
      ] as Variant[],
      [
        { id: 'default-a', name: 'Default (3 variants)', numberOfVariants: 3 },
        { id: 'variant-1', name: 'Variant 1', numberOfVariants: 1 },
      ],
    ],
    [
      'multiple defaults and multiple custom variants',
      [
        { id: 'default-a', name: 'Default-A' },
        { id: 'default-b', name: 'Default-B' },
        { id: 'default-c', name: 'Default-C' },
        { id: 'variant-a', name: 'Variant-A' },
        { id: 'variant-b', name: 'Variant-B' },
        { id: 'variant-c', name: 'Variant-C' },
        { id: 'default', name: 'Some variant' },
      ] as Variant[],
      [
        { id: 'default-a', name: 'Default (3 variants)', numberOfVariants: 3 },
        { id: 'variant-a', name: 'Variant (3 variants)', numberOfVariants: 3 },
        { id: 'default', name: 'Some variant', numberOfVariants: 1 },
      ],
    ],
  ])('groupVariants', (description, variants, groupedVariants) => {
    test(`should group ${description}`, () => {
      const result = service.groupVariants(variants);

      expect(result).toEqual(groupedVariants);
    });
  });
});
