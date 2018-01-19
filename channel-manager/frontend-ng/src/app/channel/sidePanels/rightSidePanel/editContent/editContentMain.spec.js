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

describe('EditContentMainCtrl', () => {
  let $q;
  let $rootScope;
  let CmsService;
  let ContentEditor;
  let EditContentService;
  let HippoIframeService;

  let $ctrl;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject(($controller, _$q_, _$rootScope_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;

      CmsService = jasmine.createSpyObj('CmsService', ['publish', 'reportUsageStatistic']);
      ContentEditor = jasmine.createSpyObj('ContentEditor', [
        'close', 'confirmDiscardChanges', 'confirmSaveOrDiscardChanges', 'deleteDraft', 'getDocumentId', 'isDocumentDirty',
      ]);
      EditContentService = jasmine.createSpyObj('EditContentService', ['stopEditing']);
      HippoIframeService = jasmine.createSpyObj('HippoIframeService', ['reload']);

      const $scope = $rootScope.$new();
      $ctrl = $controller('editContentMainCtrl', {
        $scope,
        CmsService,
        ContentEditor,
        EditContentService,
        HippoIframeService,
      });
    });
  });

  describe('on save', () => {
    it('reloads the iframe', () => {
      $ctrl.save();
      expect(HippoIframeService.reload).toHaveBeenCalled();
    });
    it('reports a usage statistic', () => {
      $ctrl.save();
      expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('CMSChannelsSaveDocument');
    });
  });

  it('on close stops editing', () => {
    $ctrl.close();
    expect(EditContentService.stopEditing).toHaveBeenCalled();
  });

  it('can switch the editor', () => {
    ContentEditor.getDocumentId.and.returnValue('42');

    $ctrl.switchEditor();

    expect(CmsService.publish).toHaveBeenCalledWith('open-content', '42', 'edit');
    expect(ContentEditor.close).toHaveBeenCalled();
    expect(EditContentService.stopEditing).toHaveBeenCalled();
    expect($ctrl.closing).toBeFalsy();
  });

  describe('ui-router state exit', () => {
    describe('on close', () => {
      beforeEach(() => {
        $ctrl.close();
      });

      it('succeeds when discarding changes', (done) => {
        ContentEditor.confirmDiscardChanges.and.returnValue($q.resolve());
        ContentEditor.deleteDraft.and.returnValue($q.resolve());

        $ctrl.uiCanExit().then(() => {
          expect(ContentEditor.confirmDiscardChanges).toHaveBeenCalled();
          expect(ContentEditor.deleteDraft).toHaveBeenCalled();
          expect(ContentEditor.close).toHaveBeenCalled();
          done();
        });
        $rootScope.$digest();
      });

      it('succeeds and still closes the editor when deleting the draft fails after discarding changes', (done) => {
        ContentEditor.confirmDiscardChanges.and.returnValue($q.resolve());
        ContentEditor.deleteDraft.and.returnValue($q.reject());

        $ctrl.uiCanExit().then(() => {
          expect(ContentEditor.confirmDiscardChanges).toHaveBeenCalled();
          expect(ContentEditor.deleteDraft).toHaveBeenCalled();
          expect(ContentEditor.close).toHaveBeenCalled();
          done();
        });
        $rootScope.$digest();
      });

      it('fails when discarding changes is canceled', (done) => {
        ContentEditor.confirmDiscardChanges.and.returnValue($q.reject());

        $ctrl.uiCanExit().catch(() => {
          expect(ContentEditor.confirmDiscardChanges).toHaveBeenCalled();
          done();
        });
        $rootScope.$digest();
      });
    });

    describe('when opening another document', () => {
      it('succeeds when saving changes and reloads the iframe', (done) => {
        ContentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.resolve('SAVE'));
        ContentEditor.deleteDraft.and.returnValue($q.resolve());

        $ctrl.uiCanExit().then(() => {
          expect(ContentEditor.confirmSaveOrDiscardChanges).toHaveBeenCalled();
          expect(ContentEditor.deleteDraft).toHaveBeenCalled();
          expect(ContentEditor.close).toHaveBeenCalled();
          expect(HippoIframeService.reload).toHaveBeenCalled();
          done();
        });
        $rootScope.$digest();
      });

      it('succeeds and still closes the editor when deleting the draft fails after saving changes', (done) => {
        ContentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.resolve('SAVE'));
        ContentEditor.deleteDraft.and.returnValue($q.reject());

        $ctrl.uiCanExit().then(() => {
          expect(ContentEditor.confirmSaveOrDiscardChanges).toHaveBeenCalled();
          expect(ContentEditor.deleteDraft).toHaveBeenCalled();
          expect(ContentEditor.close).toHaveBeenCalled();
          expect(HippoIframeService.reload).toHaveBeenCalled();
          done();
        });
        $rootScope.$digest();
      });

      it('succeeds when discarding changes', (done) => {
        ContentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.resolve('DISCARD'));
        ContentEditor.deleteDraft.and.returnValue($q.resolve());

        $ctrl.uiCanExit().then(() => {
          expect(ContentEditor.confirmSaveOrDiscardChanges).toHaveBeenCalled();
          expect(ContentEditor.deleteDraft).toHaveBeenCalled();
          expect(ContentEditor.close).toHaveBeenCalled();
          expect(HippoIframeService.reload).not.toHaveBeenCalled();
          done();
        });
        $rootScope.$digest();
      });

      it('succeeds and still closes the editor when deleting the draft fails after discarding changes', (done) => {
        ContentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.resolve('DISCARD'));
        ContentEditor.deleteDraft.and.returnValue($q.reject());

        $ctrl.uiCanExit().then(() => {
          expect(ContentEditor.confirmSaveOrDiscardChanges).toHaveBeenCalled();
          expect(ContentEditor.deleteDraft).toHaveBeenCalled();
          expect(ContentEditor.close).toHaveBeenCalled();
          expect(HippoIframeService.reload).not.toHaveBeenCalled();
          done();
        });
        $rootScope.$digest();
      });

      it('fails when save or discard changes is canceled', (done) => {
        ContentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.reject());

        $ctrl.uiCanExit().catch(() => {
          expect(ContentEditor.confirmSaveOrDiscardChanges).toHaveBeenCalled();
          done();
        });
        $rootScope.$digest();
      });
    });

    it('succeeds when switching editor ', (done) => {
      // because the editor is already closed in switchEditor(),
      // no confirmation dialog will be shown and no draft will be deleted
      ContentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.resolve());
      ContentEditor.deleteDraft.and.returnValue($q.resolve());

      $ctrl.uiCanExit().then(done);
      $rootScope.$digest();
    });
  });
});
