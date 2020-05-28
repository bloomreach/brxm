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

import { ComponentEntity } from './component-entity';

describe('ComponentEntity', () => {
  describe('getId', () => {
    it('should return component uuid', () => {
      const component = new ComponentEntity({ uuid: 'aaaa' });

      expect(component.getId()).toBe('aaaa');
    });
  });

  describe('getLabel', () => {
    it('should return component uuid', () => {
      const component = new ComponentEntity({ 'HST-Label': 'component A' });

      expect(component.getLabel()).toBe('component A');
    });

    it('should fallback to the component type', () => {
      const component = new ComponentEntity({ 'HST-Label': 'null', 'HST-Type': 'CONTAINER_ITEM_COMPONENT' });
      component.getType = jasmine.createSpy();
      component.getType.and.returnValue('component');

      expect(component.getLabel()).toBe('component');
      expect(component.getType).toHaveBeenCalled();
    });
  });

  describe('getLastModified', () => {
    it('should return a timestamp', () => {
      const component = new ComponentEntity({ 'HST-LastModified': '12345' });

      expect(component.getLastModified()).toBe(12345);
    });

    it('should return 0 when there is no value', () => {
      const component = new ComponentEntity({});

      expect(component.getLastModified()).toBe(0);
    });
  });

  describe('getLockedBy', () => {
    it('should return who it is locked by', () => {
      const component = new ComponentEntity({ 'HST-LockedBy': 'admin' });

      expect(component.getLockedBy()).toBe('admin');
    });
  });

  describe('getRenderUrl', () => {
    it('should return component render url', () => {
      const component = new ComponentEntity({ url: 'http://example.com' });

      expect(component.getRenderUrl()).toBe('http://example.com');
    });
  });

  describe('hasLabel', () => {
    it('should return true', () => {
      const component = new ComponentEntity({});

      expect(component.hasLabel()).toBe(true);
    });
  });

  describe('hasNoIFrameDomElement', () => {
    it('should return true', () => {
      const component = new ComponentEntity({ hasNoDom: true });

      expect(component.hasNoIFrameDomElement()).toBe(true);
    });

    it('should return false', () => {
      const component = new ComponentEntity({ hasNoDom: false });

      expect(component.hasNoIFrameDomElement()).toBe(false);
    });
  });

  describe('isEditable', () => {
    it('should return true', () => {
      const component = new ComponentEntity({ 'HST-Component-Editable': 'true' });

      expect(component.isEditable()).toBe(true);
    });

    it('should return false', () => {
      const component = new ComponentEntity({});

      expect(component.isEditable()).toBe(false);
    });
  });

  describe('isLocked', () => {
    it('should return true', () => {
      const component = new ComponentEntity({ 'HST-LockedBy': 'admin' });

      expect(component.isLocked()).toBe(true);
    });

    it('should return false', () => {
      const component = new ComponentEntity({});

      expect(component.isLocked()).toBe(false);
    });
  });

  describe('isLockedByCurrentUser', () => {
    it('should return true', () => {
      const component = new ComponentEntity({ 'HST-LockedBy-Current-User': 'true' });

      expect(component.isLockedByCurrentUser()).toBe(true);
    });

    it('should return false', () => {
      const component = new ComponentEntity({});

      expect(component.isLockedByCurrentUser()).toBe(false);
    });
  });

  describe('isShared', () => {
    it('should return true', () => {
      const component = new ComponentEntity({ 'HST-Shared': 'true' });

      expect(component.isShared()).toBe(true);
    });

    it('should return false', () => {
      const component = new ComponentEntity({});

      expect(component.isShared()).toBe(false);
    });
  });

  describe('isVisible', () => {
    it('should return true', () => {
      const component = new ComponentEntity({ 'HST-Component-Editable': 'true' });

      expect(component.isVisible()).toBe(true);
    });

    it('should return false', () => {
      const component = new ComponentEntity({});

      expect(component.isVisible()).toBe(false);
    });
  });

  describe('isXPageComponent', () => {
    it('should return true', () => {
      const component = new ComponentEntity({ 'HST-Experience-Page-Component': 'true' });

      expect(component.isXPageComponent()).toBe(true);
    });

    it('should return false', () => {
      const component = new ComponentEntity({});

      expect(component.isXPageComponent()).toBe(false);
    });
  });

  describe('addHeadContributions', () => {
    it('should add head contributions', () => {
      const component = new ComponentEntity({});
      component.addHeadContributions('something');

      expect(component.getHeadContributions()).toEqual(['something']);
    });
  });

  describe('addLinks', () => {
    it('should add links', () => {
      const component = new ComponentEntity({});
      component.addLink('something');

      expect(component.getLinks()).toEqual(['something']);
    });
  });
});
