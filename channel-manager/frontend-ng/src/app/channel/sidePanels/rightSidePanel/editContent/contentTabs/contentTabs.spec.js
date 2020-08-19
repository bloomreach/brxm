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

describe('ContentTabsCtrl', () => {
  let $q;
  let $scope;
  let ContentEditor;
  let HippoIframeService;

  let $ctrl;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject(($componentController, _$q_, $rootScope) => {
      $q = _$q_;
      ContentEditor = jasmine.createSpyObj('ContentEditor', [
        'close',
        'confirmPublication',
        'confirmSaveOrDiscardChanges',
        'discardChanges',
        'getDocument',
        'getDocumentDisplayName',
        'getDocumentErrorMessages',
        'getDocumentId',
        'getDocumentType',
        'isDocumentDirty',
        'isEditing',
        'isKeepDraftAllowed',
        'isPristine',
        'isPublishAllowed',
        'isRetainable',
        'keepDraft',
        'publish',
        'save',
      ]);
      HippoIframeService = jasmine.createSpyObj('HippoIframeService', ['reload']);
      $scope = $rootScope.$new();

      $ctrl = $componentController('contentTabs', {
        ContentEditor,
        HippoIframeService,
      });
    });
  });

  describe('ui-router state exit', () => {
    describe('when opening another document', () => {
      it('succeeds when document is retainable', (done) => {
        ContentEditor.isRetainable.and.returnValue(true);
        ContentEditor.keepDraft.and.returnValue($q.resolve());
        $ctrl.uiCanExit().then(() => {
          expect(ContentEditor.close).toHaveBeenCalled();
          done();
        });
        $scope.$digest();
      });
      it('succeeds when saving changes and reloads the iframe', (done) => {
        ContentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.resolve('SAVE'));
        ContentEditor.discardChanges.and.returnValue($q.resolve());

        $ctrl.uiCanExit().then(() => {
          expect(ContentEditor.confirmSaveOrDiscardChanges).toHaveBeenCalled();
          expect(ContentEditor.discardChanges).toHaveBeenCalled();
          expect(ContentEditor.close).toHaveBeenCalled();
          expect(HippoIframeService.reload).toHaveBeenCalled();
          done();
        });
        $scope.$digest();
      });

      it('succeeds and still closes the editor when discarding changes fails after confirmation of saving changes', (done) => { // eslint-disable-line max-len
        ContentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.resolve('SAVE'));
        ContentEditor.discardChanges.and.returnValue($q.reject());

        $ctrl.uiCanExit().then(() => {
          expect(ContentEditor.confirmSaveOrDiscardChanges).toHaveBeenCalled();
          expect(ContentEditor.discardChanges).toHaveBeenCalled();
          expect(ContentEditor.close).toHaveBeenCalled();
          expect(HippoIframeService.reload).toHaveBeenCalled();
          done();
        });
        $scope.$digest();
      });

      it('succeeds when discarding changes', (done) => {
        ContentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.resolve('DISCARD'));
        ContentEditor.discardChanges.and.returnValue($q.resolve());

        $ctrl.uiCanExit().then(() => {
          expect(ContentEditor.confirmSaveOrDiscardChanges).toHaveBeenCalled();
          expect(ContentEditor.discardChanges).toHaveBeenCalled();
          expect(ContentEditor.close).toHaveBeenCalled();
          expect(HippoIframeService.reload).not.toHaveBeenCalled();
          done();
        });
        $scope.$digest();
      });

      it('succeeds and still closes the editor when discarding changes fails after discard changes confirmation', (done) => { // eslint-disable-line max-len
        ContentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.resolve('DISCARD'));
        ContentEditor.discardChanges.and.returnValue($q.reject());

        $ctrl.uiCanExit().then(() => {
          expect(ContentEditor.confirmSaveOrDiscardChanges).toHaveBeenCalled();
          expect(ContentEditor.discardChanges).toHaveBeenCalled();
          expect(ContentEditor.close).toHaveBeenCalled();
          expect(HippoIframeService.reload).not.toHaveBeenCalled();
          done();
        });
        $scope.$digest();
      });

      it('fails when save or discard changes is canceled', (done) => {
        ContentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.reject());

        $ctrl.uiCanExit().catch(() => {
          expect(ContentEditor.confirmSaveOrDiscardChanges).toHaveBeenCalled();
          done();
        });
        $scope.$digest();
      });
    });

    it('succeeds when switching editor ', (done) => {
      // because the editor is already closed in switchEditor(),
      // no confirmation dialog will be shown and no document will be discarded
      ContentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.resolve());
      ContentEditor.discardChanges.and.returnValue($q.resolve());

      $ctrl.uiCanExit().then(done);
      $scope.$digest();
    });
  });
});
