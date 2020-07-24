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

describe('Step1Service', () => {
  let $q;
  let $rootScope;
  let ChannelService;
  let ContentService;
  let FeedbackService;
  let Step1Service;

  function expectError(spyObj, triggerError, errorData, defaultMessage) {
    // backend returns an error with passed data
    spyObj.and.returnValue($q.reject(errorData));
    triggerError();
    $rootScope.$apply();

    // parse and expect key/default-message and optional params
    const key = errorData.data ? `ERROR_${errorData.data.reason}` : defaultMessage;
    const args = [key];
    if (errorData.data && errorData.data.params) {
      args.push(errorData.data.params);
    }
    expect(FeedbackService.showError).toHaveBeenCalledWith(...args);
  }

  function expectTemplateQueryError(triggerError, errorData, defaultMessage) {
    expectError(ContentService.getDocumentTemplateQuery, triggerError, errorData, defaultMessage);
  }

  function expectCreateDocumentError(triggerError, errorData, defaultMessage) {
    expectError(ContentService.createDocument, triggerError, errorData, defaultMessage);
  }

  function expectGetFoldersError(triggerError, errorData, defaultMessage) {
    expectError(ContentService.getFolders, triggerError, errorData, defaultMessage);
  }

  function expectReset() {
    expect(Step1Service.name).toBeUndefined();
    expect(Step1Service.url).toBeUndefined();
    expect(Step1Service.locale).toBeUndefined();
    expect(Step1Service.rootPath).toBeUndefined();
    expect(Step1Service.documentTemplateQuery).toBeUndefined();
    expect(Step1Service.experiencePage).toBeUndefined();
  }

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.createContent.step1');

    ChannelService = jasmine.createSpyObj('ChannelService', ['initialize', 'getChannel']);
    ChannelService.getChannel.and.returnValue({
      contentRoot: '/channel/content',
    });
    ContentService = jasmine.createSpyObj('ContentService', [
      'createDocument',
      'getDocumentTemplateQuery',
      'getFolders',
    ]);
    ContentService.createDocument.and.returnValue(Promise.resolve());
    ContentService.getDocumentTemplateQuery.and.returnValue(Promise.resolve());
    ContentService.getFolders.and.returnValue(Promise.resolve());

    angular.mock.module(($provide) => {
      $provide.value('ChannelService', ChannelService);
      $provide.value('ContentService', ContentService);
    });

    inject((_$q_, _$rootScope_, _ChannelService_, _ContentService_, _FeedbackService_, _Step1Service_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      ChannelService = _ChannelService_;
      ContentService = _ContentService_;
      FeedbackService = _FeedbackService_;
      Step1Service = _Step1Service_;
    });
  });

  it('is created with a clean state', () => {
    expectReset();
  });

  it('is reset when stopped', () => {
    Step1Service.name = 'name';
    Step1Service.url = 'url';
    Step1Service.locale = 'locale';
    Step1Service.rootPath = 'rootPath';
    Step1Service.defaultPath = 'defaultPath';
    Step1Service.documentTemplateQuery = 'documentTemplateQuery';
    Step1Service.experiencePage = true;
    Step1Service.stop();
    expectReset();
  });

  describe('open', () => {
    it('stores the input values', () => {
      Step1Service.open('dcmt-tpl-query', 'fldr-tpl-query', '/root/path', 'default/path');
      expect(Step1Service.defaultPath).toBe('default/path');
      expect(Step1Service.rootPath).toBe('/root/path');
      expect(Step1Service.documentTemplateQuery).toBe('dcmt-tpl-query');
      expect(Step1Service.folderTemplateQuery).toBe('fldr-tpl-query');
      expect(Step1Service.experiencePage).toBe(false);
    });

    it('resets the values before storing new values', () => {
      Step1Service.open('dcmt-tpl-query', 'fldr-tpl-query', '/root/path', 'default/path');
      Step1Service.name = 'name';
      Step1Service.experiencePage = true;

      Step1Service.open('dcmt-tpl-query2', undefined, '/root/path2');
      expect(Step1Service.documentTemplateQuery).toBe('dcmt-tpl-query2');
      expect(Step1Service.folderTemplateQuery).toBeUndefined();
      expect(Step1Service.rootPath).toBe('/root/path2');
      expect(Step1Service.defaultPath).toBeUndefined();
      expect(Step1Service.name).toBeUndefined();
      expect(Step1Service.experiencePage).toBe(false);
    });

    describe('loading document types by document-template-query', () => {
      it('calls the content service to get a document template query', () => {
        Step1Service.open('tpl-query');
        expect(ContentService.getDocumentTemplateQuery).toHaveBeenCalledWith('tpl-query');
      });

      it('stores the document types returned by the content service', () => {
        ContentService.getDocumentTemplateQuery.and.returnValue($q.resolve({
          documentTypes: ['a', 'b'],
        }));
        Step1Service.open('tpl-query');
        $rootScope.$apply();
        expect(Step1Service.documentTypes).toEqual(['a', 'b']);
        expect(Step1Service.documentType).toEqual('');
      });

      it('pre-selects the documentType if there is only one returned by the content service', () => {
        ContentService.getDocumentTemplateQuery.and.returnValue($q.resolve({
          documentTypes: [{
            id: 'a',
          }],
        }));
        Step1Service.open('tpl-query');
        $rootScope.$apply();
        expect(Step1Service.documentTypes).toEqual([{
          id: 'a',
        }]);
        expect(Step1Service.documentType).toEqual('a');
      });

      it('handles template query errors', () => {
        spyOn(FeedbackService, 'showError');
        expectTemplateQueryError(() => Step1Service
          .open('tpl-query'), {}, 'Unexpected error loading template query "tpl-query"');
        expectTemplateQueryError(() => Step1Service
          .open('tpl-query'), { data: { reason: 'the_cause' } }, 'ERROR_the_cause');
      });
    });

    describe('parsing the rootPath', () => {
      it('overrides the channel root path if absolute', () => {
        Step1Service.open('tpl-query', undefined, '/root/path');
        expect(Step1Service.rootPath).toBe('/root/path');
      });

      it('is concatenated wth the channel\'s root path if relative', () => {
        Step1Service.open('tpl-query', undefined, 'some/path');
        expect(Step1Service.rootPath).toBe('/channel/content/some/path');
      });

      it('never ends with a slash', () => {
        Step1Service.open('tpl-query', undefined, '/root/path/');
        expect(Step1Service.rootPath).toBe('/root/path');

        Step1Service.open('tpl-query', undefined, 'some/path/');
        expect(Step1Service.rootPath).toBe('/channel/content/some/path');
      });
    });
  });

  describe('createDocument', () => {
    it('calls the content service to create a document', (done) => {
      Step1Service.name = 'test-name';
      Step1Service.url = 'test-url';
      Step1Service.documentTemplateQuery = 'test-tpl-query';
      Step1Service.folderTemplateQuery = 'test-fldr-tpl-query';
      Step1Service.documentType = 'test-doctype';
      Step1Service.rootPath = 'test-rootpath';
      Step1Service.defaultPath = 'test-defaultpath';
      Step1Service.experiencePage = false;

      Step1Service.createDocument().then(done);
      expect(ContentService.createDocument).toHaveBeenCalledWith({
        name: 'test-name',
        slug: 'test-url',
        documentTemplateQuery: 'test-tpl-query',
        folderTemplateQuery: 'test-fldr-tpl-query',
        documentTypeId: 'test-doctype',
        rootPath: 'test-rootpath',
        defaultPath: 'test-defaultpath',
        experiencePage: false,
      });
    });

    it('handles create-document backend errors', () => {
      spyOn(FeedbackService, 'showError');
      expectCreateDocumentError(() => Step1Service.createDocument(), {}, 'Unexpected error creating a new document');
      expectCreateDocumentError(() => Step1Service.createDocument(), {
        data: {
          reason: 'the_cause',
          params: 'the_params',
        },
      });
    });
  });

  describe('getFolders', () => {
    it('calls the content service to get folders', () => {
      Step1Service.getFolders('aa/bb');
      expect(ContentService.getFolders).toHaveBeenCalledWith('aa/bb');
    });

    it('handles getFolders backend errors', () => {
      spyOn(FeedbackService, 'showError');
      expectGetFoldersError(() => Step1Service
        .getFolders('aa/bb'), {}, 'Unexpected error loading folders for path aa/bb');
      expectGetFoldersError(() => Step1Service
        .getFolders('aa/bb'), { data: { reason: 'the_cause' } }, 'ERROR_the_cause');
    });
  });
});
