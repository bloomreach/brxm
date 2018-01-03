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
  let $componentController;
  let $rootScope;
  let $q;
  let CreateContentService;
  let FeedbackService;
  let ContentService;
  let DialogService;

  let component;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.createContentModule');

    inject((
      _$componentController_,
      _$rootScope_,
      _$q_,
      _CreateContentService_,
      _FeedbackService_,
      _ContentService_,
      _DialogService_,
    ) => {
      $componentController = _$componentController_;
      $rootScope = _$rootScope_;
      $q = _$q_;
      CreateContentService = _CreateContentService_;
      FeedbackService = _FeedbackService_;
      ContentService = _ContentService_;
      DialogService = _DialogService_;
    });

    component = $componentController('createContentStep2');

    component.options = {
      name: testDocument.displayName,
      url: 'test-document',
      locale: 'en',
    };

    spyOn(ContentService, 'getDocumentType').and.callThrough();
    spyOn(CreateContentService, 'getDocument').and.returnValue(testDocument);
    spyOn(DialogService, 'confirm').and.callThrough();

    component.onFullWidth = () => {};
    component.onSave = () => {};
    component.onBeforeStateChange = obj => obj.callback();
  });

  describe('$onInit', () => {
    it('loads the document from CreateContentService', () => {
      spyOn(component, 'loadNewDocument').and.callThrough();
      spyOn(component, 'discardAndClose');
      spyOn(component, 'onBeforeStateChange').and.callThrough();

      component.$onInit();
      expect(component.loadNewDocument).toHaveBeenCalled();
      expect(component.onBeforeStateChange).toHaveBeenCalled();
      expect(component.discardAndClose).toHaveBeenCalled();
    });
  });

  describe('setWidthState', () => {
    it('toggles parent "onFullWidth" mode on and off', () => {
      spyOn(component, 'onFullWidth');
      component.setWidthState(true);
      expect(component.isFullWidth).toBe(true);
      expect(component.onFullWidth).toHaveBeenCalledWith({ state: true });

      component.setWidthState(false);
      expect(component.isFullWidth).toBe(false);
      expect(component.onFullWidth).toHaveBeenCalledWith({ state: false });
    });
  });

  describe('loadNewDocument', () => {
    it('gets the newly created draft document from create content service', () => {
      component.loadNewDocument();
      expect(CreateContentService.getDocument).toHaveBeenCalled();
      expect(ContentService.getDocumentType).toHaveBeenCalledWith('ns:testdocument');
    });

    it('gets the newly created draft document from create content service', () => {
      component.loadNewDocument().then(() => {
        expect(component.doc).toEqual(testDocument);
        expect(component.docType).toEqual(testDocumentType);
        expect(component.loading).toEqual(false);
      });
    });
  });

  describe('close', () => {
    beforeEach(() => {
      spyOn(component, 'onClose');
    });
  });

  it('calls the confirmation dialog', () => {
    component.doc = testDocument;
    component.close();
    expect(DialogService.confirm).toHaveBeenCalled();
  });

  it('calls discardAndClose method to confirm document discard and close the panel', () => {
    spyOn(component, 'discardAndClose').and.returnValue($q.resolve());
    component.close();
    expect(component.discardAndClose).toHaveBeenCalled();
  });

  it('discards the document when "discard" is selected', () => {
    component.doc = testDocument;
    spyOn(component, 'onBeforeStateChange').and.callThrough();
    spyOn(Promise, 'resolve').and.callThrough();
    component.close().then(() => {
      expect(component.documentId).not.toBeDefined();
      expect(component.doc).not.toBeDefined();
      expect(component.docType).not.toBeDefined();
      expect(component.feedback).not.toBeDefined();
      expect(component.title).toEqual('Create new content');
      expect(component.onBeforeStateChange).toHaveBeenCalled();
      expect(Promise.resolve).toHaveBeenCalled();
      expect(component, 'onClose').toHaveBeenCalled();
    });
  });

  it('does not discard the document when cancel is clicked', () => {
    spyOn(component, 'discardAndClose').and.returnValue(Promise.reject(null));

    component.close().catch(() => {
      expect(component.onClose).not.toHaveBeenCalled();
    });
  });

  describe('onEditNameUrlClose', () => {
    beforeEach(() => {
      component.doc = testDocument;
    });

    it('receives new document name and URL when dialog is submitted', () => {
      spyOn(CreateContentService, 'setDraftNameUrl').and.callThrough();

      expect(component.doc.displayName).toEqual('test document');
      component._onEditNameUrlDialogClose({ name: 'New name', url: 'new-url' }).then(() => {
        expect(CreateContentService.setDraftNameUrl).toHaveBeenCalledWith(component.doc.id, { name: 'New name', url: 'new-url' });
        expect(component.doc.displayName).toEqual('New name');
        expect(component.documentUrl).toEqual('new-url');
      });
    });

    it('calls feedbackService.showError when an error is returned from the back-end', () => {
      spyOn(CreateContentService, 'setDraftNameUrl').and.returnValue($q.reject({
        data: { reason: 'TEST', params: {} },
      }));
      spyOn(FeedbackService, 'showError');

      expect(component.doc.displayName).toEqual('test document');
      component._onEditNameUrlDialogClose({ name: 'New name', url: 'new-url' });
      $rootScope.$apply();
      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_TEST', {});
      expect(component.doc.displayName).toEqual('test document');
    });
  });

  describe('isDocumentDirty', () => {
    it('returns true if document is set to dirty by the backend', () => {
      component.doc = testDocument;
      component.doc.info.dirty = true;
      expect(component.isDocumentDirty()).toBe(true);
    });
  });

  describe('discardAndClose', () => {
    let deleteDraftSpy;

    beforeEach(() => {
      spyOn(FeedbackService, 'showError');
      deleteDraftSpy = spyOn(CreateContentService, 'deleteDraft').and.returnValue($q.resolve());
    });

    it('deletes the draft after confirming the discard dialog', (done) => {
      spyOn(component, '_confirmDiscardChanges').and.returnValue($q.resolve());
      component.doc = testDocument;
      component.discardAndClose();
      $rootScope.$apply();

      setTimeout(() => {
        expect(component._confirmDiscardChanges).toHaveBeenCalled();
        expect(deleteDraftSpy).toHaveBeenCalled();
        expect(FeedbackService.showError).not.toHaveBeenCalled();
        done();
      });
    });
  });

  describe('saveDocument', () => {
    let saveDraftSpy;

    beforeEach(() => {
      component.doc = testDocument;
      saveDraftSpy = spyOn(ContentService, 'saveDraft');
    });

    it('creates a draft of the current document', () => {
      saveDraftSpy.and.callThrough();
      component.saveDocument();
      expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);
    });

    it('emits the id of the saved document', () => {
      saveDraftSpy.and.returnValue($q.resolve());
      spyOn(component, 'onSave');
      component.saveDocument();
      $rootScope.$apply();
      expect(component.onSave).toHaveBeenCalledWith({ documentId: testDocument.id });
    });

    it('does not trigger a discardAndClose dialog by resetting onBeforeStateChange', fakeAsync(() => {
      saveDraftSpy.and.returnValue($q.resolve());
      spyOn(component, 'onBeforeStateChange');
      component.saveDocument();
      $rootScope.$apply();
      expect(component.onBeforeStateChange).toHaveBeenCalled();
    }));
  });
});
