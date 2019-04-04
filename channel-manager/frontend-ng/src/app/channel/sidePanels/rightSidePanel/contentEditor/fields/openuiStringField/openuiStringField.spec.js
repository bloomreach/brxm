/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

describe('OpenuiStringField', () => {
  let $componentController;
  let $ctrl;
  let $element;
  let $q;
  let $rootScope;
  let mdInputContainer;
  let ngModel;
  let ContentEditor;
  let DialogService;
  let OpenUiService;
  let connection;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_, _$q_, _$rootScope_, _ContentEditor_, _DialogService_, _OpenUiService_) => {
      $componentController = _$componentController_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      ContentEditor = _ContentEditor_;
      DialogService = _DialogService_;
      OpenUiService = _OpenUiService_;
    });

    mdInputContainer = jasmine.createSpyObj('mdInputContainer', ['setHasValue']);
    ngModel = jasmine.createSpyObj('ngModel', ['$setViewValue']);
    $element = angular.element('<div/>');
    $ctrl = $componentController('openuiStringField', { $element }, {
      mdInputContainer,
      ngModel,
      value: 'test-value',
    });
    spyOn(ContentEditor, 'getDocument');
    spyOn(DialogService, 'show');

    connection = {
      iframe: angular.element('<iframe src="about:blank"></iframe>')[0],
      destroy: jasmine.createSpy('destroy'),
    };

    spyOn(OpenUiService, 'initialize').and.returnValue(connection);
  });

  describe('$onInit', () => {
    it('initializes the component', () => {
      $ctrl.$onInit();
      expect(mdInputContainer.setHasValue).toHaveBeenCalledWith(true);
    });

    it('ignores a missing inputContainer', () => {
      $ctrl = $componentController('openuiStringField', { $element }, {
        ngModel,
        value: 'test-value',
      });
      $ctrl.$onInit();
      expect(mdInputContainer.setHasValue).not.toHaveBeenCalled();
    });
  });

  describe('$onChanges', () => {
    it('does nothing without an extensionId', () => {
      expect(() => {
        $ctrl.$onChanges({});
      }).not.toThrow();
    });

    it('connects to the child', () => {
      $ctrl.$onChanges({ extensionId: { currentValue: 'test-id' } });

      expect(OpenUiService.initialize).toHaveBeenCalledWith('test-id', jasmine.objectContaining({
        appendTo: $element[0],
      }));
    });

    it('destroys a previous connection', () => {
      $ctrl.$onChanges({ extensionId: { currentValue: 'test-id1' } });
      $ctrl.$onChanges({ extensionId: { currentValue: 'test-id2' } });
      expect(connection.destroy).toHaveBeenCalled();
    });
  });

  describe('$onDestroy', () => {
    it('destroys the connection with the child', () => {
      $ctrl.$onChanges({ extensionId: { currentValue: 'test-id' } });
      $ctrl.$onDestroy();
      expect(connection.destroy).toHaveBeenCalled();
    });
  });

  it('gets value', () => {
    expect($ctrl.getValue()).toBe('test-value');
  });

  it('sets value', () => {
    $ctrl.setValue('new-value');

    expect($ctrl.getValue()).toBe('new-value');
    expect(ngModel.$setViewValue).toHaveBeenCalledWith('new-value');
  });

  it('fails to set a long value', () => {
    expect(() => $ctrl.setValue('a'.repeat(4097))).toThrow();
  });

  it('sets the height', () => {
    $ctrl.$onChanges({ extensionId: { currentValue: 'test-id' } });
    $ctrl.setHeight(42);
    expect(connection.iframe).toHaveCss({
      height: '42px',
    });
  });

  describe('getDocument', () => {
    beforeEach(() => {
      const document = {
        id: 'handle-id',
        displayName: 'test name',
        info: { locale: 'sv' },
        urlName: 'test-url-name',
        variantId: 'variant-id',
      };
      ContentEditor.getDocument.and.returnValue(document);
    });

    describe('displayName', () => {
      it('is set to the display name of a document', () => {
        expect($ctrl.getDocument().displayName).toBe('test name');
      });
    });

    describe('id', () => {
      it('is set to the id of a document', () => {
        expect($ctrl.getDocument().id).toBe('handle-id');
      });
    });

    describe('locale', () => {
      it('is set to the locale of a document', () => {
        expect($ctrl.getDocument().locale).toBe('sv');
      });
    });

    describe('mode', () => {
      it('is set to edit, which is the only mode supported', () => {
        expect($ctrl.getDocument().mode).toBe('edit');
      });
    });

    describe('urlName', () => {
      it('is set to the url name of a document', () => {
        expect($ctrl.getDocument().urlName).toBe('test-url-name');
      });
    });

    describe('variant id', () => {
      it('is set to the id of the document variant', () => {
        expect($ctrl.getDocument().variant.id).toBe('variant-id');
      });
    });
  });

  describe('openDialog', () => {
    it('opens a dialog and returns a value when the dialog is confirmed', (done) => {
      DialogService.show.and.returnValue($q.resolve('test-value'));

      $ctrl.openDialog().then((value) => {
        expect(DialogService.show).toHaveBeenCalled();
        expect(value).toBe('test-value');
        expect($ctrl.isDialogOpen).toBe(false);
        done();
      });
      $rootScope.$digest();
    });

    it('rejects with DialogCanceled when the dialog is canceled', (done) => {
      DialogService.show.and.returnValue($q.reject());

      $ctrl.openDialog().catch((value) => {
        expect(value).toEqual({ code: 'DialogCanceled', message: 'The dialog is canceled' });
        expect(DialogService.show).toHaveBeenCalled();
        expect($ctrl.isDialogOpen).toBe(false);
        done();
      });
      $rootScope.$digest();
    });

    it('rejects with DialogExists when another dialog is already open', () => {
      $ctrl.isDialogOpen = true;
      const result = $ctrl.openDialog({});
      $rootScope.$digest();

      expect(DialogService.show).not.toHaveBeenCalled();
      expect(result.$$state.value).toEqual({ code: 'DialogExists', message: 'A dialog already exists' });
      expect($ctrl.isDialogOpen).toBe(true);
    });
  });
});
