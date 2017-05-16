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
  let HstService;

  const mountId = '12';

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    const configServiceMock = jasmine.createSpyObj('ConfigService', ['getCmsContextPath']);
    configServiceMock.getCmsContextPath.and.returnValue('/test/');

    angular.mock.module(($provide) => {
      $provide.value('ConfigService', configServiceMock);
    });

    const channelServiceMock = jasmine.createSpyObj('ChannelService', ['getChannel', 'initialize']);
    channelServiceMock.getChannel.and.returnValue({ mountId });

    angular.mock.module(($provide) => {
      $provide.value('ChannelService', channelServiceMock);
    });

    inject((_$httpBackend_, _ProjectService_, _HstService_) => {
      $httpBackend = _$httpBackend_;
      ProjectService = _ProjectService_;
      HstService = _HstService_;
    });

    spyOn(HstService, 'doPut');
  });

  afterEach(() => {
    $httpBackend.verifyNoOutstandingRequest();
    $httpBackend.verifyNoOutstandingExpectation();
  });

  it('can get projects', () => {
    const withBranch = [
      {
        id: 'test1',
        name: 'test1',
      },
      {
        id: 'test2',
        name: 'test2',
      }];
    const withoutBranch = [
      {
        id: 'test3',
        name: 'test3',
      },
    ];
    const returnFromRest = { withBranch, withoutBranch };
    let actual = null;

    $httpBackend.expectGET(`/test/ws/projects/${mountId}/channel`).respond(200, returnFromRest);

    ProjectService._getProjects(12).then((returned) => {
      actual = returned;
    });
    $httpBackend.flush();

    expect(actual).toEqual(returnFromRest);
  });

  it('selects master if the selectedProject changes to master', () => {
    const master = {
      id: 'master',
      name: 'master',
    };
    ProjectService._master = master;
    ProjectService._mountId = mountId;
    ProjectService.projectChanged(master);
    expect(HstService.doPut).toHaveBeenCalledWith(null, mountId, 'selectmaster');
  });

  it('selects the branch associated with the selectedProject if the selectedProject has already been associated', () => {
    const master = {
      id: 'master',
      name: 'master',
    };
    const test1 = {
      id: 'test1',
      name: 'test1',
    };
    const withBranch = [
      test1,
      {
        id: 'test2',
        name: 'test2',
      }];
    ProjectService.withBranch = withBranch;
    ProjectService.withoutBranch = [];
    ProjectService._master = master;
    ProjectService._mountId = mountId;
    ProjectService.projectChanged(test1);
    expect(HstService.doPut).toHaveBeenCalledWith(null, mountId, 'selectbranch', test1.id);
  });

  it('creates a branch based on the selectedProject if the selectedProject has not been yet associated', () => {
    const master = {
      id: 'master',
      name: 'master',
    };
    const test1 = {
      id: 'test1',
      name: 'test1',
    };
    const withoutBranch = [
      test1,
      {
        id: 'test2',
        name: 'test2',
      }];
    ProjectService.withBranch = [];
    ProjectService.withoutBranch = withoutBranch;
    ProjectService._master = master;
    ProjectService._mountId = mountId;
    ProjectService.projectChanged(test1);
    expect(HstService.doPut).toHaveBeenCalledWith(null, mountId, 'createbranch', test1.id);
  });
});
