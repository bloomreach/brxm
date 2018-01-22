/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

const testDocumentType = {
  id: 'ns:testdocument',
  displayName: 'test-name 1',
};

describe('Create content step 2 component', () => {
  let $rootScope;
  let $q;
  let CreateContentService;
  let FeedbackService;
  let ContentService;
  let DialogService;

  let $ctrl;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.createContentModule');

    inject((
      _$controller_,
      _$rootScope_,
      _$q_,
      _CreateContentService_,
      _FeedbackService_,
      _ContentService_,
      _DialogService_,
    ) => {
      $rootScope = _$rootScope_;
      $q = _$q_;
      CreateContentService = _CreateContentService_;
      FeedbackService = _FeedbackService_;
      ContentService = _ContentService_;
      DialogService = _DialogService_;

      $ctrl = $controller('step2Ctrl');
    });


    $ctrl.options = {
      name: testDocument.displayName,
      url: 'test-document',
      locale: 'en',
    };

    spyOn(ContentService, 'getDocumentType').and.callThrough();
    spyOn(CreateContentService, 'getDocument').and.returnValue(testDocument);
    spyOn(DialogService, 'confirm').and.callThrough();

    $ctrl.onFullWidth = () => {};
    $ctrl.onSave = () => {};
    $ctrl.onBeforeStateChange = obj => obj.callback();
  });

  describe('$onInit', () => {
    it('loads the document from CreateContentService', () => {
      spyOn($ctrl, 'loadNewDocument').and.callThrough();
      spyOn($ctrl, 'discardAndClose');
      spyOn($ctrl, 'onBeforeStateChange').and.callThrough();

      $ctrl.$onInit();
      expect($ctrl.loadNewDocument).toHaveBeenCalled();
      expect($ctrl.onBeforeStateChange).toHaveBeenCalled();
      expect($ctrl.discardAndClose).toHaveBeenCalled();
    });
  });

  describe('setWidthState', () => {
    it('toggles parent "onFullWidth" mode on and off', () => {
      spyOn($ctrl, 'onFullWidth');
      $ctrl.setWidthState(true);
      expect($ctrl.isFullWidth).toBe(true);
      expect($ctrl.onFullWidth).toHaveBeenCalledWith({ state: true });

      $ctrl.setWidthState(false);
      expect($ctrl.isFullWidth).toBe(false);
      expect($ctrl.onFullWidth).toHaveBeenCalledWith({ state: false });
    });
  });

  describe('loadNewDocument', () => {
    it('gets the newly created draft document from create content service', () => {
      $ctrl.loadNewDocument();
      expect(CreateContentService.getDocument).toHaveBeenCalled();
      expect(ContentService.getDocumentType).toHaveBeenCalledWith('ns:testdocument');
    });

    it('gets the newly created draft document from create content service', () => {
      $ctrl.loadNewDocument().then(() => {
        expect($ctrl.doc).toEqual(testDocument);
        expect($ctrl.docType).toEqual(testDocumentType);
        expect($ctrl.loading).toEqual(false);
      });
    });
  });

  describe('close', () => {
    beforeEach(() => {
      spyOn($ctrl, 'onClose');
    });
  });

  it('calls the confirmation dialog', () => {
    $ctrl.doc = testDocument;
    $ctrl.close();
    expect(DialogService.confirm).toHaveBeenCalled();
  });

  it('calls discardAndClose method to confirm document discard and close the panel', () => {
    spyOn($ctrl, 'discardAndClose').and.returnValue($q.resolve());
    $ctrl.close();
    expect($ctrl.discardAndClose).toHaveBeenCalled();
  });

  it('discards the document when "discard" is selected', () => {
    $ctrl.doc = testDocument;
    spyOn($ctrl, 'onBeforeStateChange').and.callThrough();
    spyOn(Promise, 'resolve').and.callThrough();
    $ctrl.close().then(() => {
      expect($ctrl.documentId).not.toBeDefined();
      expect($ctrl.doc).not.toBeDefined();
      expect($ctrl.docType).not.toBeDefined();
      expect($ctrl.feedback).not.toBeDefined();
      expect($ctrl.title).toEqual('Create new content');
      expect($ctrl.onBeforeStateChange).toHaveBeenCalled();
      expect(Promise.resolve).toHaveBeenCalled();
      expect($ctrl, 'onClose').toHaveBeenCalled();
    });
  });

  it('does not discard the document when cancel is clicked', () => {
    spyOn($ctrl, 'discardAndClose').and.returnValue(Promise.reject(null));

    $ctrl.close().catch(() => {
      expect($ctrl.onClose).not.toHaveBeenCalled();
    });
  });

  describe('onEditNameUrlClose', () => {
    beforeEach(() => {
      $ctrl.doc = testDocument;
    });

    it('receives new document name and URL when dialog is submitted', () => {
      spyOn(CreateContentService, 'setDraftNameUrl').and.callThrough();

      expect($ctrl.doc.displayName).toEqual('test document');
      $ctrl._onEditNameUrlDialogClose({ name: 'New name', url: 'new-url' }).then(() => {
        expect(CreateContentService.setDraftNameUrl).toHaveBeenCalledWith($ctrl.doc.id, { name: 'New name', url: 'new-url' });
        expect($ctrl.doc.displayName).toEqual('New name');
        expect($ctrl.documentUrl).toEqual('new-url');
      });
    });

    it('calls feedbackService.showError when an error is returned from the back-end', () => {
      spyOn(CreateContentService, 'setDraftNameUrl').and.returnValue($q.reject({
        data: { reason: 'TEST', params: {} },
      }));
      spyOn(FeedbackService, 'showError');

      expect($ctrl.doc.displayName).toEqual('test document');
      $ctrl._onEditNameUrlDialogClose({ name: 'New name', url: 'new-url' });
      $rootScope.$apply();
      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_TEST', {});
      expect($ctrl.doc.displayName).toEqual('test document');
    });
  });

  describe('isDocumentDirty', () => {
    it('returns true if document is set to dirty by the backend', () => {
      $ctrl.doc = testDocument;
      $ctrl.doc.info.dirty = true;
      expect($ctrl.isDocumentDirty()).toBe(true);
    });
  });

  describe('discardAndClose', () => {
    let deleteDraftSpy;

    beforeEach(() => {
      spyOn(FeedbackService, 'showError');
      deleteDraftSpy = spyOn(CreateContentService, 'deleteDraft').and.returnValue($q.resolve());
    });

    it('deletes the draft after confirming the discard dialog', (done) => {
      spyOn($ctrl, '_confirmDiscardChanges').and.returnValue($q.resolve());
      $ctrl.doc = testDocument;
      $ctrl.discardAndClose();
      $rootScope.$apply();

      setTimeout(() => {
        expect($ctrl._confirmDiscardChanges).toHaveBeenCalled();
        expect(deleteDraftSpy).toHaveBeenCalled();
        expect(FeedbackService.showError).not.toHaveBeenCalled();
        done();
      });
    });
  });

  describe('saveDocument', () => {
    let saveDraftSpy;

    beforeEach(() => {
      $ctrl.doc = testDocument;
      saveDraftSpy = spyOn(ContentService, 'saveDraft');
    });

    it('creates a draft of the current document', () => {
      saveDraftSpy.and.callThrough();
      $ctrl.saveDocument();
      expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);
    });

    it('emits the id of the saved document', () => {
      saveDraftSpy.and.returnValue($q.resolve());
      spyOn($ctrl, 'onSave');
      $ctrl.saveDocument();
      $rootScope.$apply();
      expect($ctrl.onSave).toHaveBeenCalledWith({ documentId: testDocument.id });
    });

    it('does not trigger a discardAndClose dialog by resetting onBeforeStateChange', () => {
      saveDraftSpy.and.returnValue($q.resolve());
      spyOn($ctrl, 'onBeforeStateChange');
      $ctrl.saveDocument();
      $rootScope.$apply();
      expect($ctrl.onBeforeStateChange).toHaveBeenCalled();
    });
  });
});
