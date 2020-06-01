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

import { Page } from './page';

describe('Page', () => {
  let containers;
  let links;
  let meta;
  let page;

  beforeEach(() => {
    containers = [
      jasmine.createSpyObj('Container', ['getComponent', 'getId']),
      jasmine.createSpyObj('Container', ['getComponent', 'getId']),
    ];
    links = [];
    meta = {};

    page = new Page(meta, containers, links);
  });

  describe('getMeta', () => {
    it('should return meta-data object', () => {
      expect(page.getMeta()).toBe(meta);
    });
  });

  describe('getContainers', () => {
    it('should return containers', () => {
      expect(page.getContainers()).toBe(containers);
    });
  });

  describe('getLinks', () => {
    it('should return links', () => {
      expect(page.getLinks()).toBe(links);
    });
  });

  describe('getComponentById', () => {
    it('should return a component', () => {
      const component = {};
      const [container1, container2] = containers;

      container2.getComponent.and.returnValue(component);

      expect(page.getComponentById('id1')).toBe(component);
      expect(container1.getComponent).toHaveBeenCalledWith('id1');
      expect(container2.getComponent).toHaveBeenCalledWith('id1');
    });

    it('should return null when there is no component', () => {
      expect(page.getComponentById('id1')).toBeNull();
    });
  });

  describe('getContainerById', () => {
    it('should return a container', () => {
      const [container] = containers;

      container.getId.and.returnValue('id1');

      expect(page.getContainerById('id1')).toBe(container);
    });

    it('should return undefined when there is no container', () => {
      expect(page.getContainerById('id1')).toBeUndefined();
    });
  });

  describe('hasContainer', () => {
    it('should return true when the container is present', () => {
      const [container] = containers;

      expect(page.hasContainer(container)).toBe(true);
    });

    it('should return false when the container is not a part of the page', () => {
      const container = {};

      expect(page.hasContainer(container)).toBe(false);
    });
  });

  describe('replaceContainer', () => {
    it('should throw an error when there is no container', () => {
      expect(() => page.replaceContainer({})).toThrowError('Cannot find container.');
    });

    it('should remove the container when the new container is not specified', () => {
      const [container] = containers;

      expect(page.replaceContainer(container)).toBeNull();
      expect(page.getContainers()).not.toContain(container);
    });

    it('should replace the container with the new one', () => {
      const [container] = containers;
      const newContainer = {};

      expect(page.replaceContainer(container, newContainer)).toBe(newContainer);
      expect(page.getContainers()).not.toContain(container);
      expect(page.getContainers()).toContain(newContainer);
    });
  });
});
