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
  let $translate;
  let $q;
  let ProjectService;
  let CmsService;
  let ChannelActionsService;
  let ChannelService;
  let DialogService;
  let $mdSelect;
  let project;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      $componentController,
      _$rootScope_,
      _$translate_,
      _$q_,
      _HippoIframeService_,
      _ProjectService_,
      _CmsService_,
      _ChannelActionsService_,
      _ChannelService_,
      _DialogService_,
      _$mdSelect_,
    ) => {
      $ctrl = $componentController('projectToggle', {});
      $rootScope = _$rootScope_;
      $translate = _$translate_;
      $q = _$q_;
      ProjectService = _ProjectService_;
      CmsService = _CmsService_;
      ChannelActionsService = _ChannelActionsService_;
      ChannelService = _ChannelService_;
      DialogService = _DialogService_;
      $mdSelect = _$mdSelect_;
    });

    project = { name: 'testProject', id: 1 };
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
      const projectList = $ctrl.getProjects();
      expect(projectList).toEqual([]);
    });
  });

  describe('get selectedProjects', () => {
    it('return core project if there are no selected projects', () => {
      ProjectService.selectedProject = null;
      // activate onInit to get the core set up
      $ctrl.$onInit();
      const expectedCoreProject = { name: 'CORE' };

      expect($ctrl.selectedProject).toEqual(expectedCoreProject);
    });

    it('return the selected project if set', () => {
      ProjectService.selectedProject = project;

      expect($ctrl.selectedProject).toEqual(project);
    });
  });

  describe('set selectedProject', () => {
    beforeEach(() => {
      // init to get core project
      $ctrl.$onInit();
      spyOn($mdSelect, 'hide').and.returnValue($q.resolve());
      spyOn(ChannelActionsService, 'hasAnyChanges').and.returnValue(false);
      spyOn(ProjectService, 'updateSelectedProject');
      spyOn(CmsService, 'reportUsageStatistic');
    });

    it('should call $mdSelect hide', () => {
      $ctrl.selectedProject = project;

      expect($mdSelect.hide).toHaveBeenCalled();
    });

    it('should call update selectedProject on project service with the selected project id', () => {
      $ctrl.selectedProject = project;
      $rootScope.$apply();

      expect(ProjectService.updateSelectedProject).toHaveBeenCalledWith(project.id);
      expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('CMSChannelsProjectSwitch');
    });

    describe('if currently selected channel has unsaved changes', () => {
      beforeEach(() => {
        spyOn(ChannelService, 'discardOwnChanges');
        spyOn(DialogService, 'show').and.callThrough();
        spyOn($translate, 'instant');

        ChannelActionsService.hasAnyChanges.and.returnValue(true);
      });

      it('should show confirmation dialog asking if channel changes should be discarded', () => {
        $ctrl.selectedProject = project;
        $rootScope.$apply();

        expect(DialogService.show).toHaveBeenCalled();
        expect($translate.instant).toHaveBeenCalledWith('CONFIRM_DISCARD_UNSAVED_CHANGES_MESSAGE', {
          documentName: $ctrl.core.name,
        });
      });

      it('User chooses discard: should call update selectedProject on project service with the selected project id', () => {
        DialogService.show.and.returnValue($q.resolve());
        $ctrl.selectedProject = project;
        $rootScope.$apply();

        expect(ProjectService.updateSelectedProject).toHaveBeenCalledWith(project.id);
        expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('CMSChannelsProjectSwitch');
      });

      it('User chooses cancel: should not call any method or do any operation', () => {
        DialogService.show.and.returnValue($q.reject());
        $ctrl.selectedProject = project;
        $rootScope.$apply();

        expect(ProjectService.updateSelectedProject).not.toHaveBeenCalledWith(project.id);
        expect(CmsService.reportUsageStatistic).not.toHaveBeenCalledWith('CMSChannelsProjectSwitch');
      });
    });
  });
});
