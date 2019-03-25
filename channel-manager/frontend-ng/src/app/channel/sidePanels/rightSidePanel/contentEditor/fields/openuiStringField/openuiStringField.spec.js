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
  let mdInputContainer;
  let ngModel;
  let ContentEditor;
  let OpenUiService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_, _$log_, _$q_, _$rootScope_, _ContentEditor_, _OpenUiService_) => {
      $componentController = _$componentController_;
      $q = _$q_;
      ContentEditor = _ContentEditor_;
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
  });

  it('initializes the component', () => {
    $ctrl.$onInit();

    expect(mdInputContainer.setHasValue).toHaveBeenCalledWith(true);
  });

  describe('$onChanges', () => {
    it('connects to the child', () => {
      spyOn(OpenUiService, 'initialize').and.returnValue($q.resolve());
      $ctrl.$onChanges({ extensionId: { currentValue: 'test-id' } });

      expect(OpenUiService.initialize).toHaveBeenCalledWith('test-id', jasmine.objectContaining({
        appendTo: $element[0],
      }));
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

  describe('getProperties', () => {
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
        expect($ctrl.getProperties().document.displayName).toBe('test name');
      });
    });

    describe('id', () => {
      it('is set to the id of a document', () => {
        expect($ctrl.getProperties().document.id).toBe('handle-id');
      });
    });

    describe('locale', () => {
      it('is set to the locale of a document', () => {
        expect($ctrl.getProperties().document.locale).toBe('sv');
      });
    });

    describe('mode', () => {
      it('is set to edit, which is the only mode supported', () => {
        expect($ctrl.getProperties().document.mode).toBe('edit');
      });
    });

    describe('urlName', () => {
      it('is set to the url name of a document', () => {
        expect($ctrl.getProperties().document.urlName).toBe('test-url-name');
      });
    });

    describe('variant id', () => {
      it('is set to the id of the document variant', () => {
        expect($ctrl.getProperties().document.variant.id).toBe('variant-id');
      });
    });
  });
});
