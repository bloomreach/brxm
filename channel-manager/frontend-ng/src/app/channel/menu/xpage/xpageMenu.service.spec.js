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

import angular from 'angular';
import 'angular-mocks';

describe('XPageMenuService', () => {
  let $translate;
  let PageService;
  let XPageMenuService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      _$translate_,
      _PageService_,
      _XPageMenuService_,
    ) => {
      $translate = _$translate_;
      PageService = _PageService_;
      XPageMenuService = _XPageMenuService_;
    });
  });

  function getItem(name) {
    return XPageMenuService.menu.items.find(item => item.name === name);
  }

  function addAction(name, enabled = true) {
    if (!PageService.actions) {
      PageService.actions = {
        xpage: {
          items: {},
        },
      };
    }
    PageService.actions.xpage.items[name] = {
      enabled,
    };
  }

  beforeEach(() => {
    spyOn($translate, 'instant');

    PageService.actions = null;
  });

  describe('xpage menu', () => {
    it('should hide the menu button', () => {
      expect(XPageMenuService.menu.isVisible()).toBe(false);
    });

    it('should show the menu button', () => {
      PageService.actions = {
        xpage: {},
      };

      expect(XPageMenuService.menu.isVisible()).toBe(true);
    });
  });

  describe('new', () => {
    it('should hide the "new" action', () => {
      expect(getItem('new').isVisible()).toBe(false);
    });

    it('should show the "new" action', () => {
      addAction('new');

      expect(getItem('new').isVisible()).toBe(true);
      expect(getItem('new').isEnabled()).toBe(true);
    });

    it('should show a disabled "new" action', () => {
      addAction('new', false);

      expect(getItem('new').isVisible()).toBe(true);
      expect(getItem('new').isEnabled()).toBe(false);
    });
  });

  describe('move', () => {
    it('should hide the "move" action', () => {
      expect(getItem('move').isVisible()).toBe(false);
    });

    it('should show the "move" action', () => {
      addAction('move');

      expect(getItem('move').isVisible()).toBe(true);
      expect(getItem('move').isEnabled()).toBe(true);
    });

    it('should show a disabled "move" action', () => {
      addAction('move', false);

      expect(getItem('move').isVisible()).toBe(true);
      expect(getItem('move').isEnabled()).toBe(false);
    });
  });

  describe('delete', () => {
    it('should hide the "delete" action', () => {
      expect(getItem('delete').isVisible()).toBe(false);
    });

    it('should show the "delete" action', () => {
      addAction('delete');

      expect(getItem('delete').isVisible()).toBe(true);
      expect(getItem('delete').isEnabled()).toBe(true);
    });

    it('should show a disabled "delete" action', () => {
      addAction('delete', false);

      expect(getItem('delete').isVisible()).toBe(true);
      expect(getItem('delete').isEnabled()).toBe(false);
    });
  });
});
