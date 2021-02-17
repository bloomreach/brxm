/*
 * Copyright 2015-2021 Hippo B.V. (http://www.onehippo.com)
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

describe('projectToggle component', () => {
  let $ctrl;
  let $q;
  let $rootScope;
  let ProjectService;
  let CmsService;
  let ChannelService;

  const projectMock = {
    name: 'testProject',
    id: 1,
    channels: [],
  };

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    ChannelService = jasmine.createSpyObj('ChannelService', [
      'initializeChannel',
      'getBaseId',
    ]);

    ChannelService.channel = {
      id: 'channel-id',
      contextPath: 'something',
      hostGroup: 'something',
    };

    inject((
      $componentController,
      _$q_,
      _$rootScope_,
      _HippoIframeService_,
      _ProjectService_,
      _CmsService_,
    ) => {
      $ctrl = $componentController('projectToggle', { ChannelService });
      $q = _$q_;
      $rootScope = _$rootScope_;
      ProjectService = _ProjectService_;
      CmsService = _CmsService_;
    });

    ProjectService.selectedProject = projectMock;
  });

  describe('get projects', () => {
    it('return projects list from projectService', () => {
      const projects = [
        { id: 'test1' },
        { id: 'test1' },
      ];

      ProjectService.projects = projects;
      expect($ctrl.projects).toEqual(projects);
    });
  });

  describe('get selectedProject', () => {
    it('returns the selected project if set', () => {
      expect($ctrl.selectedProject).toEqual(projectMock);
    });
  });

  describe('sets selectedProject', () => {
    beforeEach(() => {
      spyOn(ProjectService, 'updateSelectedProject');
      spyOn(CmsService, 'reportUsageStatistic');
    });

    it('should update selected project on project service with the selected project id', () => {
      const projectMock2 = {
        id: 'test',
        channels: [
          {
            id: 'channel-id-test',
            branchOf: 'channel-id',
          },
        ],
      };
      ChannelService.getBaseId.and.returnValue('channel-id');
      ProjectService.updateSelectedProject.and.returnValue($q.resolve());
      $ctrl.selectedProject = projectMock2;
      $rootScope.$digest();

      expect(ChannelService.initializeChannel).toHaveBeenCalledWith(
        'channel-id-test',
        jasmine.anything(),
        jasmine.anything(),
        'test',
      );
      expect(ProjectService.updateSelectedProject).toHaveBeenCalledWith(projectMock2.id);
      expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('CMSChannelsProjectSwitch');
    });

    it('should not publish a usage statistic event when updating the selected project failed', () => {
      ProjectService.selectedProject = projectMock;
      ProjectService.updateSelectedProject.and.returnValue($q.reject());
      $ctrl.selectedProject = projectMock;
      $rootScope.$digest();

      expect(ProjectService.updateSelectedProject).toHaveBeenCalled();
      expect(CmsService.reportUsageStatistic).not.toHaveBeenCalled();
    });
  });
});
