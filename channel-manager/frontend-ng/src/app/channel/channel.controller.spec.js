/*
 * Copyright 2015-2020 Hippo B.V. (http://www.onehippo.com)
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

describe('ChannelController', () => {
  let $ctrl;
  let $q;
  let $state;
  let $timeout;
  let $window;
  let ChannelService;
  let ComponentsService;
  let FeedbackService;
  let HippoIframeService;
  let PageService;
  let PageStructureService;
  let ProjectService;
  let SidePanelService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      $componentController,
      _$q_,
      _$state_,
      _$timeout_,
      _$window_,
      _ChannelService_,
      _CmsService_,
      _FeedbackService_,
      _PageService_,
      _ProjectService_,
    ) => {
      const resolvedPromise = _$q_.when();

      $q = _$q_;
      $state = _$state_;
      $timeout = _$timeout_;
      $window = _$window_;
      ChannelService = _ChannelService_;
      FeedbackService = _FeedbackService_;
      PageService = _PageService_;
      ProjectService = _ProjectService_;

      spyOn(ChannelService, 'clearChannel');
      spyOn(ChannelService, 'hasChannel');
      spyOn(ChannelService, 'isEditable');

      SidePanelService = jasmine.createSpyObj('SidePanelService', [
        'open',
        'isFullScreen',
      ]);

      ComponentsService = jasmine.createSpyObj('ComponentsService', [
        'components',
        'getComponents',
      ]);
      ComponentsService.getComponents.and.returnValue(resolvedPromise);

      HippoIframeService = jasmine.createSpyObj('HippoIframeService', [
        'isPageLoaded',
        'load',
        'reload',
      ]);

      PageStructureService = jasmine.createSpyObj('PageStructureService', [
        'getPage',
      ]);

      $ctrl = $componentController('channel', {
        ChannelService,
        SidePanelService,
        ComponentsService,
        HippoIframeService,
        PageStructureService,
      });
    });

    spyOn(FeedbackService, 'showError');
  });

  it('checks whether the channel is loaded', () => {
    ChannelService.hasChannel.and.returnValue(false);
    expect($ctrl.isChannelLoaded()).toBe(false);

    ChannelService.hasChannel.and.returnValue(true);
    expect($ctrl.isChannelLoaded()).toBe(true);
  });

  it('checks whether the page is loaded', () => {
    HippoIframeService.isPageLoaded.and.returnValue(false);
    expect($ctrl.isPageLoaded()).toBe(false);

    HippoIframeService.isPageLoaded.and.returnValue(true);
    expect($ctrl.isPageLoaded()).toBe(true);
  });

  it('checks whether controls are disabled', () => {
    ChannelService.hasChannel.and.returnValue(false);
    HippoIframeService.isPageLoaded.and.returnValue(false);
    expect($ctrl.isControlsDisabled()).toBe(true);

    ChannelService.hasChannel.and.returnValue(false);
    HippoIframeService.isPageLoaded.and.returnValue(true);
    expect($ctrl.isControlsDisabled()).toBe(true);

    ChannelService.hasChannel.and.returnValue(true);
    HippoIframeService.isPageLoaded.and.returnValue(false);
    expect($ctrl.isControlsDisabled()).toBe(true);

    ChannelService.hasChannel.and.returnValue(true);
    HippoIframeService.isPageLoaded.and.returnValue(true);
    expect($ctrl.isControlsDisabled()).toBe(false);
  });

  it('gets the render variant from the page meta', () => {
    const page = jasmine.createSpyObj('page', ['getMeta']);
    const pageMeta = jasmine.createSpyObj('pageMeta', ['getRenderVariant']);

    PageStructureService.getPage.and.returnValue(page);
    page.getMeta.and.returnValue(pageMeta);
    pageMeta.getRenderVariant.and.returnValue('variant1');

    expect($ctrl.getRenderVariant()).toBe('variant1');
  });

  it('correctly shows and hides subpages', () => {
    expect($ctrl.isSubpageOpen()).toBe(false);
    HippoIframeService.reload.and.returnValue($q.when());

    $ctrl.showSubpage('test');
    expect($ctrl.isSubpageOpen()).toBe(true);

    $ctrl.hideSubpage();
    expect($ctrl.isSubpageOpen()).toBe(false);
    $timeout.flush();

    $ctrl.showSubpage('test');
    $ctrl.onSubpageError('key', { param: 'value' });
    expect($ctrl.isSubpageOpen()).toBe(false);
    expect(FeedbackService.showError).toHaveBeenCalledWith('key', { param: 'value' });

    FeedbackService.showError.calls.reset();
    $ctrl.showSubpage('test');
    $ctrl.onSubpageError();
    expect($ctrl.isSubpageOpen()).toBe(false);
    expect(FeedbackService.showError).not.toHaveBeenCalled();

    $ctrl.showSubpage('test');
    $ctrl.onSubpageSuccess('key', { param: 'value' });
    expect($ctrl.isSubpageOpen()).toBe(false);

    $ctrl.showSubpage('test');
    $ctrl.onSubpageSuccess();
    expect($ctrl.isSubpageOpen()).toBe(false);
  });

  it('opens the menu editor when told so', () => {
    $ctrl.editMenu('testUuid');

    expect($ctrl.menuUuid).toBe('testUuid');
    expect($ctrl.currentSubpage).toBe('site-menu-editor');
  });

  it('should return channel toolbar display status', () => {
    ChannelService.setToolbarDisplayed(true);
    expect($ctrl.isToolbarDisplayed()).toBe(true);
    ChannelService.setToolbarDisplayed(false);
    expect($ctrl.isToolbarDisplayed()).toBe(false);
  });

  it('delegates the side-panel full-screen check to the SidePanelService', () => {
    $ctrl.isSidePanelFullScreen('right');
    expect(SidePanelService.isFullScreen).toHaveBeenCalledWith('right');

    $ctrl.isSidePanelFullScreen('left');
    expect(SidePanelService.isFullScreen).toHaveBeenCalledWith('left');
  });

  describe('reload-page event from ExtJS', () => {
    beforeEach(() => {
      $ctrl.$onInit();
    });

    it('handles the reload-page event from ExtJS when an item is already locked', () => {
      $window.CMS_TO_APP.publish('reload-page', { error: 'ITEM_ALREADY_LOCKED', parameterMap: { lockedBy: 'admin' } });

      expect(FeedbackService.showError).toHaveBeenCalledWith(
        'ERROR_UPDATE_COMPONENT_ITEM_ALREADY_LOCKED',
        { lockedBy: 'admin' },
      );
      expect(HippoIframeService.reload).toHaveBeenCalled();
    });

    it('handles the reload-page event from ExtJS when an item is not found', () => {
      $window.CMS_TO_APP.publish('reload-page', { error: 'ITEM_NOT_FOUND', parameterMap: { component: 'Banner' } });

      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_COMPONENT_DELETED', { component: 'Banner' });
      expect(HippoIframeService.reload).toHaveBeenCalled();
    });

    it('handles the reload-page event from ExtJS when editing a component failed', () => {
      $window.CMS_TO_APP.publish('reload-page', { error: '' });

      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_UPDATE_COMPONENT', undefined);
      expect(HippoIframeService.reload).toHaveBeenCalled();
    });

    it('is unsubscribed when the controller is destroyed', () => {
      $ctrl.$onDestroy();
      $window.CMS_TO_APP.publish('reload-page', { error: '' });
      expect(FeedbackService.showError).not.toHaveBeenCalled();
      expect(HippoIframeService.reload).not.toHaveBeenCalled();
    });
  });

  describe('add new XPage', () => {
    beforeEach(() => {
      PageService.states = {
        channel: {
          xPageLayouts: {
            layout1: 'Layout 1',
            layout2: 'Layout 2',
          },
          xPageTemplateQueries: {
            query1: 'query1-path',
            query2: 'query2-path',
          },
        },
      };
    });

    describe('canAddXPage', () => {
      it('should return true', () => {
        expect($ctrl.canAddXPage()).toBe(true);
      });

      it('should return false', () => {
        delete PageService.states;
        expect($ctrl.canAddXPage()).toBe(false);

        PageService.states = { channel: {} };
        expect($ctrl.canAddXPage()).toBe(false);

        PageService.states.channel.xPageLayouts = {};
        expect($ctrl.canAddXPage()).toBe(false);

        PageService.states.channel.xPageLayouts = { layout1: 'Layout 1' };
        expect($ctrl.canAddXPage()).toBe(false);

        PageService.states.channel.xPageTemplateQueries = {};
        expect($ctrl.canAddXPage()).toBe(false);
      });
    });

    describe('add', () => {
      it('should go to the "create-content" state', () => {
        spyOn($state, 'go');

        $ctrl.add();

        expect($state.go).toHaveBeenCalledWith('hippo-cm.channel.create-content-step-1', {
          config: {
            layouts: [
              { id: 'layout1', displayName: 'Layout 1' },
              { id: 'layout2', displayName: 'Layout 2' },
            ],
            documentTemplateQuery: 'query1',
            rootPath: 'query1-path',
          },
        });
      });
    });
  });

  describe('isConfigurationLocked', () => {
    let containers;
    let page;
    let pageMeta;

    beforeEach(() => {
      spyOn(ChannelService, 'isConfigurationLocked').and.returnValue('valueFromChannelService');
      spyOn(ProjectService, 'isBranch');
      spyOn(ProjectService, 'isInReview');

      containers = [];
      page = jasmine.createSpyObj('page', ['getMeta', 'getContainers']);
      pageMeta = jasmine.createSpyObj('pageMeta', ['isXPage']);

      PageStructureService.getPage.and.returnValue(page);
      page.getMeta.and.returnValue(pageMeta);
      page.getContainers.and.returnValue(containers);
    });

    it('should return value from ChannelService if there is no page', () => {
      PageStructureService.getPage.and.returnValue(null);

      expect($ctrl.isConfigurationLocked()).toBe('valueFromChannelService');
    });

    it('should return value from ChannelService if there is no page meta', () => {
      page.getMeta.and.returnValue(null);

      expect($ctrl.isConfigurationLocked()).toBe('valueFromChannelService');
    });

    it('should return value from ChannelService if not an xpage', () => {
      pageMeta.isXPage.and.returnValue(false);

      expect($ctrl.isConfigurationLocked()).toBe('valueFromChannelService');
    });

    it('should return value from ChannelService if xpage is not part of project branch', () => {
      pageMeta.isXPage.and.returnValue(true);
      ProjectService.isBranch.and.returnValue(false);

      expect($ctrl.isConfigurationLocked()).toBe('valueFromChannelService');
    });

    it('should return value from ChannelService if xpage is part of a project but not in review', () => {
      pageMeta.isXPage.and.returnValue(true);
      ProjectService.isBranch.and.returnValue(true);
      ProjectService.isInReview.and.returnValue(false);

      expect($ctrl.isConfigurationLocked()).toBe('valueFromChannelService');
    });

    it('should return value from ChannelService if xpage is part of project and is in review', () => {
      pageMeta.isXPage.and.returnValue(true);
      ProjectService.isBranch.and.returnValue(true);
      ProjectService.isInReview.and.returnValue(true);

      expect($ctrl.isConfigurationLocked()).toBe('valueFromChannelService');
    });

    it('should return false if xpage is part of project and is rejected', () => {
      pageMeta.isXPage.and.returnValue(true);
      ProjectService.isBranch.and.returnValue(true);
      ProjectService.isInReview.and.returnValue(true);

      containers.push({ isXPageEditable: () => false });
      containers.push({ isXPageEditable: () => true });

      expect($ctrl.isConfigurationLocked()).toBe(false);
    });
  });
});
