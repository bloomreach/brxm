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
  let $rootScope;
  let CmsService;
  let ContentEditor;

  let $ctrl;
  let $q;
  let $scope;
  let form;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_, _$q_, _$rootScope_, _CmsService_) => {
      $componentController = _$componentController_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      CmsService = _CmsService_;
    });

    spyOn(CmsService, 'reportUsageStatistic');

    ContentEditor = jasmine.createSpyObj('ContentEditor', [
      'getDocument',
      'getDocumentType',
      'getError',
      'isEditing',
      'markDocumentDirty',
      'cancelRequestPublication',
    ]);

    $scope = $rootScope.$new();

    form = jasmine.createSpyObj('form', ['$setPristine']);

    $ctrl = $componentController('contentEditor',
      { $scope, ContentEditor },
      { form });
    $scope.$digest();
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

  describe('cancelRequestPublication', () => {
    it('cancels the publication request showing a loading indicator', () => {
      spyOn($ctrl, 'startLoading').and.callThrough();
      ContentEditor.cancelRequestPublication.and.returnValue($q.resolve());

      $ctrl.cancelRequestPublication();
      $scope.$digest();

      expect(ContentEditor.cancelRequestPublication).toHaveBeenCalled();
      expect($ctrl.startLoading).toHaveBeenCalled();
      expect($ctrl.loading).toBe(false);
      expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('VisualEditingCancelRequest');
    });
  });
});
