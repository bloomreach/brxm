/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

describe('ContentEditorCtrl', () => {
  let $componentController;
  let $q;
  let $rootScope;
  let ContentEditor;

  let $ctrl;
  let $scope;
  let cancelLabel;
  let form;
  let onSave;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_, _$q_, _$rootScope_) => {
      $componentController = _$componentController_;
      $q = _$q_;
      $rootScope = _$rootScope_;
    });

    ContentEditor = jasmine.createSpyObj('ContentEditor', [
      'getDocument', 'getDocumentType', 'getError', 'isDocumentDirty', 'isDocumentDirty', 'isEditing',
      'markDocumentDirty', 'save',
    ]);

    $scope = $rootScope.$new();

    cancelLabel = 'CANCEL';
    form = jasmine.createSpyObj('form', ['$setPristine']);
    onSave = jasmine.createSpy('onSave');

    $ctrl = $componentController('contentEditor',
      { $scope, ContentEditor },
      { cancelLabel, form, onSave },
    );
    $rootScope.$digest();
  });

  it('marks the content editor dirty when the form becomes dirty', () => {
    $ctrl.$onInit();

    form.$dirty = false;
    $scope.$digest();
    expect(ContentEditor.markDocumentDirty).not.toHaveBeenCalled();

    form.$dirty = true;
    $scope.$digest();
    expect(ContentEditor.markDocumentDirty).toHaveBeenCalled();
  });

  it('knows when the content editor is editing', () => {
    ContentEditor.isEditing.and.returnValue(true);
    expect($ctrl.isEditing()).toBe(true);

    ContentEditor.isEditing.and.returnValue(false);
    expect($ctrl.isEditing()).toBe(false);
  });

  it('knows when save is allowed', () => {
    [true, false].forEach((editing) => {
      [true, false].forEach((dirty) => {
        [true, false].forEach((valid) => {
          [true, false].forEach((allRequiredFieldsIncluded) => {
            ContentEditor.isEditing.and.returnValue(editing);
            ContentEditor.isDocumentDirty.and.returnValue(dirty);
            form.$valid = valid;
            const docType = new DocumentType();
            docType.allRequiredFieldsIncluded = allRequiredFieldsIncluded;
            ContentEditor.getDocumentType.and.returnValue(docType);
            expect($ctrl.isSaveAllowed()).toBe(editing && dirty && valid && allRequiredFieldsIncluded);
          });
        });
      });
    });
  });

  it('knows when not all fields are shown', () => {
    [true, false].forEach((editing) => {
      [true, false].forEach((allFieldsIncluded) => {
        ContentEditor.isEditing.and.returnValue(editing);
        ContentEditor.getDocumentType.and.returnValue({ allFieldsIncluded });
        expect($ctrl.notAllFieldsShown()).toBe(editing && !allFieldsIncluded);
      });
    });
  });

  it('gets the field types', () => {
    const fields = [];
    ContentEditor.getDocumentType.and.returnValue({ fields });
    expect($ctrl.getFieldTypes()).toBe(fields);
  });

  it('gets the field values', () => {
    const fields = [];
    ContentEditor.getDocument.and.returnValue({ fields });
    expect($ctrl.getFieldValues()).toBe(fields);
  });

  it('gets the error', () => {
    const error = {};
    ContentEditor.getError.and.returnValue(error);
    expect($ctrl.getError()).toBe(error);
  });

  it('resets the form and calls the on-save handler when saving succeeds', () => {
    ContentEditor.save.and.returnValue($q.resolve());

    $ctrl.save();
    $rootScope.$digest();

    expect(form.$setPristine).toHaveBeenCalled();
    expect(onSave).toHaveBeenCalled();
  });

  it('does nothing when saving fails', () => {
    ContentEditor.save.and.returnValue($q.reject());

    $ctrl.save();
    $rootScope.$digest();

    expect(form.$setPristine).not.toHaveBeenCalled();
    expect(onSave).not.toHaveBeenCalled();
  });

  describe('close button label', () => {
    it('is the cancel label when the document is dirty', () => {
      ContentEditor.isDocumentDirty.and.returnValue(true);
      expect($ctrl.closeButtonLabel()).toBe(cancelLabel);
    });

    it('is "close" when the document is not dirty', () => {
      ContentEditor.isDocumentDirty.and.returnValue(false);
      expect($ctrl.closeButtonLabel()).toBe('CLOSE');
    });
  });
});
