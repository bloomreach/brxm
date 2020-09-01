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
import { Container } from './container';

describe('Container', () => {
  describe('getType', () => {
    it('should return "container"', () => {
      const container = new Container({});

      expect(container.getType()).toBe('container');
    });
  });

  describe('isEmpty', () => {
    it('should return true when the container is empty', () => {
      const container = new Container({});

      expect(container.isEmpty()).toBe(true);
    });

    it('should return false when the container is not empty', () => {
      const component = new Component({});
      const container = new Container({});
      container.addComponent(component);

      expect(container.isEmpty()).toBe(false);
    });
  });

  describe('isInherited', () => {
    it('should return true when the container is inherited', () => {
      const container = new Container({ 'HST-Inherited': 'true' });

      expect(container.isInherited()).toBe(true);
    });

    it('should return false when the container is not inherited', () => {
      const container = new Container({});

      expect(container.isInherited()).toBe(false);
    });
  });

  describe('isDisabled', () => {
    let container;

    beforeEach(() => {
      container = new Container();
      spyOn(container, 'isInherited');
      spyOn(container, 'isLocked');
      spyOn(container, 'isLockedByCurrentUser');
    });

    it('should return true when the container is inherited', () => {
      container.isInherited.and.returnValue(true);

      expect(container.isDisabled()).toBe(true);
    });

    it('should return true when the container is locked', () => {
      container.isInherited.and.returnValue(false);
      container.isLocked.and.returnValue(true);
      container.isLockedByCurrentUser.and.returnValue(false);

      expect(container.isDisabled()).toBe(true);
    });

    it('should return false when the container is not locked', () => {
      container.isInherited.and.returnValue(false);
      container.isLocked.and.returnValue(false);

      expect(container.isDisabled()).toBe(false);
    });

    it('should return false when the container is locked by the current user', () => {
      container.isInherited.and.returnValue(false);
      container.isLocked.and.returnValue(true);
      container.isLockedByCurrentUser.and.returnValue(true);

      expect(container.isDisabled()).toBe(false);
    });
  });

  describe('isXTypeNoMarkup', () => {
    it('should return true when this is no markup container', () => {
      const container = new Container({ 'HST-XType': 'HST.NoMarkup' });

      expect(container.isXTypeNoMarkup()).toBe(true);
    });

    it('should return false when there is no xtype defined', () => {
      const container = new Container({});

      expect(container.isXTypeNoMarkup()).toBe(false);
    });

    it('should return false when this is container with a markup', () => {
      const container = new Container({ 'HST-XType': 'HST.Span' });

      expect(container.isXTypeNoMarkup()).toBe(false);
    });
  });

  describe('getDragDirection', () => {
    it('should return horizontal for the span container', () => {
      const container = new Container({ 'HST-XType': 'HST.Span' });

      expect(container.getDragDirection()).toBe('horizontal');
    });

    it('should return vertical for not the span container', () => {
      const container = new Container({ 'HST-XType': 'HST.NoMarkup' });

      expect(container.getDragDirection()).toBe('vertical');
    });
  });

  describe('addComponent', () => {
    const container = new Container({});
    const component = jasmine.createSpyObj('component', ['setContainer']);
    container.addComponent(component);

    it('should set a container for the component', () => {
      expect(component.setContainer).toHaveBeenCalledWith(container);
    });

    it('should store added component', () => {
      expect(container.getComponents()).toContain(component);
    });
  });

  describe('addComponentBefore', () => {
    let component1;
    let component2;
    let container;

    beforeEach(() => {
      component1 = jasmine.createSpyObj('component', ['setContainer']);
      component2 = jasmine.createSpyObj('component', ['setContainer']);
      container = new Container({});
    });

    it('should set a container for the component', () => {
      container.addComponentBefore(component1);

      expect(component1.setContainer).toHaveBeenCalledWith(container);
    });

    it('should add a component to the end when the next component is not defined', () => {
      container.addComponentBefore(component2);
      container.addComponentBefore(component1);

      expect(container.getComponents()).toEqual([component2, component1]);
    });

    it('should add a component to the end when the next component is not a part of the container', () => {
      const component3 = jasmine.createSpyObj('component', ['setContainer']);

      container.addComponentBefore(component2, component3);
      container.addComponentBefore(component1, component3);

      expect(container.getComponents()).toEqual([component2, component1]);
    });

    it('should add a component before the next component', () => {
      container.addComponentBefore(component2);
      container.addComponentBefore(component1, component2);

      expect(container.getComponents()).toEqual([component1, component2]);
    });
  });

  describe('removeComponent', () => {
    let component;
    let container;

    beforeEach(() => {
      component = jasmine.createSpyObj('component', ['setContainer']);
      container = new Container({});
      container.addComponent(component);
    });

    it('should do nothing if the component is not present', () => {
      const anotherComponent = jasmine.createSpyObj('component', ['setContainer']);

      expect(() => container.removeComponent(anotherComponent)).not.toThrow();
      expect(anotherComponent.setContainer).not.toHaveBeenCalled();
      expect(container.getComponents()).toEqual([component]);
    });

    it('should remove the component', () => {
      container.removeComponent(component);

      expect(component.setContainer).toHaveBeenCalledWith(null);
      expect(container.getComponents()).toEqual([]);
    });
  });

  describe('getComponents', () => {
    let component;
    let container;

    beforeEach(() => {
      component = jasmine.createSpyObj('component', ['setContainer']);
      container = new Container({});
      container.addComponent(component);
    });

    it('should return previously added components', () => {
      expect(container.getComponents()).toEqual([component]);
    });
  });

  describe('getComponent', () => {
    const component = jasmine.createSpyObj('component', ['getId', 'setContainer']);
    const container = new Container({});

    component.getId.and.returnValue('id1');
    container.addComponent(component);

    it('should find the component by id', () => {
      expect(container.getComponent('id1')).toBe(component);
    });

    it('should return undefined when there is a component', () => {
      expect(container.getComponent('id2')).toBeUndefined();
    });
  });

  describe('replaceComponent', () => {
    let component1;
    let component2;
    let container;

    beforeEach(() => {
      component1 = jasmine.createSpyObj('component', ['setContainer']);
      component2 = jasmine.createSpyObj('component', ['setContainer']);
      container = new Container({});
      container.addComponent(component1);
    });

    it('should not replace a component if it was not previously added', () => {
      const component3 = jasmine.createSpyObj('component', ['setContainer']);

      expect(() => container.replaceComponent(component2, component3)).not.toThrow();
      expect(container.getComponents()).toEqual([component1]);
    });

    it('should replace a component with the new one', () => {
      expect(() => container.replaceComponent(component1, component2)).not.toThrow();
      expect(container.getComponents()).toEqual([component2]);
    });
  });

  describe('hasComponent', () => {
    const component = jasmine.createSpyObj('component', ['setContainer']);
    const container = new Container({});

    container.addComponent(component);

    it('should return true if it contains the component', () => {
      expect(container.hasComponent(component)).toBe(true);
    });

    it('should return false if it does not contain the component', () => {
      const anotherComponent = jasmine.createSpyObj('component', ['setContainer']);

      expect(container.hasComponent(anotherComponent)).toBe(false);
    });
  });

  describe('getHstRepresentation', () => {
    const component = jasmine.createSpyObj('component', ['getId', 'setContainer']);
    const container = new Container({ uuid: 'container-id', 'HST-LastModified': 12345 });

    component.getId.and.returnValue('component-id');
    container.addComponent(component);

    it('should contain container id', () => {
      expect(container.getHstRepresentation()).toEqual(jasmine.objectContaining({ id: 'container-id' }));
    });

    it('should contain container last modified timestamp', () => {
      expect(container.getHstRepresentation()).toEqual(jasmine.objectContaining({ lastModifiedTimestamp: 12345 }));
    });

    it('should contain components ids', () => {
      expect(container.getHstRepresentation()).toEqual(jasmine.objectContaining({ children: ['component-id'] }));
    });
  });

  describe('getDropGroups', () => {
    it('should return "xpages" if the component is an Experience Page component', () => {
      const container = new Container({ 'HST-Experience-Page-Component': 'true' });

      expect(container.getDropGroups()).toContain('xpages');
      expect(container.getDropGroups()).not.toContain('default');
    });

    it('should return "default" if the component is not an Experience Page component', () => {
      const container = new Container({});

      expect(container.getDropGroups()).toContain('default');
      expect(container.getDropGroups()).not.toContain('xpages');
    });

    it('should append the suffix "-shared" to group names from a shared container', () => {
      const container1 = new Container({ 'HST-Shared': 'true' });
      const container2 = new Container({ 'HST-Experience-Page-Component': 'true', 'HST-Shared': 'true' });

      expect(container1.getDropGroups()).toEqual(['default-shared']);
      expect(container2.getDropGroups()).toEqual(['xpages-shared']);
    });
  });

  describe('getXPageEditable', () => {
    it('should return true', () => {
      const container = new Container({ 'HST-XPage-Editable': 'true' });

      expect(container.isXPageEditable()).toBe(true);
    });

    it('should return false', () => {
      const container = new Container({ 'HST-XPage-Editable': 'false' });

      expect(container.isXPageEditable()).toBe(false);
    });
  });
});
