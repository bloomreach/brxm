/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

describe('ExperimentStateService', () => {
  'use strict';

  let ExperimentStateService;
  let $translate;

  beforeEach(() => {
    module('hippo-cm');

    inject((_ExperimentStateService_, _$translate_) => {
      ExperimentStateService = _ExperimentStateService_;
      $translate = _$translate_;
    });
  });

  it('returns whether a component has an experiment', () => {
    expect(ExperimentStateService.hasExperiment({
      metaData: {},
    })).toBe(false);
    expect(ExperimentStateService.hasExperiment({
      metaData: {
        'Targeting-experiment-id': '1234',
      },
    })).toBe(true);
  });

  it('returns the experiment ID for a component with an experiment', () => {
    expect(ExperimentStateService.getExperimentId({
      metaData: {
        'Targeting-experiment-id': '1234',
      },
    })).toBe('1234');
  });

  it('returns an undefined experiment ID for a component without an experiment', () => {
    expect(ExperimentStateService.getExperimentId({
      metaData: {},
    })).not.toBeDefined();
  });

  it('returns the experiment state label for a component with an experiment', () => {
    spyOn($translate, 'instant').and.returnValue('Label');
    expect(ExperimentStateService.getExperimentStateLabel({
      type: 'component',
      metaData: {
        'Targeting-experiment-id': '1234',
        'Targeting-experiment-state': 'running',
      },
    })).toBe('Label');
    expect($translate.instant).toHaveBeenCalledWith('EXPERIMENT_LABEL_running');
  });

  it('returns a null label for a component without an experiment', () => {
    expect(ExperimentStateService.getExperimentStateLabel({
      type: 'component',
      metaData: {},
    })).toBeNull();
  });
});
