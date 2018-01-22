/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

fdescribe('Create content step 1 component', () => {
  let $rootScope;
  let $q;
  let CreateContentService;
  let FeedbackService;

  let $ctrl;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.createContentModule');

    inject((
      _$controller_,
      _$rootScope_,
      _$q_,
      _CreateContentService_,
      _FeedbackService_,
    ) => {
      $rootScope = _$rootScope_;
      $q = _$q_;
      CreateContentService = _CreateContentService_;
      FeedbackService = _FeedbackService_;

      $ctrl = $controller('step1Ctrl');
    });
  });

  describe('DocumentType', () => {
    it('throws an error if options are not set', () => {
      expect(() => {
        $ctrl.options = null;
        $ctrl.$onInit();
      }).toThrow();
    });

    it('throws an error if templateQuery is not set', () => {
      expect(() => {
        $ctrl.options = { templatQuery: null };
        $ctrl.$onInit();
      }).toThrow();
    });

    it('loads documentLocation properties from options object', () => {
      $ctrl.options = {
        templateQuery: 'test-templateQuery',
        rootPath: 'test-rootPath',
        defaultPath: 'test-defaultPath',
      };
      $ctrl.$onInit();
      expect($ctrl.documentLocationField.defaultPath).toBe('test-defaultPath');
      expect($ctrl.documentLocationField.rootPath).toBe('test-rootPath');
    });

    it('loads documentTypes from the templateQuery', () => {
      const documentTypes = [
        { id: 'test-id1', displayName: 'test-name 1' },
        { id: 'test-id2', displayName: 'test-name 2' },
      ];

      const spy = spyOn(CreateContentService, 'getTemplateQuery')
        .and.returnValue($q.resolve({ documentTypes }));

      $ctrl.options = { templateQuery: 'test-template-query' };
      $ctrl.$onInit();
      $rootScope.$apply();

      expect(spy).toHaveBeenCalledWith('test-template-query');
      expect($ctrl.documentType).toBeNull();
      expect($ctrl.documentTypes).toBe(documentTypes);
    });

    it('pre-selects the documentType if only one is returned from the templateQuery', () => {
      const documentTypes = [{ id: 'test-id1', displayName: 'test-name 1' }];
      spyOn(CreateContentService, 'getTemplateQuery')
        .and.returnValue($q.resolve({ documentTypes }));

      $ctrl.options = { templateQuery: 'test-template-query' };
      $ctrl.$onInit();
      $rootScope.$apply();

      expect($ctrl.documentType).toBe('test-id1');
    });

    it('sends feedback as error when server returns 500', () => {
      const feedbackSpy = spyOn(FeedbackService, 'showError');
      spyOn(CreateContentService, 'getTemplateQuery')
        .and.returnValue($q.reject({
          status: 500,
          data: {
            reason: 'INVALID_TEMPLATE_QUERY',
            params: {
              templateQuery: 'new-document',
            },
          },
        }));

      $ctrl.options = { templateQuery: 'test-template-query' };
      $ctrl.$onInit();
      $rootScope.$apply();

      expect(feedbackSpy).toHaveBeenCalledWith('ERROR_INVALID_TEMPLATE_QUERY', { templateQuery: 'new-document' });
    });
  });

  describe('Creating a new draft', () => {
    beforeEach(() => {
      // Mock templateQuery calls that gets executed on "onInit"
      // Disabling this will fail the tests
      $ctrl.options = {
        templateQuery: 'test-template-query',
        rootPath: '/content/documents/hap/news',
        defaultPath: '2017/12',
      };
      const documentTypes = [
        { id: 'test-id1', displayName: 'test-name 1' },
      ];
      spyOn(CreateContentService, 'getTemplateQuery')
        .and.returnValue($q.resolve({ documentTypes }));
    });

    it('assembles document object and sends it to the server', () => {
      $ctrl.$onInit();

      $ctrl.nameUrlFields.nameField = 'New doc';
      $ctrl.nameUrlFields.urlField = 'new-doc';
      $ctrl.documentType = 'hap:contentdocument';
      $ctrl.documentLocationField.rootPath = '/content/documents/hap/news';
      $ctrl.documentLocationField.defaultPath = '2017/12';

      const data = {
        name: 'New doc',
        slug: 'new-doc',
        templateQuery: 'test-template-query',
        documentTypeId: 'hap:contentdocument',
        rootPath: '/content/documents/hap/news',
        defaultPath: '2017/12',
      };

      const spy = spyOn(CreateContentService, 'createDraft')
        .and.returnValue($q.resolve('resolved'));

      $ctrl.submit();
      $rootScope.$apply();

      expect(spy).toHaveBeenCalledWith(data);
    });

    it('sends feedback as error when server returns 500', () => {
      const feedbackSpy = spyOn(FeedbackService, 'showError');
      spyOn(CreateContentService, 'createDraft')
        .and.returnValue($q.reject({
          status: 500,
          data: {
            reason: 'INVALID_DOCUMENT_DETAILS',
          },
        }));

      $ctrl.submit();
      $rootScope.$apply();

      expect(feedbackSpy).toHaveBeenCalledWith('ERROR_INVALID_DOCUMENT_DETAILS');
    });
  });
});
