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

import angular from 'angular';
import 'angular-mocks';

describe('ContentService', () => {
  let $httpBackend;
  let ContentService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    const configServiceMock = jasmine.createSpyObj('ConfigService', ['getCmsContextPath']);
    configServiceMock.getCmsContextPath.and.returnValue('/test');

    angular.mock.module(($provide) => {
      $provide.value('ConfigService', configServiceMock);
    });

    inject((_$httpBackend_, _ContentService_) => {
      $httpBackend = _$httpBackend_;
      ContentService = _ContentService_;
    });
  });

  afterEach(() => {
    $httpBackend.verifyNoOutstandingRequest();
    $httpBackend.verifyNoOutstandingExpectation();
  });

  it('can create a draft', () => {
    const docInfo = {
      id: '123',
      info: {
        type: {
          id: 'test',
        },
      },
    };
    let result = null;

    $httpBackend.expectPOST('/test/ws/content/documents/123/draft').respond(200, docInfo);
    ContentService.createDraft('123').then((returned) => {
      result = returned;
    });
    $httpBackend.flush();

    expect(result).toEqual(docInfo);
  });

  it('can save a draft', () => {
    const doc = {
      id: '123',
      fields: {
        test: 'value',
      },
    };
    let result = null;

    $httpBackend.expectPUT('/test/ws/content/documents/123/draft').respond(200, doc);
    ContentService.saveDraft(doc).then((saved) => {
      result = saved;
    });
    $httpBackend.flush();

    expect(result).toEqual(doc);
  });

  it('can delete a draft', () => {
    const successCallback = jasmine.createSpy('successCallback');
    const failureCallback = jasmine.createSpy('failureCallback');

    $httpBackend.expectDELETE('/test/ws/content/documents/123/draft').respond(200);
    ContentService.deleteDraft('123').then(successCallback, failureCallback);
    $httpBackend.flush();

    expect(successCallback).toHaveBeenCalled();
    expect(failureCallback).not.toHaveBeenCalled();
  });

  it('fails to delete a draft', () => {
    const successCallback = jasmine.createSpy('successCallback');
    const failureCallback = jasmine.createSpy('failureCallback');

    $httpBackend.expectDELETE('/test/ws/content/documents/123/draft').respond(403);
    ContentService.deleteDraft('123').then(successCallback, failureCallback);
    $httpBackend.flush();

    expect(successCallback).not.toHaveBeenCalled();
    expect(failureCallback).toHaveBeenCalled();
  });

  it('can get a document type', () => {
    const docType = {
      id: 'test',
    };
    let result = null;

    $httpBackend.expectGET('/test/ws/content/documenttypes/test').respond(200, docType);
    ContentService.getDocumentType('test').then((returned) => {
      result = returned;
    });
    $httpBackend.flush();

    expect(result).toEqual(docType);
  });

  it('can draft a field', () => {
    const successCallback = jasmine.createSpy('successCallback');
    const failureCallback = jasmine.createSpy('failureCallback');
    const fieldValue = [{ value: 'bla' }];

    $httpBackend.expectPUT('/test/ws/content/documents/123/draft/somefield', fieldValue).respond(200);
    ContentService.draftField('123', 'somefield', fieldValue).then(successCallback, failureCallback);
    $httpBackend.flush();

    expect(successCallback).toHaveBeenCalled();
    expect(failureCallback).not.toHaveBeenCalled();
  });

  it('can draft a child field', () => {
    const successCallback = jasmine.createSpy('successCallback');
    const failureCallback = jasmine.createSpy('failureCallback');
    const fieldValue = [{ value: 'bla' }];

    $httpBackend.expectPUT('/test/ws/content/documents/123/draft/somefield/childfield', fieldValue).respond(200);
    ContentService.draftField('123', 'somefield/childfield', fieldValue).then(successCallback, failureCallback);
    $httpBackend.flush();

    expect(successCallback).toHaveBeenCalled();
    expect(failureCallback).not.toHaveBeenCalled();
  });

  it('can draft a field with a numbered suffix', () => {
    const successCallback = jasmine.createSpy('successCallback');
    const failureCallback = jasmine.createSpy('failureCallback');
    const fieldValue = [{ value: 'bla' }];

    $httpBackend.expectPUT('/test/ws/content/documents/123/draft/choice%5B2%5D', fieldValue).respond(200);
    ContentService.draftField('123', 'choice[2]', fieldValue).then(successCallback, failureCallback);
    $httpBackend.flush();

    expect(successCallback).toHaveBeenCalled();
    expect(failureCallback).not.toHaveBeenCalled();
  });

  it('fails to draft a field', () => {
    const successCallback = jasmine.createSpy('successCallback');
    const failureCallback = jasmine.createSpy('failureCallback');
    const fieldValue = [{ value: 'bla' }];

    $httpBackend.expectPUT('/test/ws/content/documents/123/draft/somefield', fieldValue).respond(403);
    ContentService.draftField('123', 'somefield', fieldValue).then(successCallback, failureCallback);
    $httpBackend.flush();

    expect(successCallback).not.toHaveBeenCalled();
    expect(failureCallback).toHaveBeenCalled();
  });

  it('retrieves document-types async', () => {
    const docType = { id: 'test' };
    const result = jasmine.createSpy('result');

    $httpBackend.expectGET('/test/ws/content/documenttypes/test').respond(200, docType);
    ContentService.getDocumentType('test').then(result);

    $httpBackend.expectGET('/test/ws/content/documenttypes/test').respond(200, docType);
    ContentService.getDocumentType('test').then(result);
    $httpBackend.flush();

    expect(result).toHaveBeenCalledTimes(2);
  });

  it('sends draft requests synchronously and in order', () => {
    const successCallback = jasmine.createSpy('successCallback');
    const failureCallback = jasmine.createSpy('failureCallback');
    const doc = { id: '123' };
    const fieldValue = [{ value: 'bla' }];

    $httpBackend.when('POST', '/test/ws/content/documents/123/draft').respond(200, 'create');
    $httpBackend.when('PUT', '/test/ws/content/documents/123/draft').respond(200, 'save');
    $httpBackend.when('DELETE', '/test/ws/content/documents/123/draft').respond(200, 'delete');
    $httpBackend.when('PUT', '/test/ws/content/documents/123/draft/fieldA', fieldValue).respond(200, 'updateField');

    ContentService.createDraft(doc.id).then(successCallback, failureCallback);
    ContentService.saveDraft(doc).then(successCallback, failureCallback);
    ContentService.deleteDraft('123').then(successCallback, failureCallback);
    ContentService.draftField('123', 'fieldA', fieldValue).then(successCallback, failureCallback);

    $httpBackend.flush();

    expect(successCallback).toHaveBeenCalledTimes(4);
    expect(failureCallback).not.toHaveBeenCalled();
    expect(successCallback.calls.argsFor(0)).toEqual(['create']);
    expect(successCallback.calls.argsFor(1)).toEqual(['save']);
    expect(successCallback.calls.argsFor(2)).toEqual(['delete']);
    expect(successCallback.calls.argsFor(3)).toEqual(['updateField']);
  });
});
