/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

describe('NameUrlFields', () => {
  let $componentController;
  let $q;
  let $rootScope;
  let $timeout;
  let CreateContentService;

  let component;
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

    $rootScope.$new();
    element = angular.element('<form><input ng-model="$ctrl.nameField" name="name" placeholder="Document name" required autocomplete="off" id="nameInputElement"></form>');
    component = $componentController('nameUrlFields', {
      $element: element,
    });

    spies.generateDocumentUrlByName = spyOn(CreateContentService, 'generateDocumentUrlByName').and.returnValue($q.resolve());
    spies.setDocumentUrlByName = spyOn(component, 'setDocumentUrlByName').and.callThrough();

    component.locale = 'en';
    component.$onInit();
  });

  afterEach(() => {
    component.nameField = '';
  });

  function setNameInputValue(value) {
    component.nameInputField.val(value);
    component.nameField = value;
    component.onNameChange();
  }

  describe('$onInit', () => {
    it('calls setDocumentUrlByName directly after first keyup is triggered on nameInputElement', () => {
      setNameInputValue('test val');
      expect(component.setDocumentUrlByName).toHaveBeenCalled();
    });

    it('calls setDocumentUrlByName 400ms after second keyup is triggered on nameInputElement', () => {
      setNameInputValue('test');
      setNameInputValue(' val');
      expect(component.setDocumentUrlByName.calls.count()).toBe(1);
      $timeout.flush();
      expect(component.setDocumentUrlByName.calls.count()).toBe(2);
    });

    it('waits until server callback resolves before submitting a new documentUrlByName request with the latest value', () => {
      const deferredRequest = $q.defer();
      spies.generateDocumentUrlByName.and.returnValue(deferredRequest.promise);
      setNameInputValue('1');
      setNameInputValue('12');
      setNameInputValue('123');
      setNameInputValue('1234');
      expect(component.setDocumentUrlByName.calls.count()).toBe(1);

      deferredRequest.resolve();
      $timeout.flush();
      expect(component.setDocumentUrlByName.calls.count()).toBe(2);
      expect(spies.generateDocumentUrlByName).toHaveBeenCalledWith('1', 'en');
      expect(spies.generateDocumentUrlByName).not.toHaveBeenCalledWith('12', 'en');
      expect(spies.generateDocumentUrlByName).not.toHaveBeenCalledWith('123', 'en');
      expect(spies.generateDocumentUrlByName).toHaveBeenCalledWith('1234', 'en');
    });

    it('sets the url with locale automatically after locale has been changed', () => {
      setNameInputValue('some val');

      expect(component.setDocumentUrlByName).toHaveBeenCalled();
      expect(spies.generateDocumentUrlByName).toHaveBeenCalledWith('some val', 'en');

      component.locale = 'de';
      const changes = {
        locale: {
          currentValue: component.locale,
          previousValue: 'en',
          isFirstChange: () => false,
        },
      };
      component.$onChanges(changes);
      expect(component.setDocumentUrlByName).toHaveBeenCalled();
      expect(spies.generateDocumentUrlByName).toHaveBeenCalledWith('some val', 'de');
    });
  });

  describe('setDocumentUrlByName', () => {
    it('applies the url', () => {
      spies.generateDocumentUrlByName.and.returnValue($q.resolve('test'));

      component.$onInit();
      setNameInputValue('test');
      $rootScope.$apply();
      component.setDocumentUrlByName();

      expect(spies.generateDocumentUrlByName).toHaveBeenCalledWith('test', 'en');

      $rootScope.$apply();
      expect(component.urlField).toEqual('test');
    });

    it('Manual editing of the URL', () => {
      component.$onInit();
      component.setManualUrlEditMode(true);
      component.urlField = 'manual-edit-of-url';

      spies.generateDocumentUrlByName.calls.reset();

      // Until manual editing mode is disabled, URL generations should be bypassed
      setNameInputValue('Second edit, should not change the URL');
      $rootScope.$apply();
      expect(spies.generateDocumentUrlByName).not.toHaveBeenCalled();
      expect(component.urlField).toEqual('manual-edit-of-url');
    });
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
