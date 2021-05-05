/*!
 * Copyright 2021 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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

import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { DocumentWorkflowService } from './document-workflow.service';
import { Ng1ConfigService, NG1_CONFIG_SERVICE } from './ng1/config.ng1.service';
import { NG1_ROOT_SCOPE } from './ng1/root-scope.ng1.service';

describe('DocumentWorkflowService', () => {
  let service: DocumentWorkflowService;
  let httpTestingController: HttpTestingController;
  let $rootScope: ng.IRootScopeService;

  beforeEach(() => {
    const $rootScopeMock: Partial<ng.IRootScopeService> = {
      $emit: jest.fn(),
    };

    const configServiceMock: Partial<Ng1ConfigService> = {
      getCmsContextPath: jest.fn(() => '/test/'),
    };

    TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
      ],
      providers: [
        { provide: NG1_ROOT_SCOPE, useValue: $rootScopeMock },
        { provide: NG1_CONFIG_SERVICE, useValue: configServiceMock },
      ],
    });

    service = TestBed.inject(DocumentWorkflowService);
    httpTestingController = TestBed.inject(HttpTestingController);
    $rootScope = TestBed.inject(NG1_ROOT_SCOPE);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('creates a new workflow action', () => {
    service.postAction('documentId', ['workflowActionId']);

    const req = httpTestingController.expectOne('/test/ws/content/workflows/documents/documentId/workflowActionId');
    req.flush(null);
  });

  it('encodes the documentId and workflowActionId arguments', () => {
    service.postAction('document Id', [ 'workflow Action Id' ]);

    const req = httpTestingController.expectOne('/test/ws/content/workflows/documents/document%20Id/workflow%20Action%20Id');
    req.flush(null);
  });

  it('emits a "page:check-changes" event after a workflow action is resolved', () => {
    service.postAction('documentId', ['workflowActionId'])
      .then(() => {
        expect($rootScope.$emit).toHaveBeenCalled();
      });

    const req = httpTestingController.expectOne('/test/ws/content/workflows/documents/documentId/workflowActionId');
    req.flush(null);
  });
});
