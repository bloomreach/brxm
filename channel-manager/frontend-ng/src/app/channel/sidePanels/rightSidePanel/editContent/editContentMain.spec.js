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
  let $scope;
  let CmsService;
  let ContentEditor;
  let EditContentService;
  let HippoIframeService;
  let RightSidePanelService;

  let $ctrl;
  let form;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject(($controller, _$q_, $rootScope) => {
      $q = _$q_;

      CmsService = jasmine.createSpyObj('CmsService', ['publish', 'reportUsageStatistic']);
      ContentEditor = jasmine.createSpyObj('ContentEditor', [
        'close',
        'confirmDiscardChanges',
        'confirmPublication',
        'confirmSaveOrDiscardChanges',
        'discardChanges',
        'getDocumentId',
        'getDocumentType',
        'isDocumentDirty',
        'isEditing',
        'isPublishAllowed',
        'isEditing',
        'publish',
        'save',
      ]);
      EditContentService = jasmine.createSpyObj('EditContentService', ['stopEditing']);
      HippoIframeService = jasmine.createSpyObj('HippoIframeService', ['reload']);
      RightSidePanelService = jasmine.createSpyObj('RightSidePanelService', [
        'isClosing',
        'setClosing',
        'startLoading',
        'stopLoading',
      ]);

      $scope = $rootScope.$new();
      form = jasmine.createSpyObj('form', ['$setPristine']);
      $ctrl = $controller('editContentMainCtrl as $ctrl', {
        $scope,
        CmsService,
        ContentEditor,
        EditContentService,
        HippoIframeService,
        RightSidePanelService,
      },
      { form },
      );

      $ctrl.$onInit();
      $scope.$digest();
    });
  });

  it('starts loading when "loading" is set to true', () => {
    expect(RightSidePanelService.startLoading).not.toHaveBeenCalled();
    $ctrl.loading = true;
    $scope.$digest();
    expect(RightSidePanelService.startLoading).toHaveBeenCalled();
  });

  it('stops loading when "loading" is set to false', () => {
    expect(RightSidePanelService.stopLoading).not.toHaveBeenCalled();
    $ctrl.loading = false;
    $scope.$digest();
    expect(RightSidePanelService.stopLoading).toHaveBeenCalled();
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

  describe('save', () => {
    it('reloads the iframe', (done) => {
      ContentEditor.save.and.returnValue($q.resolve());

      $ctrl.save().then(() => {
        expect(HippoIframeService.reload).toHaveBeenCalled();
        done();
      });
      $scope.$digest();
    });

    it('reports a usage statistic', (done) => {
      ContentEditor.save.and.returnValue($q.resolve());

      $ctrl.save().then(() => {
        expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('CMSChannelsSaveDocument');
        done();
      });
      $scope.$digest();
    });

    it('shows the loading indicator while saving and resets it once done', () => {
      ContentEditor.save.and.returnValue($q.resolve());
      spyOn($ctrl, 'showLoadingIndicator').and.callThrough();

      $ctrl.save();
      $scope.$digest();

      expect($ctrl.showLoadingIndicator).toHaveBeenCalled();
      expect($ctrl.loading).toBe(false);
    });
  });

  it('can switch the editor', () => {
    ContentEditor.getDocumentId.and.returnValue('42');

    $ctrl.switchEditor();

    expect(CmsService.publish).toHaveBeenCalledWith('open-content', '42', 'edit');
    expect(ContentEditor.close).toHaveBeenCalled();
    expect(EditContentService.stopEditing).toHaveBeenCalled();
    expect($ctrl.closing).toBeFalsy();
  });

  it('knows when publish is allowed', () => {
    [true, false].forEach((editorAllowsPublish) => {
      [true, false].forEach((dirty) => {
        ContentEditor.isPublishAllowed.and.returnValue(editorAllowsPublish);
        ContentEditor.isDocumentDirty.and.returnValue(dirty);
        expect($ctrl.isPublishAllowed()).toBe(editorAllowsPublish && !dirty);
      });
    });
  });

  it('knows when save is allowed', () => {
    [true, false].forEach((editing) => {
      [true, false].forEach((dirty) => {
        [true, false].forEach((valid) => {
          ContentEditor.isEditing.and.returnValue(editing);
          ContentEditor.isDocumentDirty.and.returnValue(dirty);
          form.$valid = valid;
          expect($ctrl.isSaveAllowed()).toBe(editing && dirty && valid);
        });
      });
    });
  });

  describe('publish', () => {
    it('shows a confirmation dialog', () => {
      ContentEditor.confirmPublication.and.returnValue($q.resolve());

      $ctrl.publish();
      $scope.$digest();

      expect(ContentEditor.confirmPublication).toHaveBeenCalled();
      expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('VisualEditingPublishButton');
    });

    it('does not publish nor save if the confirmation dialog is cancelled for a publication', () => {
      ContentEditor.isDocumentDirty.and.returnValue(true);
      ContentEditor.confirmPublication.and.returnValue($q.reject());

      $ctrl.publish();
      $scope.$digest();

      expect(ContentEditor.save).not.toHaveBeenCalled();
      expect(ContentEditor.publish).not.toHaveBeenCalled();
      expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('VisualEditingPublishButton');
    });

    it('does not publish nor save if the confirmation dialog is cancelled for a request publication', () => {
      ContentEditor.isDocumentDirty.and.returnValue(true);
      ContentEditor.confirmPublication.and.returnValue($q.reject());

      $ctrl.publish();
      $scope.$digest();

      expect(ContentEditor.save).not.toHaveBeenCalled();
      expect(ContentEditor.publish).not.toHaveBeenCalled();
      expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('VisualEditingPublishButton');
    });

    it('publishes if the confirmation dialog is confirmed', () => {
      ContentEditor.confirmPublication.and.returnValue($q.resolve());

      $ctrl.publish();
      $scope.$digest();

      expect(ContentEditor.publish).toHaveBeenCalled();
      expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('VisualEditingPublishButton');
    });

    it('saves the form of a dirty document, prior to publishing', () => {
      ContentEditor.isDocumentDirty.and.returnValue(true);
      ContentEditor.save.and.returnValue($q.resolve());
      ContentEditor.confirmPublication.and.returnValue($q.resolve());

      $ctrl.publish();
      $scope.$digest();

      expect(ContentEditor.save).toHaveBeenCalled();
      expect(form.$setPristine).toHaveBeenCalled();
      expect(ContentEditor.publish).toHaveBeenCalled();
    });

    it('does not publish if saving fails', () => {
      ContentEditor.confirmPublication.and.returnValue($q.resolve());
      ContentEditor.isDocumentDirty.and.returnValue(true);
      ContentEditor.save.and.returnValue($q.reject());

      $ctrl.publish();
      $scope.$digest();

      expect(ContentEditor.publish).not.toHaveBeenCalled();
    });

    it('shows the loading indicator while publishing and resets it once done', () => {
      ContentEditor.confirmPublication.and.returnValue($q.resolve());
      spyOn($ctrl, 'showLoadingIndicator').and.callThrough();

      $ctrl.publish();
      $scope.$digest();

      expect($ctrl.showLoadingIndicator).toHaveBeenCalled();
      expect($ctrl.loading).toBe(false);
    });
  });

  describe('ui-router state exit', () => {
    describe('on close', () => {
      beforeEach(() => {
        RightSidePanelService.isClosing.and.returnValue(true);
      });

      it('succeeds when discarding changes', (done) => {
        ContentEditor.confirmDiscardChanges.and.returnValue($q.resolve());
        ContentEditor.discardChanges.and.returnValue($q.resolve());

        $ctrl.uiCanExit().then(() => {
          expect(ContentEditor.confirmDiscardChanges).toHaveBeenCalledWith('CONFIRM_DISCARD_UNSAVED_CHANGES_MESSAGE');
          expect(ContentEditor.discardChanges).toHaveBeenCalled();
          expect(ContentEditor.close).toHaveBeenCalled();
          done();
        });
        $scope.$digest();
      });

      it('succeeds and still closes the editor when discarding changes fails after discard changes confirmation', (done) => {
        ContentEditor.confirmDiscardChanges.and.returnValue($q.resolve());
        ContentEditor.discardChanges.and.returnValue($q.reject());

        $ctrl.uiCanExit().then(() => {
          expect(ContentEditor.confirmDiscardChanges).toHaveBeenCalledWith('CONFIRM_DISCARD_UNSAVED_CHANGES_MESSAGE');
          expect(ContentEditor.discardChanges).toHaveBeenCalled();
          expect(ContentEditor.close).toHaveBeenCalled();
          done();
        });
        $scope.$digest();
      });

      it('has closed the editor when the returned promise resolves', (done) => {
        ContentEditor.confirmDiscardChanges.and.returnValue($q.resolve());

        const deferredDiscard = $q.defer();
        ContentEditor.discardChanges.and.returnValue(deferredDiscard.promise);

        $ctrl.uiCanExit().then(() => {
          expect(ContentEditor.confirmDiscardChanges).toHaveBeenCalledWith('CONFIRM_DISCARD_UNSAVED_CHANGES_MESSAGE');
          expect(ContentEditor.discardChanges).toHaveBeenCalled();
          expect(ContentEditor.close).toHaveBeenCalled();
          done();
        });

        $scope.$digest();
        expect(ContentEditor.close).not.toHaveBeenCalled();

        deferredDiscard.reject();
        $scope.$digest();
      });

      it('fails when discarding changes is canceled', (done) => {
        ContentEditor.confirmDiscardChanges.and.returnValue($q.reject());

        $ctrl.uiCanExit().catch(() => {
          expect(ContentEditor.confirmDiscardChanges).toHaveBeenCalledWith('CONFIRM_DISCARD_UNSAVED_CHANGES_MESSAGE');
          done();
        });
        $scope.$digest();
      });
    });

    describe('when opening another document', () => {
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

      it('succeeds and still closes the editor when discarding changes fails after confirmation of saving changes', (done) => {
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

      it('succeeds and still closes the editor when discarding changes fails after discard changes confirmation', (done) => {
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
