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
  let $q;
  let $httpBackend;
  let ProjectService;
  let HstService;

  const mountId = '12';

  const branches = [
    {
      id: 'test1',
      name: 'test1',
    },
    {
      id: 'test2',
      name: 'test2',
    }];

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

    inject((_$q_, _$httpBackend_, _ProjectService_, _HstService_) => {
      $q = _$q_;
      $httpBackend = _$httpBackend_;
      ProjectService = _ProjectService_;
      HstService = _HstService_;
    });

    spyOn(HstService, 'doGet');
    spyOn(HstService, 'doPut');
  });

  afterEach(() => {
    $httpBackend.verifyNoOutstandingRequest();
    $httpBackend.verifyNoOutstandingExpectation();
  });

  it('loads projects and sets the current branch', () => {
    const currentBranch = branches[0];
    $httpBackend.expectGET(`/test/ws/projects/${mountId}/associated-with-channel`).respond(200, branches);
    HstService.doGet.and.returnValue($q.when({ data: currentBranch.id }));
    ProjectService.load({ mountId });
    $httpBackend.flush();
    expect(ProjectService.projects).toEqual(branches);
    expect(ProjectService.selectedProject).toEqual(currentBranch);
    expect(HstService.doGet).toHaveBeenCalledWith(mountId, 'currentbranch');
  });

  it('calls setmaster if the selectedProject is not a branch', () => {
    const currentBranch = branches[0];
    $httpBackend.expectGET(`/test/ws/projects/${mountId}/associated-with-channel`).respond(200, branches);
    HstService.doGet.and.returnValue($q.when({ data: currentBranch.id }));
    HstService.doPut.and.returnValue($q.when({ data: 'master' }));
    ProjectService.load({ mountId });
    $httpBackend.flush();
    ProjectService.selectProject('master');
    expect(HstService.doPut).toHaveBeenCalledWith(null, mountId, 'selectmaster');
  });

  it('calls setbranch if the selectedProject is a branch', () => {
    const currentBranch = branches[0];
    $httpBackend.expectGET(`/test/ws/projects/${mountId}/associated-with-channel`).respond(200, branches);
    HstService.doGet.and.returnValue($q.when({ data: currentBranch.id }));
    HstService.doPut.and.returnValue($q.when({ data: currentBranch.id }));
    ProjectService.load({ mountId });
    $httpBackend.flush();
    ProjectService.selectProject(branches[1].id);
    expect(HstService.doPut).toHaveBeenCalledWith(null, mountId, 'selectbranch', branches[1].id);
  });
});
