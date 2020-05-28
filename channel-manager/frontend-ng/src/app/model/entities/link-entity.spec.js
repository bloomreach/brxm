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

import { LinkEntity } from './link-entity';

describe('LinkEntity', () => {
  describe('getId', () => {
    it('should return link uuid', () => {
      const link = new LinkEntity({ uuid: 'something' });

      expect(link.getId()).toBe('something');
    });
  });

  describe('getLabel', () => {
    it('should return an empty string', () => {
      const link = new LinkEntity({});

      expect(link.getLabel()).toBe('');
    });
  });

  describe('hasLabel', () => {
    it('should return false', () => {
      const link = new LinkEntity({});

      expect(link.hasLabel()).toBe(false);
    });
  });

  describe('setComponent', () => {
    it('should store linked component', () => {
      const link = new LinkEntity({});
      const component = {};

      link.setComponent(component);

      expect(link.getComponent()).toBe(component);
    });
  });

  describe('isShared', () => {
    it('should return false', () => {
      const link = new LinkEntity({});

      expect(link.isShared()).toBe(false);
    });
  });

  describe('isVisible', () => {
    it('should return true', () => {
      const link = new LinkEntity({});

      expect(link.isVisible()).toBe(true);
    });
  });
});
