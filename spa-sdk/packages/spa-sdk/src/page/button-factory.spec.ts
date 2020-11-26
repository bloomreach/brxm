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
import { ButtonFactory } from './button-factory';
import { MetaCollectionFactory } from './meta-collection-factory';
import { MetaCollection, isMetaCollection } from './meta-collection';

jest.mock('./meta-collection');

let metaCollectionFactory: jest.MockedFunction<MetaCollectionFactory>;

beforeEach(() => {
  metaCollectionFactory = jest.fn();
});

function createButtonFactory() {
  return new ButtonFactory(metaCollectionFactory);
}

describe('ButtonFactory', () => {
  let factory: ButtonFactory;
  let builder1: jest.MockedFunction<Parameters<ButtonFactory['register']>[1]>;
  let builder2: jest.MockedFunction<Parameters<ButtonFactory['register']>[1]>;

  beforeEach(() => {
    builder1 = jest.fn(() => ({} as MetaCollection));
    builder2 = jest.fn(() => ({} as MetaCollection));
    factory = createButtonFactory()
      .register('type1', builder1)
      .register('type2', builder2);
  });

  describe('create', () => {
    it('should call a registered builder', () => {
      factory.create('type1', { id: 'id1' });
      factory.create('type2', { id: 'id2' });

      expect(builder1).toBeCalledWith({ id: 'id1' });
      expect(builder2).toBeCalledWith({ id: 'id2' });
    });

    it('should throw an error on unknown button type', () => {
      expect(() => factory.create('type3')).toThrowError();
    });

    it('should create a meta-collection instance', () => {
      const button = {} as MetaCollection;

      mocked(isMetaCollection).mockReturnValueOnce(false);
      metaCollectionFactory.mockReturnValueOnce(button);

      factory.create('type1', { id: 'id1' });

      expect(metaCollectionFactory).toBeCalledWith(button);
    });

    it('should create a meta-collection instance', () => {
      const button = {} as MetaCollection;

      mocked(isMetaCollection).mockReturnValueOnce(true);
      builder1.mockReturnValueOnce(button);

      expect(factory.create('type1', { id: 'id1' })).toBe(button);
    });
  });
});
