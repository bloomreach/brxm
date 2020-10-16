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

import { LinkFactory } from './link-factory';
import { TYPE_LINK_EXTERNAL, TYPE_LINK_INTERNAL, TYPE_LINK_RESOURCE, TYPE_LINK_UNKNOWN } from './link';

describe('LinkFactory', () => {
  describe('create', () => {
    const builder1 = jest.fn();
    const builder2 = jest.fn();
    const factory = new LinkFactory()
      .register(TYPE_LINK_EXTERNAL, builder1)
      .register(TYPE_LINK_INTERNAL, builder2);

    beforeEach(() => {
      builder1.mockClear();
      builder2.mockClear();
    });

    it('should call a registered builder', () => {
      factory.create({ href: 'link1', type: TYPE_LINK_EXTERNAL });
      factory.create({ href: 'link2', type: TYPE_LINK_INTERNAL });

      expect(builder1).toBeCalledWith('link1');
      expect(builder2).toBeCalledWith('link2');
    });

    it('should return link as-is on the unregistered link type', () => {
      expect(factory.create({ href: 'link', type: TYPE_LINK_RESOURCE })).toBe('link');
    });

    it('should return undefined on the unknown link type', () => {
      expect(factory.create({ type: TYPE_LINK_UNKNOWN })).toBeUndefined();
    });

    it('should return link as-is when there is no type specified', () => {
      expect(factory.create({ href: 'link' })).toBe('link');
      expect(builder1).not.toBeCalled();
      expect(builder2).not.toBeCalled();
    });

    it('should fallback to internal link type for path', () => {
      factory.create('link');

      expect(builder2).toBeCalledWith('link');
    });
  });
});
