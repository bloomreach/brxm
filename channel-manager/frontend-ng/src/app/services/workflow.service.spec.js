/*
 * Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

describe('WorkflowService', () => {
  let $httpBackend;
  let $rootScope;
  let ConfigService;
  let WorkflowService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      _$httpBackend_,
      _$rootScope_,
      _ConfigService_,
      _WorkflowService_,
    ) => {
      $httpBackend = _$httpBackend_;
      $rootScope = _$rootScope_;
      ConfigService = _ConfigService_;
      WorkflowService = _WorkflowService_;
    });

    spyOn(ConfigService, 'getCmsContextPath').and.returnValue('/test/');
  });

  afterEach(() => {
    $httpBackend.verifyNoOutstandingRequest();
    $httpBackend.verifyNoOutstandingExpectation();
  });

  it('creates a new workflow action', () => {
    $httpBackend.expectPOST('/test/ws/content/workflows/documents/documentId/workflowActionId').respond(200);

    WorkflowService.createWorkflowAction('documentId', {}, 'workflowActionId');
    $httpBackend.flush();
  });

  it('encodes the documentId and workflowActionId arguments', () => {
    $httpBackend.expectPOST('/test/ws/content/workflows/documents/document%20Id/workflow%20Action%20Id').respond(200);

    WorkflowService.createWorkflowAction('document Id', {}, 'workflow Action Id');
    $httpBackend.flush();
  });

  it('emits a "page:check-changes" event after a workflow action is resolved', () => {
    const listener = jasmine.createSpy('page-check-changes');
    $rootScope.$on('page:check-changes', listener);
    $httpBackend.expectPOST('/test/ws/content/workflows/documents/documentId/workflowActionId').respond(200);

    WorkflowService.createWorkflowAction('documentId', {}, 'workflowActionId');
    $httpBackend.flush();

    expect(listener).toHaveBeenCalled();
  });
});
