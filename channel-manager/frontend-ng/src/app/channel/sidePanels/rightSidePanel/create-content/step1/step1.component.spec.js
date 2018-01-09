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

describe('Create content step 1 component', () => {
  let $componentController;
  let $rootScope;
  let $q;
  let CreateContentService;
  let FeedbackService;

  let component;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.createContentModule');

    inject((
      _$componentController_,
      _$rootScope_,
      _$q_,
      _CreateContentService_,
      _FeedbackService_,
    ) => {
      $componentController = _$componentController_;
      $rootScope = _$rootScope_;
      $q = _$q_;
      CreateContentService = _CreateContentService_;
      FeedbackService = _FeedbackService_;
    });

    component = $componentController('createContentStep1');
    component.onClose = () => {};
    component.onContinue = () => {};
  });

  describe('DocumentType', () => {
    it('throws an error if options are not set', () => {
      expect(() => {
        component.options = null;
        component.$onInit();
      }).toThrow();
    });

    it('throws an error if templateQuery is not set', () => {
      expect(() => {
        component.options = { templatQuery: null };
        component.$onInit();
      }).toThrow();
    });

    it('loads documentTypes from the templateQuery', () => {
      const documentTypes = [
        { id: 'test-id1', displayName: 'test-name 1' },
        { id: 'test-id2', displayName: 'test-name 2' },
      ];

      const spy = spyOn(CreateContentService, 'getTemplateQuery')
        .and.returnValue($q.resolve({ documentTypes }));

      component.options = { templateQuery: 'test-template-query' };
      component.$onInit();
      $rootScope.$apply();

      expect(spy).toHaveBeenCalledWith('test-template-query');
      expect(component.documentType).toBeNull();
      expect(component.documentTypes).toBe(documentTypes);
    });

    it('pre-selects the documentType if only one is returned from the templateQuery', () => {
      const documentTypes = [{ id: 'test-id1', displayName: 'test-name 1' }];
      spyOn(CreateContentService, 'getTemplateQuery')
        .and.returnValue($q.resolve({ documentTypes }));

      component.options = { templateQuery: 'test-template-query' };
      component.$onInit();
      $rootScope.$apply();

      expect(component.documentType).toBe('test-id1');
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

      component.options = { templateQuery: 'test-template-query' };
      component.$onInit();
      $rootScope.$apply();

      expect(feedbackSpy).toHaveBeenCalledWith('ERROR_INVALID_TEMPLATE_QUERY', { templateQuery: 'new-document' });
    });
  });

  describe('Creating a new draft', () => {
    beforeEach(() => {
      // Mock templateQuery calls that gets executed on "onInit"
      // Disabling this will fail the tests
      component.options = {
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
      component.$onInit();

      component.nameUrlFields.nameField = 'New doc';
      component.nameUrlFields.urlField = 'new-doc';
      component.documentType = 'hap:contentdocument';
      component.documentLocationField.rootPath = '/content/documents/hap/news';
      component.documentLocationField.defaultPath = '2017/12';

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

      component.submit();
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

      component.submit();
      $rootScope.$apply();

      expect(feedbackSpy).toHaveBeenCalledWith('ERROR_INVALID_DOCUMENT_DETAILS');
    });
  });
});
