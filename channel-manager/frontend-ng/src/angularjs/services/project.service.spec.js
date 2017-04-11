/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
import angular from 'angular';
import 'angular-mocks';

describe('ProjectService', () => {
  let $httpBackend;
  let ProjectService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    const configServiceMock = jasmine.createSpyObj('ConfigService', ['getCmsContextPath']);
    configServiceMock.getCmsContextPath.and.returnValue('/test');

    angular.mock.module(($provide) => {
      $provide.value('ConfigService', configServiceMock);
    });

    inject((_$httpBackend_, _ProjectService_) => {
      $httpBackend = _$httpBackend_;
      ProjectService = _ProjectService_;
    });
  });

  afterEach(() => {
    $httpBackend.verifyNoOutstandingRequest();
    $httpBackend.verifyNoOutstandingExpectation();
  });

  it('can get projects', () => {
    const returnFromRest = [{
      withBranches: [
        {
          id: 'test1',
          name: 'test1',
        },
        {
          id: 'test2',
          name: 'test2',
        }],
      withoutBranches: [
        {
          id: 'test3',
          name: 'test3',
        },
      ] },
    ];
    let actual = null;
    const expected = [
      {
        id: 'test1',
        name: 'test1',
        isBranch: true,
      },
      {
        id: 'test2',
        name: 'test2',
        isBranch: true,
      },
      {
        id: 'test3',
        name: 'test3',
        isBranch: false,
      },
      ProjectService.master,
    ];

    $httpBackend.expectGET('/test/ws/projects/12/channel').respond(200, returnFromRest);

    ProjectService.projects(12).then((returned) => {
      actual = returned;
    });
    $httpBackend.flush();

    expect(actual).toEqual(expected);
  });

  it('can detect an absent REST url', () => {
    $httpBackend.expectGET('/test/ws/projects').respond(404);

    ProjectService.doInitializeIsAvailable();

    expect(ProjectService.available).toBe(false);
  });
});
