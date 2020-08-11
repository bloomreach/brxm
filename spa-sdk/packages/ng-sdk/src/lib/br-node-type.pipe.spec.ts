/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { mocked } from 'ts-jest/utils';
import { isContainer, isContainerItem, Component, Container, ContainerItem } from '@bloomreach/spa-sdk';
import { BrNodeTypePipe } from './br-node-type.pipe';

jest.mock('@bloomreach/spa-sdk');

describe('BrNodeTypePipe', () => {
  beforeEach(() => {
    jest.resetAllMocks();
  });

  describe('transform', () => {
    const pipe = new BrNodeTypePipe();

    it('should return "container-item" for the container item node', () => {
      mocked(isContainerItem).mockReturnValueOnce(true);

      expect(pipe.transform({} as ContainerItem)).toBe('container-item');
    });

    it('should return "container" for the container node', () => {
      mocked(isContainer).mockReturnValueOnce(true);

      expect(pipe.transform({} as Container)).toBe('container');
    });

    it('should return "component" in other cases', () => {
      expect(pipe.transform({} as Component)).toBe('component');
    });
  });
});
