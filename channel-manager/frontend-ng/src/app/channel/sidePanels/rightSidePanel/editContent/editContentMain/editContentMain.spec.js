/*
 * Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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
  let $translate;
  let CmsService;
  let ContentEditor;
  let DialogService;
  let EditContentService;
  let HippoIframeService;
  let RightSidePanelService;

  let $ctrl;
  let form;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject(($componentController, _$q_, $rootScope, _$translate_, _DialogService_) => {
      $q = _$q_;
      $translate = _$translate_;

      CmsService = jasmine.createSpyObj('CmsService', ['publish', 'reportUsageStatistic']);
      ContentEditor = jasmine.createSpyObj('ContentEditor', [
        'close',
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
      DialogService = _DialogService_;
      EditContentService = jasmine.createSpyObj('EditContentService', ['stopEditing', 'isEditingXPage']);
      HippoIframeService = jasmine.createSpyObj('HippoIframeService', ['reload']);
      RightSidePanelService = jasmine.createSpyObj('RightSidePanelService', [
        'setClosing',
        'startLoading',
        'stopLoading',
      ]);

      $scope = $rootScope.$new();
      form = jasmine.createSpyObj('form', ['$setPristine']);
      $ctrl = $componentController('editContentMain', {
        $scope,
        CmsService,
        ContentEditor,
        EditContentService,
        HippoIframeService,
        RightSidePanelService,
      },
      { form });

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

  it('returns the document error messages', () => {
    const errorMessages = [
      'some error',
      'another error',
    ];
    ContentEditor.getDocumentErrorMessages.and.returnValue(errorMessages);
    expect($ctrl.getDocumentErrorMessages()).toBe(errorMessages);
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
      spyOn($ctrl, 'startLoading').and.callThrough();

      $ctrl.save();
      $scope.$digest();

      expect($ctrl.startLoading).toHaveBeenCalled();
      expect($ctrl.loading).toBe(false);
    });

    describe('on server-side validation error', () => {
      let $$element;

      beforeEach(() => {
        $$element = { focus: jasmine.createSpy() };
        $ctrl.form = {
          $error: {
            server: [{ $$element }],
          },
        };
        ContentEditor.save.and.returnValue($q.reject());
      });

      it('sets focus on the first element on server-side validation error', () => {
        $ctrl.save();
        $scope.$digest();

        expect($$element.focus).toHaveBeenCalled();
      });

      it('removes an old watch for setting focus on the first element on server-side validation error', () => {
        const stopPreviousWatch = jasmine.createSpy('stopPreviousWatch');
        $ctrl.stopServerErrorWatch = stopPreviousWatch;

        $ctrl.save();
        $scope.$digest();

        expect(stopPreviousWatch).toHaveBeenCalled();
        expect($ctrl.stopServerErrorWatch).toBeUndefined();
      });
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
    [true, false].forEach((isXPage) => {
      [true, false].forEach((editorAllowsPublish) => {
        [true, false].forEach((dirty) => {
          EditContentService.isEditingXPage.and.returnValue(isXPage);
          ContentEditor.isPublishAllowed.and.returnValue(editorAllowsPublish);
          ContentEditor.isDocumentDirty.and.returnValue(dirty);
          expect($ctrl.isPublishAllowed()).toBe(!isXPage && editorAllowsPublish && !dirty);
        });
      });
    });
  });

  it('knows when save is allowed', () => {
    [true, false].forEach((editing) => {
      [true, false].forEach((dirty) => {
        [true, false].forEach((valid) => {
          [true, false].forEach((retainable) => {
            ContentEditor.isEditing.and.returnValue(editing);
            ContentEditor.isDocumentDirty.and.returnValue(dirty);
            ContentEditor.isRetainable.and.returnValue(retainable);
            form.$valid = valid;
            expect($ctrl.isSaveAllowed()).toBe(editing && (dirty || retainable) && valid);
          });
        });
      });
    });
  });

  it('knows when keep draft is shown', () => {
    [true, false].forEach((allowed) => {
      ContentEditor.isKeepDraftAllowed.and.returnValue(allowed);
      expect($ctrl.isKeepDraftShown()).toBe(allowed);
    });
  });

  it('knows when it is retainable', () => {
    [true, false].forEach((retainable) => {
      ContentEditor.isRetainable.and.returnValue(retainable);
      expect($ctrl.isRetainable()).toBe(retainable);
    });
  });

  it('knows when keep draft is enabled', () => {
    [true, false].forEach((retainable) => {
      ContentEditor.isRetainable.and.returnValue(retainable);
      expect($ctrl.isKeepDraftEnabled()).toBe(retainable);
    });
  });

  describe('publish', () => {
    it('publishes the document', () => {
      ContentEditor.publish.and.returnValue($q.resolve());

      $ctrl.publish();
      $scope.$digest();

      expect(ContentEditor.publish).toHaveBeenCalled();
      expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('VisualEditingPublishButton');
    });

    it('saves the form of a dirty document, prior to publishing', () => {
      ContentEditor.isDocumentDirty.and.returnValue(true);
      ContentEditor.save.and.returnValue($q.resolve());

      $ctrl.publish();
      $scope.$digest();

      expect(ContentEditor.save).toHaveBeenCalled();
      expect(form.$setPristine).toHaveBeenCalled();
      expect(ContentEditor.publish).toHaveBeenCalled();
    });

    it('does not publish if saving fails', () => {
      ContentEditor.isDocumentDirty.and.returnValue(true);
      ContentEditor.save.and.returnValue($q.reject());

      $ctrl.publish();
      $scope.$digest();

      expect(ContentEditor.publish).not.toHaveBeenCalled();
    });

    it('shows the loading indicator while publishing and resets it once done', (done) => {
      ContentEditor.publish.and.returnValue($q.resolve());
      spyOn($ctrl, 'startLoading').and.callThrough();

      $ctrl.publish().then(() => {
        expect($ctrl.startLoading).toHaveBeenCalled();
        expect($ctrl.loading).toBe(false);

        done();
      });

      $scope.$digest();
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

  describe('confirm discard changes', () => {
    beforeEach(() => {
      ContentEditor.getDocumentDisplayName.and.returnValue('Test');
      spyOn($translate, 'instant');
      spyOn(DialogService, 'confirm').and.callThrough();
      spyOn(DialogService, 'show').and.returnValue($q.resolve());
    });

    it('shows a dialog', () => {
      ContentEditor.isPristine.and.returnValue(false);
      $ctrl.discard();

      expect(DialogService.confirm).toHaveBeenCalled();
      expect(DialogService.show).toHaveBeenCalled();
    });

    it('shows a dialog with a document related message', () => {
      ContentEditor.isPristine.and.returnValue(false);

      $ctrl.discard();

      expect($translate.instant).toHaveBeenCalledWith('CONFIRM_DISCARD_DOCUMENT_UNSAVED_CHANGES_MESSAGE', {
        documentName: 'Test',
      });
    });

    it('shows a dialog with a page related message', () => {
      ContentEditor.isPristine.and.returnValue(false);
      EditContentService.isEditingXPage.and.returnValue(true);

      $ctrl.discard();

      expect($translate.instant).toHaveBeenCalledWith('CONFIRM_DISCARD_XPAGE_UNSAVED_CHANGES_MESSAGE', {
        documentName: 'Test',
      });
    });

    it('does not show a dialog when the document has not changed', () => {
      ContentEditor.isPristine.and.returnValue(true);
      $ctrl.discard();

      expect(DialogService.show).not.toHaveBeenCalled();
    });
  });
});
