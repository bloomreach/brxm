/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

import { Component } from './component';

describe('Component', () => {
  describe('hasExperiment', () => {
    it('should return true when the component has an experiment', () => {
      const component = new Component({
        'Targeting-experiment-id': 'something',
      });

      expect(component.hasExperiment()).toBe(true);
    });

    it('should return false when the component has no experiment', () => {
      const component = new Component({});

      expect(component.hasExperiment()).toBe(false);
    });
  });

  describe('getExperimentId', () => {
    it('should return an experiment id', () => {
      const component = new Component({
        'Targeting-experiment-id': 'something',
      });

      expect(component.getExperimentId()).toBe('something');
    });
  });

  describe('getExperimentStateLabel', () => {
    it('should return null when the component has no experiment', () => {
      const component = new Component({});

      expect(component.getExperimentStateLabel()).toBeNull();
    });

    it('should return an experiment state label', () => {
      const component = new Component({
        'Targeting-experiment-id': 'something',
        'Targeting-experiment-state': 'COMPLETED',
      });

      expect(component.getExperimentStateLabel()).toBe('EXPERIMENT_LABEL_COMPLETED');
    });
  });

  describe('setContainer', () => {
    it('should set a container', () => {
      const component = new Component({});
      component.setContainer('something');

      expect(component.getContainer()).toBe('something');
    });
  });

  describe('getType', () => {
    it('should return "component"', () => {
      const component = new Component({});

      expect(component.getType()).toBe('component');
    });
  });

  describe('getRenderVariant', () => {
    it('should return component render variant', () => {
      const component = new Component({ 'HST-Render-Variant': 'something' });

      expect(component.getRenderVariant()).toBe('something');
    });

    it('should return a default render variant', () => {
      const component = new Component({});

      expect(component.getRenderVariant()).toBe('hippo-default');
    });
  });

  describe('getReferenceNamespace', () => {
    it('should return component reference namespace', () => {
      const component = new Component({ refNS: 'something' });

      expect(component.getReferenceNamespace()).toBe('something');
    });
  });
});
