/*
 * Copyright 2017-2020 Hippo B.V. (http://www.onehippo.com)
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

const testDocument = {
  id: 'testId',
  displayName: 'test document',
  info: {
    dirty: false,
    type: {
      id: 'ns:testdocument',
    },
  },
};

describe('Create content step 2 controller', () => {
  let $rootScope;
  let $q;
  let $scope;
  let $timeout;
  let $translate;
  let CmsService;
  let ContentEditor;
  let CreateContentService;
  let DialogService;
  let FeedbackService;
  let RightSidePanelService;
  let Step2Service;

  let $ctrl;
  let form;

  const $element = angular.element('<form></form>');

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.createContent.step2');

    inject((
      $controller,
      _$rootScope_,
      _$translate_,
      _$q_,
      _$timeout_,
      _CmsService_,
      _ContentEditor_,
      _CreateContentService_,
      _DialogService_,
      _FeedbackService_,
      _Step2Service_,
    ) => {
      $rootScope = _$rootScope_;
      $q = _$q_;
      $scope = $rootScope.$new();
      $timeout = _$timeout_;
      $translate = _$translate_;
      CmsService = _CmsService_;
      ContentEditor = _ContentEditor_;
      CreateContentService = _CreateContentService_;
      DialogService = _DialogService_;
      FeedbackService = _FeedbackService_;
      RightSidePanelService = jasmine.createSpyObj('RightSidePanelService', ['startLoading', 'stopLoading']);
      Step2Service = _Step2Service_;

      form = jasmine.createSpyObj('form', ['$setPristine', 'focus']);
      $ctrl = $controller('step2Ctrl as $ctrl', {
        $scope,
        $element,
        RightSidePanelService,
      },
      { form });
    });

    spyOn(CmsService, 'reportUsageStatistic');
    spyOn(ContentEditor, 'getDocument').and.returnValue(testDocument);
    spyOn(ContentEditor, 'getDocumentId').and.returnValue(testDocument.id);
    spyOn(DialogService, 'show');
    spyOn(FeedbackService, 'showError');
    spyOn(FeedbackService, 'showNotification');
  });

  describe('$onInit', () => {
    it('set documentIsSaved state to false', () => {
      $ctrl.documentIsSaved = true;
      $ctrl.$onInit();
      expect($ctrl.documentIsSaved).toBe(false);
    });

    it('does focus the form', () => {
      spyOn($element, 'find').and.returnValue(form);
      $ctrl.$onInit();
      expect($element.find).toHaveBeenCalledWith('form');
      expect(form.focus).toHaveBeenCalled();
    });
  });

  describe('loading indicator', () => {
    it('starts loading when "loading" is set to true', () => {
      $ctrl.$onInit();
      $scope.$digest();
      expect(RightSidePanelService.startLoading).not.toHaveBeenCalled();
      $ctrl.loading = true;
      $scope.$digest();
      expect(RightSidePanelService.startLoading).toHaveBeenCalled();
    });

    it('stops loading when "loading" is set to false', () => {
      $ctrl.$onInit();
      $scope.$digest();
      expect(RightSidePanelService.stopLoading).not.toHaveBeenCalled();
      $ctrl.loading = false;
      $scope.$digest();
      expect(RightSidePanelService.stopLoading).toHaveBeenCalled();
    });
  });

  it('knows when all mandatory fields are shown', () => {
    spyOn(ContentEditor, 'isEditing');
    spyOn(ContentEditor, 'getDocumentType');

    [true, false].forEach((editing) => {
      [true, false].forEach((canCreateAllRequiredFields) => {
        ContentEditor.isEditing.and.returnValue(editing);
        ContentEditor.getDocumentType.and.returnValue({ canCreateAllRequiredFields });
        expect($ctrl.allMandatoryFieldsShown()).toBe(editing && canCreateAllRequiredFields);
      });
    });
  });

  it('knows when the content editor is editing', () => {
    spyOn(ContentEditor, 'isEditing');

    ContentEditor.isEditing.and.returnValue(true);
    expect($ctrl.isEditing()).toBe(true);

    ContentEditor.isEditing.and.returnValue(false);
    expect($ctrl.isEditing()).toBe(false);
  });

  it('can switch the editor', () => {
    spyOn(CmsService, 'publish');
    spyOn(ContentEditor, 'close');
    spyOn(CreateContentService, 'stop');

    $ctrl.switchEditor();

    expect(CmsService.publish).toHaveBeenCalledWith('open-content', testDocument.id, 'edit');
    expect(ContentEditor.close).toHaveBeenCalled();
    expect(CreateContentService.stop).toHaveBeenCalled();
  });

  it('discards the editable instance, saves the component parameter and finishes create-content on save', () => {
    spyOn(ContentEditor, 'discardChanges').and.returnValue($q.resolve());
    spyOn(Step2Service, 'saveComponentParameter').and.returnValue($q.resolve());
    spyOn(CreateContentService, 'finish');
    spyOn(ContentEditor, 'save').and.returnValue($q.resolve());

    $ctrl.save();
    $timeout.flush();

    expect($ctrl.documentIsSaved).toBe(true);
    expect(ContentEditor.discardChanges).toHaveBeenCalled();
    expect(FeedbackService.showNotification).toHaveBeenCalled();
    expect(CreateContentService.finish).toHaveBeenCalledWith('testId');
    expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('CreateContent2Done');
  });

  it('saves the document even if there are no field updates', (done) => {
    spyOn(ContentEditor, 'save').and.returnValue($q.resolve());

    $ctrl.save().then(() => {
      expect(ContentEditor.save).toHaveBeenCalledWith(true);
      done();
    });

    $rootScope.$digest();
  });

  describe('server-side validation error', () => {
    let $$element;

    beforeEach(() => {
      $$element = { focus: jasmine.createSpy() };
      $ctrl.form = {
        $error: {
          server: [{ $$element }],
        },
      };
      spyOn(ContentEditor, 'save').and.returnValue($q.reject());
    });

    it('sets focus on the first element on server-side validation error', () => {
      $ctrl.save();
      $rootScope.$digest();

      expect($$element.focus).toHaveBeenCalled();
    });

    it('removes an old watch for setting focus on the first element on server-side validation error', () => {
      const stopPreviousWatch = jasmine.createSpy('stopPreviousWatch');
      $ctrl.stopServerErrorWatch = stopPreviousWatch;

      $ctrl.save();
      $rootScope.$digest();

      expect(stopPreviousWatch).toHaveBeenCalled();
      expect($ctrl.stopServerErrorWatch).toBeUndefined();
    });
  });

  it('stops create content when close is called', () => {
    spyOn(CreateContentService, 'stop');
    $ctrl.close();
    expect(CreateContentService.stop).toHaveBeenCalled();
  });

  it('returns the document reference of the ContentEditor', () => {
    expect($ctrl.getDocument()).toBe(testDocument);
  });

  it('returns the document error messages', () => {
    const errorMessages = [
      'some error',
      'another error',
    ];
    spyOn(ContentEditor, 'getDocumentErrorMessages').and.returnValue(errorMessages);
    expect($ctrl.getDocumentErrorMessages()).toBe(errorMessages);
  });

  it('opens the edit-name-url dialog', () => {
    spyOn(Step2Service, 'openEditNameUrlDialog');
    $ctrl.openEditNameUrlDialog();
    expect(Step2Service.openEditNameUrlDialog).toHaveBeenCalled();
  });

  describe('confirm discard changes', () => {
    const showPromise = {};

    beforeEach(() => {
      ContentEditor.document = {
        displayName: 'Test',
      };
      spyOn($translate, 'instant');
      spyOn(DialogService, 'confirm').and.callThrough();
      DialogService.show.and.returnValue(showPromise);
    });

    it('shows a dialog with a title', () => {
      const result = $ctrl.confirmDiscardChanges('MESSAGE_KEY', 'TITLE_KEY');

      expect(DialogService.confirm).toHaveBeenCalled();
      expect($translate.instant).toHaveBeenCalledWith('MESSAGE_KEY', {
        documentName: 'Test',
      });
      expect($translate.instant).toHaveBeenCalledWith('TITLE_KEY', {
        documentName: 'Test',
      });
      expect(DialogService.show).toHaveBeenCalled();
      expect(result).toBe(showPromise);
    });

    it('does not show a dialog when the editor is killed', (done) => {
      ContentEditor.kill();
      $ctrl.confirmDiscardChanges().then(() => {
        expect(DialogService.show).not.toHaveBeenCalled();
        done();
      });
      $rootScope.$digest();
    });
  });

  describe('uiCanExit', () => {
    let deleteDocumentSpy;

    beforeEach(() => {
      spyOn($ctrl, 'confirmDiscardChanges');
      deleteDocumentSpy = spyOn(ContentEditor, 'deleteDocument').and.returnValue($q.resolve());
    });

    it('allows ui-exit without dialog if document is already saved', () => {
      $ctrl.documentIsSaved = true;
      expect($ctrl.uiCanExit()).toBe(true);
      expect($ctrl.confirmDiscardChanges).not.toHaveBeenCalled();
    });

    it('allows ui-exit without dialog when switching editor', () => {
      $ctrl.switchingEditor = true;
      expect($ctrl.uiCanExit()).toBe(true);
      expect($ctrl.confirmDiscardChanges).not.toHaveBeenCalled();
    });

    it('calls confirmDiscardChanges if document is not yet saved', () => {
      $ctrl.confirmDiscardChanges.and.returnValue($q.resolve());
      $ctrl.uiCanExit();
      expect($ctrl.confirmDiscardChanges).toHaveBeenCalled();
    });

    it('does not discard the changes if confirmDiscardChanges is canceled', (done) => {
      $ctrl.confirmDiscardChanges.and.returnValue($q.reject());

      $ctrl.uiCanExit().then(
        () => fail('Dialog promise should not be resolved'),
        () => {
          expect(deleteDocumentSpy).not.toHaveBeenCalled();
          done();
        },
      );
      $rootScope.$digest();
    });

    it('deletes the document when confirmDiscardChanges is resolved', (done) => {
      $ctrl.confirmDiscardChanges.and.returnValue($q.resolve());

      $ctrl.uiCanExit().then(() => {
        expect(deleteDocumentSpy).toHaveBeenCalled();
        done();
      }, () => fail('Dialog should not reject'));
      $rootScope.$digest();
    });
  });
});
