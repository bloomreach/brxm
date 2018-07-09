/*
 * Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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
  let $rootScope;
  let ProjectService;
  let CmsService;

  const projectMock = {
    name: 'testProject',
    id: 1,
  };

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      $componentController,
      _$rootScope_,
      _HippoIframeService_,
      _ProjectService_,
      _CmsService_,
    ) => {
      $ctrl = $componentController('projectToggle', {});
      $rootScope = _$rootScope_;
      ProjectService = _ProjectService_;
      CmsService = _CmsService_;
    });

    ProjectService.selectedProject = projectMock;
  });

  describe('onInit', () => {
    it('should define $ctrl.core with the name', () => {
      $ctrl.$onInit();
      expect($ctrl.core).toBeDefined();
      expect($ctrl.core.name).toEqual('CORE');
    });
  });

  describe('getProjects', () => {
    it('return projects list from projectService', () => {
      ProjectService.projects = [];
      $ctrl.$onInit();
      const projectList = $ctrl.projects;
      expect(projectList).toEqual([]);
    });
  });

  describe('get selectedProject', () => {
    it('returns the selected project if set', () => {
      expect($ctrl.selectedProject).toEqual(projectMock);
    });
  });

  describe('sets selectedProject', () => {
    beforeEach(() => {
      // init to get core project
      $ctrl.$onInit();
      spyOn(ProjectService, 'updateSelectedProject');
      spyOn(CmsService, 'reportUsageStatistic');
    });

    it('should update selected project on project service with the selected project id', () => {
      const projectMock2 = { id: 'test' };
      $ctrl.selectedProject = projectMock2;
      $rootScope.$digest();

      expect(ProjectService.updateSelectedProject).toHaveBeenCalledWith(projectMock2.id);
      expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('CMSChannelsProjectSwitch');
    });

    it('should not update selected project when it did not change', () => {
      ProjectService.selectedProject = projectMock;
      $ctrl.selectedProject = projectMock;
      $rootScope.$digest();

      expect(ProjectService.updateSelectedProject).not.toHaveBeenCalled();
      expect(CmsService.reportUsageStatistic).not.toHaveBeenCalled();
    });
  });
});
