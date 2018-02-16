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

describe('Step2Service', () => {
  let $q;
  let $rootScope;
  let $translate;
  let CmsService;
  let ContentEditor;
  let ContentService;
  let DialogService;
  let FeedbackService;
  let HstComponentService;
  let Step2Service;

  function expectReset() {
    expect(Step2Service.documentLocale).toBeUndefined();
    expect(Step2Service.documentUrl).toBeUndefined();
  }

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.createContent.step2');

    ContentService = jasmine.createSpyObj('ContentService', ['_send']);
    ContentService._send.and.returnValue(Promise.resolve());

    angular.mock.module(($provide) => {
      $provide.value('ContentService', ContentService);
    });

    inject((
      _$q_,
      _$rootScope_,
      _$translate_,
      _CmsService_,
      _ContentEditor_,
      _DialogService_,
      _FeedbackService_,
      _HstComponentService_,
      _Step2Service_,
    ) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      $translate = _$translate_;
      CmsService = _CmsService_;
      ContentEditor = _ContentEditor_;
      DialogService = _DialogService_;
      FeedbackService = _FeedbackService_;
      HstComponentService = _HstComponentService_;
      Step2Service = _Step2Service_;
    });

    spyOn($translate, 'instant').and.callThrough();
    spyOn(FeedbackService, 'showNotification');
    spyOn(CmsService, 'reportUsageStatistic');

    Step2Service.componentInfo = {
      parameterName: 'componentParameterName',
      id: 'componentId',
      label: 'componentName',
      variant: 'componentVariant',
      parameterBasePath: 'componentParameterBasePath',
    };

    ContentEditor.document = {
      repositoryPath: 'documentRepositoryPath',
    };
  });

  it('is created with a clean state', () => {
    expectReset();
  });

  describe('open', () => {
    it('resets locale and url values', () => {
      Step2Service.documentLocale = 'test-locale';
      Step2Service.documentUrl = 'test-url';
      spyOn(ContentEditor, 'loadDocumentType').and.returnValue($q.reject());
      Step2Service.open({}, 'another-test-url', 'another-test-locale');
      expectReset();
    });

    it('saves locale and url values when documentTypes are loaded', () => {
      const documentType = {};
      spyOn(ContentEditor, 'loadDocumentType').and.returnValue($q.resolve(documentType));

      Step2Service.open({}, 'test-url', 'test-locale');
      $rootScope.$digest();

      expect(Step2Service.documentLocale).toBe('test-locale');
      expect(Step2Service.documentUrl).toBe('test-url');
    });

    it('marks ContentEditor.document as dirty when documentTypes are loaded', () => {
      const documentType = {};
      spyOn(ContentEditor, 'loadDocumentType').and.returnValue($q.resolve(documentType));
      spyOn(ContentEditor, 'markDocumentDirty');

      Step2Service.open();
      $rootScope.$digest();

      expect(ContentEditor.markDocumentDirty).toHaveBeenCalled();
    });

    it('resolves the document type', (done) => {
      const documentType = {};
      spyOn(ContentEditor, 'loadDocumentType').and.returnValue($q.resolve(documentType));
      Step2Service.open().then((docType) => {
        expect(docType).toBe(documentType);
        expect(CmsService.reportUsageStatistic).not.toHaveBeenCalled();
        done();
      });
      $rootScope.$digest();
    });

    it('reports unsupported field types in the document type', (done) => {
      const documentType = {
        unsupportedFieldTypes: ['One', 'Two', 'Three'],
        unsupportedRequiredFieldTypes: ['Two', 'Three'],
      };
      spyOn(ContentEditor, 'loadDocumentType').and.returnValue($q.resolve(documentType));
      Step2Service.open().then((docType) => {
        expect(docType).toBe(documentType);
        expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('CreateContentUnsupportedFields', {
          unsupportedFieldTypes: 'One,Two,Three',
          unsupportedMandatoryFieldTypes: 'Two,Three',
        });
        done();
      });
      $rootScope.$digest();
    });
  });

  describe('openEditNameUrlDialog', () => {
    it('shows the edit-name-url dialog', () => {
      spyOn(DialogService, 'show').and.returnValue($q.reject());
      ContentEditor.document = { displayName: 'test-display-name' };
      Step2Service.documentLocale = 'test-locale';
      Step2Service.documentUrl = 'test-url';

      Step2Service.openEditNameUrlDialog();
      $rootScope.$digest();

      const locals = {
        title: 'CHANGE_DOCUMENT_NAME',
        nameField: 'test-display-name',
        urlField: 'test-url',
        locale: 'test-locale',
      };
      expect(DialogService.show).toHaveBeenCalledWith(jasmine.objectContaining({ locals }));
    });

    it('sets the draft name/url after closing', () => {
      ContentEditor.document = { displayName: 'test-display-name', id: 'test-document-id' };
      const dialogData = { name: 'new-display-name', url: 'new-url' };
      spyOn(DialogService, 'show').and.returnValue($q.resolve(dialogData));
      const responseData = { displayName: dialogData.name, urlName: dialogData.url };
      ContentService._send.and.returnValue($q.resolve(responseData));

      Step2Service.openEditNameUrlDialog();
      $rootScope.$digest();

      expect(ContentEditor.document.displayName).toBe('new-display-name');
      expect(Step2Service.documentUrl).toBe('new-url');
    });

    it('handles errors by showing a feedback message', () => {
      spyOn(FeedbackService, 'showError');
      ContentEditor.document = { id: 'test-document-id' };
      const dialogData = { name: 'name', url: 'url' };
      spyOn(DialogService, 'show').and.returnValue($q.resolve(dialogData));
      const error = {
        data: {
          reason: 'error_reason',
          params: 'error_params',
        },
      };
      ContentService._send.and.returnValue($q.reject(error));

      Step2Service.openEditNameUrlDialog();
      $rootScope.$digest();

      expect($translate.instant).toHaveBeenCalledWith('ERROR_error_reason');
      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_error_reason', 'error_params');
    });
  });

  describe('saveComponentParameter', () => {
    it('resolves without taking action if component does not have a "parameterName"', (done) => {
      spyOn(HstComponentService, 'setPathParameter');
      delete Step2Service.componentInfo.parameterName;

      Step2Service.saveComponentParameter().then(() => {
        expect(HstComponentService.setPathParameter).not.toHaveBeenCalled();
        done();
      });
      $rootScope.$digest();
    });

    it('invokes HstComponentService.setPathParameter to store the component parameter data', () => {
      spyOn(HstComponentService, 'setPathParameter').and.returnValue($q.resolve());

      Step2Service.saveComponentParameter();
      $rootScope.$digest();

      expect(HstComponentService.setPathParameter).toHaveBeenCalledWith(
        'componentId',
        'componentVariant',
        'componentParameterName',
        'documentRepositoryPath',
        'componentParameterBasePath',
      );
    });

    it('shows a notification if path parameter is set', () => {
      spyOn(HstComponentService, 'setPathParameter').and.returnValue($q.resolve());
      Step2Service.saveComponentParameter();
      $rootScope.$digest();

      expect(FeedbackService.showNotification).toHaveBeenCalledWith(
        'NOTIFICATION_DOCUMENT_SELECTED_FOR_COMPONENT',
        { componentName: 'componentName' },
      );
    });

    it('shows an error if path parameter can not be set', () => {
      spyOn(FeedbackService, 'showErrorResponse');
      spyOn(HstComponentService, 'setPathParameter').and.returnValue($q.reject());
      Step2Service.saveComponentParameter();
      $rootScope.$digest();

      expect(FeedbackService.showErrorResponse).toHaveBeenCalledWith(
        undefined,
        'ERROR_DOCUMENT_SELECTED_FOR_COMPONENT',
        jasmine.any(Object),
        { componentName: 'componentName' },
      );
    });
  });

  describe('killEditor', () => {
    beforeEach(() => {
      ContentEditor.documentId = '42';
      spyOn(ContentEditor, 'kill');
    });

    it('kills the content editor for the given document ID', () => {
      const result = Step2Service.killEditor('42');
      expect(result).toBe(true);
      expect(ContentEditor.kill).toHaveBeenCalled();
    });

    it('does not kill the content editor for another document ID', () => {
      const result = Step2Service.killEditor('1');
      expect(result).toBe(false);
      expect(ContentEditor.kill).not.toHaveBeenCalled();
    });
  });
});
