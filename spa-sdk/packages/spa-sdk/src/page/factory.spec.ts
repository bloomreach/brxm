/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

import { SingleTypeFactory, MultipleTypeFactory } from './factory';

describe('SingleTypeFactory', () => {
  const builder = jest.fn();
  const factory = new SingleTypeFactory(builder);

  beforeEach(() => {
    builder.mockClear();
  });

  describe('create', () => {
    it('should pass parameters to a builder function', () => {
      const input = {};
      factory.create(input);

      expect(builder).toBeCalledWith(input);
    });

    it('should return a builder function output', () => {
      const output = {};
      builder.mockReturnValueOnce(output);

      expect(factory.create()).toBe(output);
    });
  });
});

describe('MultipleTypeFactory', () => {
  const factory = new class extends MultipleTypeFactory<string, (param: string) => string> {
    create(param: string) {
      this.mapping.forEach(builder => builder(param));
    }
  };

  describe('register', () => {
    it('should provide a fluent interface', () => {
      expect(factory.register('something', jest.fn())).toBe(factory);
    });

    it('should store builder in the mapping', () => {
      const builder = jest.fn();
      factory.register('something', builder);
      factory.create('something');

      expect(builder).toBeCalledWith('something');
    });
  });
});
