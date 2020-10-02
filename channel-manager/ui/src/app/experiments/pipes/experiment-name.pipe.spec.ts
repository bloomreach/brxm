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

import { Experiment } from '../models/experiment.model';

import { ExperimentNamePipe } from './experiment-name.pipe';

describe('ExperimentNamePipe', () => {
  let pipe: ExperimentNamePipe;

  beforeEach(() => {
    pipe = new ExperimentNamePipe();
  });

  it('should return an empty string if the provided experiment is undefined', () => {
    const result = pipe.transform(undefined);

    expect(result).toBe('');
  });

  it('should return an empty string if the provided experiment does not contain variants', () => {
    const experiment = {
      variants: undefined,
    } as unknown as Experiment;

    const result = pipe.transform(experiment);

    expect(result).toBe('');
  });

  it('should return an empty string if the provided experiment contains zero variants', () => {
    const experiment = {
      variants: [],
    } as unknown as Experiment;

    const result = pipe.transform(experiment);

    expect(result).toBe('');
  });

  it('should group the same segment and characteristic variant names', () => {
    const experiment = {
      variants: [
        { variantName: 'Some variant name-A' },
        { variantName: 'Some variant name-B' },
        { variantName: 'Some variant name-C' },
      ],
    } as Experiment;

    const result = pipe.transform(experiment);

    expect(result).toBe('Some variant name (3 variants)');
  });

  it('should return the variant name compared against the default variant', () => {
    const experiment = {
      variants: [
        { variantName: 'Default' },
        { variantName: 'Some variant name' },
      ],
    } as Experiment;

    const result = pipe.transform(experiment);

    expect(result).toBe('Some variant name');
  });
});
