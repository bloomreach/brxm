/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

describe('createContentStep2Controller', () => {
  let $componentController;
  let $q;
  let $rootScope;
  let $timeout;
  let $translate;
  let SidePanelService;
  let CmsService;
  let ContentService;
  let DialogService;
  let HippoIframeService;
  let FeedbackService;
  let CreateContentService;

  let $ctrl;
  let $scope;
  let dialog;

  const stringField = {
    id: 'ns:string',
    type: 'STRING',
  };
  const multipleStringField = {
    id: 'ns:multiplestring',
    type: 'STRING',
    multiple: true,
  };
  const emptyMultipleStringField = {
    id: 'ns:emptymultiplestring',
    type: 'STRING',
    multiple: true,
  };
  const testDocumentType = {
    id: 'ns:testdocument',
    fields: [
      stringField,
      multipleStringField,
      emptyMultipleStringField,
    ],
  };
  const testDocument = {
    id: 'test',
    displayName: 'testDoc',
    info: {
      type: {
        id: 'ns:testdocument',
      },
    },
    fields: {
      'ns:string': [
        {
          value: '',
        },
      ],
    },
  };

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_, _$q_, _$rootScope_, _$timeout_, _$translate_) => {
      $componentController = _$componentController_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      $timeout = _$timeout_;
      $translate = _$translate_;
    });

    CreateContentService = jasmine.createSpyObj('CreateContentService', ['getTemplateQuery', 'getDocument', 'createDraft', 'generateDocumentUrlByName']);

    ContentService = jasmine.createSpyObj('ContentService', ['createDraft', 'getDocumentType', 'saveDraft', 'deleteDraft']);
    FeedbackService = jasmine.createSpyObj('FeedbackService', ['showError']);

    CmsService = jasmine.createSpyObj('CmsService', ['closeDocumentWhenValid', 'publish', 'reportUsageStatistic', 'subscribe']);
    DialogService = jasmine.createSpyObj('DialogService', ['confirm', 'show']);
    HippoIframeService = jasmine.createSpyObj('HippoIframeService', ['reload']);

    SidePanelService = jasmine.createSpyObj('SidePanelService', ['close']);

    dialog = jasmine.createSpyObj('dialog', ['textContent', 'ok', 'cancel']);
    dialog.textContent.and.returnValue(dialog);
    dialog.ok.and.returnValue(dialog);
    dialog.cancel.and.returnValue(dialog);
    DialogService.confirm.and.returnValue(dialog);

    spyOn($translate, 'instant').and.callThrough();

    $scope = $rootScope.$new();
    const $element = angular.element('<div></div>');
    $ctrl = $componentController('hippoCreateContentStep2', {
      $scope,
      $element,
      $timeout,
      SidePanelService,
      CmsService,
      ContentService,
      DialogService,
      HippoIframeService,
      FeedbackService,
      CreateContentService,
    });
    $ctrl.form = jasmine.createSpyObj('form', ['$setPristine']);
    $ctrl.onFullWidth = () => true;
    $ctrl.onBeforeStateChange = () => $q.resolve();
    $rootScope.$apply();
  });

  it('subscribes to the kill-editor event', () => {
    spyOn($ctrl, 'close');
    expect(CmsService.subscribe).toHaveBeenCalled();
    const onKillEditor = CmsService.subscribe.calls.mostRecent().args[1];

    $ctrl.documentId = 'documentId';

    onKillEditor('differentId');
    expect($ctrl.close).not.toHaveBeenCalled();

    onKillEditor('documentId');
    expect($ctrl.close).toHaveBeenCalled();
  });

  it('should call parent "on full width" mode on and off', () => {
    spyOn($ctrl, 'onFullWidth');
    $ctrl.setFullWidth(true);
    expect($ctrl.isFullWidth).toBe(true);
    expect($ctrl.onFullWidth).toHaveBeenCalledWith({ state: true });

    $ctrl.setFullWidth(false);
    expect($ctrl.isFullWidth).toBe(false);
    expect($ctrl.onFullWidth).toHaveBeenCalledWith({ state: false });
  });

  it('should detect ESC keypress', () => {
    const e = angular.element.Event('keydown');
    e.which = 27;

    spyOn($ctrl, 'close');
    $ctrl.$element.trigger(e);
    expect($ctrl.close).toHaveBeenCalled();
  });

  it('onInit, loads the document from the createContentService', () => {
    spyOn($ctrl,'loadNewDocument');

    $ctrl.$onInit();
    expect($ctrl.loadNewDocument).toHaveBeenCalled();
  });

  describe('opening a document', () => {
    beforeEach(() => {
      ContentService.getDocumentType.and.returnValue($q.resolve(testDocumentType));
      CreateContentService.getDocument.and.returnValue(testDocument);
    });

    it('gets the newly created draft document from create content service', () => {
      $ctrl.loadNewDocument();
      expect(CreateContentService.getDocument).toHaveBeenCalled();
      expect(ContentService.getDocumentType).toHaveBeenCalledWith('ns:testdocument');
    });

    it('gets the newly created draft document from create content service', () => {
      spyOn($ctrl, '_onLoadSuccess');
      $ctrl.loadNewDocument();
      $rootScope.$apply();
      expect($ctrl._onLoadSuccess).toHaveBeenCalledWith(testDocument, testDocumentType);
      expect($ctrl.loading).not.toBeDefined();
    });
  });

  describe('closing the panel', () => {
    beforeEach(() => {
      ContentService.getDocumentType.and.returnValue($q.resolve(testDocumentType));
      CreateContentService.getDocument.and.returnValue(testDocument);
      DialogService.confirm.and.callThrough();

      $ctrl.$onInit();
      $rootScope.$digest();
    });

    it('Calls discardAndClose method to confirm document discard and close the panel', () => {
      spyOn($ctrl, '_discardAndClose').and.returnValue($q.resolve());

      $ctrl.close();
      expect($ctrl._discardAndClose).toHaveBeenCalled();
    });

    it('Discards the document when "discard" is selected', () => {
      spyOn($ctrl, '_confirmDiscardChanges').and.returnValue($q.resolve());

      $ctrl.close();
      $rootScope.$digest();

      expect($ctrl.doc).toBeUndefined();
      expect($ctrl.documentId).toBeUndefined();
      expect($ctrl.docType).toBeUndefined();
      expect($ctrl.editing).toBeUndefined();
      expect($ctrl.feedback).toBeUndefined();
      expect($ctrl.title).toBe($ctrl.defaultTitle);
      expect($ctrl.form.$setPristine).toHaveBeenCalled();
    });

    it('Will not discard the document when cancel is clicked', () => {
      spyOn($ctrl, '_confirmDiscardChanges').and.returnValue($q.reject());

      $ctrl.close();
      $rootScope.$digest();

      expect($ctrl.doc).not.toBeUndefined();
      expect($ctrl.documentId).not.toBeUndefined();
      expect($ctrl.docType).not.toBeUndefined();
      expect($ctrl.editing).not.toBeUndefined();
      expect($ctrl.title).not.toBe($ctrl.defaultTitle);
      expect($ctrl.form.$setPristine).not.toHaveBeenCalled();
    });
  });

  describe('changing name or URL of the document', () => {
    beforeEach(() => {
      spyOn($ctrl, '_openEditNameUrlDialog');
      ContentService.getDocumentType.and.returnValue($q.resolve(testDocumentType));
      CreateContentService.getDocument.and.returnValue(testDocument);
      DialogService.confirm.and.callThrough();

      $ctrl.$onInit();
      $rootScope.$digest();
    });
    it('open a change url-name dialog', () => {
      $ctrl._openEditNameUrlDialog.and.returnValue($q.resolve());
      $ctrl.editNameUrl();

      expect($ctrl._openEditNameUrlDialog).toHaveBeenCalled();
    });

    it('changes document title if the change is submitted in dialog', () => {
      spyOn($ctrl, '_submitEditNameUrl');
      $ctrl._openEditNameUrlDialog.and.returnValue($q.resolve({ name: 'docName', url: 'doc-url' }));
      $ctrl.editNameUrl();
      $rootScope.$apply();

      expect($ctrl._submitEditNameUrl).toHaveBeenCalledWith({ name: 'docName', url: 'doc-url' });
    });

    it('takes no action if user clicks cancel on the dialog', () => {
      spyOn($ctrl, '_submitEditNameUrl');
      $ctrl._openEditNameUrlDialog.and.returnValue($q.reject());
      $ctrl.editNameUrl();

      expect($ctrl._submitEditNameUrl).not.toHaveBeenCalled();
    });
  });

  it('_openEditNameUrlDialog method open a dialog with the correct details', () => {
    ContentService.getDocumentType.and.returnValue($q.resolve(testDocumentType));
    CreateContentService.getDocument.and.returnValue(testDocument);
    $ctrl.$onInit();
    $rootScope.$apply();

    $ctrl._openEditNameUrlDialog();

    const dialogArguments = DialogService.show.calls.mostRecent().args[0];

    expect(dialogArguments.locals.title).toBe('CHANGE_DOCUMENT_NAME');
    expect(dialogArguments.locals.name).toBe('testDoc');
  });

  it('show correct dialog to change name or URL of the document', () => {});

  it('knows the document is dirty when the backend says so', () => {
    $ctrl.doc = {
      info: {
        dirty: true,
      },
    };
    expect($ctrl.isDocumentDirty()).toBe(true);
  });

  it('knows the document is dirty when the form is dirty', () => {
    $ctrl.form.$dirty = true;
    expect($ctrl.isDocumentDirty()).toBe(true);
  });

  it('knows the document is dirty when both the backend says so and the form is dirty', () => {
    $ctrl.doc = {
      info: {
        dirty: true,
      },
    };
    $ctrl.form.$dirty = true;
    expect($ctrl.isDocumentDirty()).toBe(true);
  });
});
