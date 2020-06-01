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

import * as HstConstants from './constants';
import {
  Component, Container, HeadContributions, LinkEntity, PageMeta,
} from './entities';

describe('ModelFactoryService', () => {
  let $log;
  let ModelFactoryService;

  beforeEach(() => {
    angular.mock.module('hippo-cm-model');

    inject((
      _$log_,
      _ModelFactoryService_,
    ) => {
      $log = _$log_;
      ModelFactoryService = _ModelFactoryService_;
    });
  });

  describe('createPage', () => {
    it('should return undefined on empty meta', () => {
      expect(ModelFactoryService.createPage()).toBeUndefined();
      expect(ModelFactoryService.createPage([])).toBeUndefined();
    });

    it('should merge meta', () => {
      const meta = new PageMeta();
      spyOn(meta, 'addMeta').and.returnValue(meta);

      ModelFactoryService.register(HstConstants.TYPE_PAGE_META, () => meta);
      const page = ModelFactoryService.createPage([
        { 'HST-Type': 'PAGE-META-DATA', name: 'Page-1' },
        { 'HST-Type': 'PAGE-META-DATA', name: 'Page-2' },
      ]);

      expect(page.getMeta()).toBe(meta);
      expect(meta.addMeta).toHaveBeenCalledTimes(1);
      expect(meta.addMeta).toHaveBeenCalledWith(meta);
    });

    it('should create empty page meta object if there is no page meta-data', () => {
      const page = ModelFactoryService.createPage([{ 'HST-Type': 'EDIT_MENU_LINK' }]);

      expect(page.getMeta().toJSON()).toEqual({});
    });

    it('should build children', () => {
      const page = ModelFactoryService.createPage([
        { 'HST-Type': 'CONTAINER_COMPONENT' },
        { 'HST-End': 'true' },
        { 'HST-Type': 'CONTAINER_COMPONENT' },
        { 'HST-End': 'true' },
      ]);

      expect(page.getContainers()).toEqual([jasmine.any(Container), jasmine.any(Container)]);
    });

    it('should build links', () => {
      const page = ModelFactoryService.createPage([
        { 'HST-Type': 'MANAGE_CONTENT_LINK' },
        { 'HST-Type': 'EDIT_MENU_LINK' },
      ]);

      expect(page.getLinks()).toEqual([jasmine.any(LinkEntity), jasmine.any(LinkEntity)]);
    });

    it('should log a warning on unknown HST type', () => {
      spyOn($log, 'warn');
      ModelFactoryService.createPage([
        { 'HST-Type': 'SOMETHING' },
      ]);

      expect($log.warn).toHaveBeenCalledWith("Ignoring unknown page structure element 'SOMETHING'.");
    });
  });

  describe('createComponent', () => {
    it('should return a component instance', () => {
      const component = ModelFactoryService.createComponent([
        { 'HST-Type': 'CONTAINER_ITEM_COMPONENT' },
        { 'HST-End': 'true' },
        { 'HST-Type': 'CONTAINER_ITEM_COMPONENT' },
        { 'HST-End': 'true' },
      ]);

      expect(component instanceof Component).toBe(true);
    });

    it('should throw an error when there was no component created', () => {
      expect(() => ModelFactoryService.createComponent([
        { 'HST-Type': 'CONTAINER_COMPONENT' },
        { 'HST-End': 'true' },
      ])).toThrowError();
    });

    it('should create component links', () => {
      const component = ModelFactoryService.createComponent([
        { 'HST-Type': 'CONTAINER_ITEM_COMPONENT' },
        { 'HST-Type': 'MANAGE_CONTENT_LINK' },
        { 'HST-End': 'true' },
      ]);

      expect(component.getLinks()).toEqual([jasmine.any(LinkEntity)]);
    });

    it('should create component head contributions', () => {
      const component = ModelFactoryService.createComponent([
        { 'HST-Type': 'CONTAINER_ITEM_COMPONENT' },
        { 'HST-Type': 'HST_PROCESSED_HEAD_CONTRIBUTIONS' },
        { 'HST-End': 'true' },
      ]);

      expect(component.getHeadContributions()).toEqual([jasmine.any(HeadContributions)]);
    });
  });

  describe('createContainer', () => {
    it('should return a container instance', () => {
      const container = ModelFactoryService.createContainer([
        { 'HST-Type': 'CONTAINER_COMPONENT' },
        { 'HST-End': 'true' },
        { 'HST-Type': 'CONTAINER_COMPONENT' },
        { 'HST-End': 'true' },
      ]);

      expect(container instanceof Container).toBe(true);
    });

    it('should throw an error when there was no container created', () => {
      expect(() => ModelFactoryService.createContainer([
        { 'HST-Type': 'CONTAINER_ITEM_COMPONENT' },
        { 'HST-End': 'true' },
      ])).toThrowError();
    });

    it('should create a nested component', () => {
      const container = ModelFactoryService.createContainer([
        { 'HST-Type': 'CONTAINER_COMPONENT' },
        { 'HST-Type': 'CONTAINER_ITEM_COMPONENT' },
        { 'HST-End': 'true' },
        { 'HST-End': 'true' },
      ]);

      expect(container.getComponents()).toEqual([jasmine.any(Component)]);
    });
  });

  describe('transform', () => {
    it('should implement fluent interface', () => {
      expect(ModelFactoryService.transform(() => {})).toBe(ModelFactoryService);
    });

    it('should set a function transforming meta-data item', () => {
      const comment = { 'HST-Type': 'PAGE-META-DATA', name: 'Page-1' };
      const transform = jasmine.createSpy();

      transform.and.callFake(model => model);
      ModelFactoryService.transform(transform);
      ModelFactoryService.createPage([comment]);

      expect(transform).toHaveBeenCalledWith(comment);
    });
  });
});
