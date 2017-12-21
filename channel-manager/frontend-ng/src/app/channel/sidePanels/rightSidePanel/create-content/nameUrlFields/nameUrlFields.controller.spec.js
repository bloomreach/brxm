/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

import { fakeAsync, tick } from '@angular/core/testing';

describe('NameUrlFields', () => {
  let $componentController;
  let $q;
  let $rootScope;
  let $timeout;
  let CreateContentService;

  let component;
  let scope;
  let element;
  const spies = {};

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.createContentModule');

    inject((_$componentController_, _$q_, _$rootScope_, _$timeout_, _CreateContentService_) => {
      $componentController = _$componentController_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      $timeout = _$timeout_;
      CreateContentService = _CreateContentService_;
    });

    scope = $rootScope.$new();
    // element = angular.element('<hippo-name-url-fields name-field="nameField" url-field="urlField" locale="locale"></hippo-name-url-fields>');
    element = angular.element('<form><input ng-model="$ctrl.nameField" name="name" placeholder="Document name" required autocomplete="off" id="nameInputElement"></form>');
    component = $componentController('nameUrlFields', {
      $element: element,
    });

    spies.generateDocumentUrlByName = spyOn(CreateContentService, 'generateDocumentUrlByName').and.returnValue($q.resolve());
    spies.setDocumentUrlByName = spyOn(component, 'setDocumentUrlByName').and.callThrough();

    component.locale = 'en';
    component.urlUpdate = () => angular.noop();
    component.$onInit();
  });

  afterEach(() => {
    component.nameField = '';
  });

  function setNameInputValue (value) {
    component.nameInputField.val(value);
    component.nameField = value;
    component.nameInputField.trigger('keyup');
  }

  describe('$onInit', () => {
    it('calls setDocumentUrlByName 1 second after keyup was triggered on nameInputElement', fakeAsync(() => {
      setNameInputValue('test val');
      tick(1000);

      expect(component.setDocumentUrlByName).toHaveBeenCalled();
    }));

    it('sets the url with locale automatically after locale has been changed', fakeAsync(() => {
      setNameInputValue('some val');
      tick(1000);

      expect(component.setDocumentUrlByName).toHaveBeenCalled();
      expect(spies.generateDocumentUrlByName).toHaveBeenCalledWith('some val', 'en');

      component.locale = 'de';
      const changes = {
        locale: {
          currentValue: component.locale,
          previousValue: 'en',
          isFirstChange: () => false,
        }
      };
      component.$onChanges(changes);
      expect(component.setDocumentUrlByName).toHaveBeenCalled();
      expect(spies.generateDocumentUrlByName).toHaveBeenCalledWith('some val', 'de');
    }));
  });

  describe('setDocumentUrlByName', () => {
    it('applies the url', fakeAsync(() => {
      spies.generateDocumentUrlByName.and.returnValue($q.resolve('test'));

      component.$onInit();
      setNameInputValue('test');
      tick(1000);
      component.setDocumentUrlByName();

      expect(spies.generateDocumentUrlByName).toHaveBeenCalledWith('test', 'en');

      $rootScope.$apply();
      expect(component.urlField).toEqual('test');
    }));

    it('Manual editing of the URL', fakeAsync(() => {
      component.$onInit();
      component.setManualUrlEditMode(true);
      component.urlField = 'manual-edit-of-url';

      spies.generateDocumentUrlByName.calls.reset();

      // Until manual editing mode is disabled, URL generations should be bypassed
      setNameInputValue('Second edit, should not change the URL');
      tick(1000);
      expect(spies.generateDocumentUrlByName).not.toHaveBeenCalled();
      expect(component.urlField).toEqual('manual-edit-of-url');
    }));
  });

  describe('validateFields', () => {
    describe('conditions scenarios', () => {
      it('returns true, all conditions resolved to "true"', () => {
        component.nameField = 'name';
        component.urlField = 'url';
        expect(component.validateFields()).toEqual(true);
      });

      it('returns false, name field is empty (conditions index 0)', () => {
        component.nameField = '';
        component.urlField = 'url';
        expect(component.validateFields()).toEqual(false);
      });

      it('returns false, url field is empty (conditions index 1)', () => {
        component.nameField = 'name';
        component.urlField = '';
        expect(component.validateFields()).toEqual(false);
      });

      it('returns false, name field is only whitespace(s) (conditions index 2)', () => {
        component.nameField = '     ';
        component.urlField = 'url';
        expect(component.validateFields()).toEqual(false);
      });

      it('returns false, url field is only whitespace(s) (conditions index 3)', () => {
        component.nameField = 'name';
        component.urlField = '     ';
        expect(component.validateFields()).toEqual(false);
      });
    });
  });
});
