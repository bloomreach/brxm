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

describe('EditContentToolsCtrl', () => {
  let $q;
  let $rootScope;
  let CmsService;
  let ContentEditor;
  let EditContentService;

  let $ctrl;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject(($controller, _$q_, _$rootScope_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;

      CmsService = jasmine.createSpyObj('CmsService', ['publish', 'reportUsageStatistic']);
      ContentEditor = jasmine.createSpyObj('ContentEditor', [
        'close',
        'confirmSaveOrDiscardChanges',
        'discardChanges',
        'getDocumentId',
        'getError',
        'getPublicationState',
        'isEditing',
      ]);
      EditContentService = jasmine.createSpyObj('EditContentService', ['stopEditing']);

      const $scope = $rootScope.$new();
      $ctrl = $controller('editContentToolsCtrl', {
        $scope,
        CmsService,
        ContentEditor,
        EditContentService,
      });
    });
  });

  it('always allows exiting the ui-router state when switch to content editor has not been clicked', () => {
    expect($ctrl.uiCanExit()).toBe(true);
  });

  it('disables buttons when the content editor error says so', () => {
    ContentEditor.getError.and.returnValue({
      disableContentButtons: true,
    });
    expect($ctrl.isDisabled()).toBeTruthy();

    ContentEditor.getError.and.returnValue({
      disableContentButtons: false,
    });
    expect($ctrl.isDisabled()).toBeFalsy();

    ContentEditor.getError.and.returnValue(undefined);
    expect($ctrl.isDisabled()).toBeFalsy();
  });

  describe('opens the content editor in view mode and', () => {
    const documentId = '42';

    beforeEach(() => {
      ContentEditor.getDocumentId.and.returnValue(documentId);

      $ctrl.openContentEditor('view');
      expect(EditContentService.stopEditing).toHaveBeenCalled();
    });

    function expectSuccess() {
      expect(ContentEditor.confirmSaveOrDiscardChanges).toHaveBeenCalledWith('SAVE_CHANGES_TO_DOCUMENT');
      expect(ContentEditor.discardChanges).toHaveBeenCalled();
      expect(CmsService.publish).toHaveBeenCalledWith('open-content', documentId, 'view');
      expect(ContentEditor.close).toHaveBeenCalled();
      expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('VisualEditingUnknownIcon');
    }

    it('succeeds', (done) => {
      ContentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.resolve());
      ContentEditor.discardChanges.and.returnValue($q.resolve());

      $ctrl.uiCanExit().then(() => {
        expectSuccess();
        done();
      });
      $rootScope.$digest();
    });

    it('fails because save/discard changes is canceled', (done) => {
      ContentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.reject());

      $ctrl.uiCanExit().catch(() => {
        expect(ContentEditor.confirmSaveOrDiscardChanges).toHaveBeenCalledWith('SAVE_CHANGES_TO_DOCUMENT');
        done();
      });
      $rootScope.$digest();
    });

    it('fails because the changes cannot be discarded', (done) => {
      ContentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.resolve());
      ContentEditor.discardChanges.and.returnValue($q.reject());

      $ctrl.uiCanExit().catch(() => {
        expect(ContentEditor.confirmSaveOrDiscardChanges).toHaveBeenCalledWith('SAVE_CHANGES_TO_DOCUMENT');
        expect(ContentEditor.discardChanges).toHaveBeenCalled();
        done();
      });
      $rootScope.$digest();
    });

    it('succeeds after a previous attempt failed because save/discard changes was canceled', (done) => {
      ContentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.reject());
      $ctrl.uiCanExit().catch(() => {
        $ctrl.openContentEditor('view');
        ContentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.resolve());
        $ctrl.uiCanExit().then(() => {
          expectSuccess();
          done();
        });
      });
      $rootScope.$digest();
    });
  });

  describe('report statistics when clicking the status icon', () => {
    const documentId = '42';

    beforeEach(() => {
      ContentEditor.getDocumentId.and.returnValue(documentId);
    });

    it('reports that the offline icon is used to go to the content editor', () => {
      ContentEditor.getPublicationState.and.returnValue('new');
      $ctrl.openContentEditor('view');
      ContentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.resolve());
      ContentEditor.discardChanges.and.returnValue($q.resolve());

      $ctrl.uiCanExit().then(() => {
        expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('VisualEditingOfflineIcon');
      });
      $rootScope.$digest();
    });

    it('reports that the online icon is used to go to the content editor', () => {
      ContentEditor.getPublicationState.and.returnValue('live');
      $ctrl.openContentEditor('view');
      ContentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.resolve());
      ContentEditor.discardChanges.and.returnValue($q.resolve());

      $ctrl.uiCanExit().then(() => {
        expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('VisualEditingOnlineIcon');
      });
      $rootScope.$digest();
    });

    it('reports that the alert icon is used to go to the content editor', () => {
      ContentEditor.getPublicationState.and.returnValue('changed');
      $ctrl.openContentEditor('view');
      ContentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.resolve());
      ContentEditor.discardChanges.and.returnValue($q.resolve());

      $ctrl.uiCanExit().then(() => {
        expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('VisualEditingAlertIcon');
      });
      $rootScope.$digest();
    });
  });
});
