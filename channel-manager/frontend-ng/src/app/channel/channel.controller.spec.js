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

describe('ChannelCtrl', () => {
  let $q;
  let $rootScope;
  let $timeout;
  let ChannelCtrl;
  let ChannelService;
  let CmsService;
  let SidePanelService;
  let ComponentsService;
  let FeedbackService;
  let HippoIframeService;
  let PageMetaDataService;
  let OverlayService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject(($controller, _$rootScope_, _$timeout_, _$q_, _FeedbackService_, _ChannelService_, _CmsService_, _OverlayService_) => {
      const resolvedPromise = _$q_.when();

      $rootScope = _$rootScope_;
      $timeout = _$timeout_;
      $q = _$q_;
      FeedbackService = _FeedbackService_;
      ChannelService = _ChannelService_;
      CmsService = _CmsService_;
      OverlayService = _OverlayService_;

      const $stateParams = {
        initialRenderPath: '/testPath',
      };

      spyOn(ChannelService, 'clearChannel');
      spyOn(ChannelService, 'hasChannel');
      spyOn(ChannelService, 'isEditable');

      SidePanelService = jasmine.createSpyObj('SidePanelService', [
        'open',
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

      PageMetaDataService = jasmine.createSpyObj('PageMetaDataService', [
        'getRenderVariant',
      ]);

      ChannelCtrl = $controller('ChannelCtrl', {
        $scope: $rootScope.$new(),
        $stateParams,
        ChannelService,
        SidePanelService,
        ComponentsService,
        HippoIframeService,
        PageMetaDataService,
      });
    });

    spyOn(FeedbackService, 'showError');
  });

  describe('initialise overlays', () => {
    it('initially, content overlay is toggled on, component overlay is toggled off', () => {
      expect(ChannelCtrl.isContentOverlayDisplayed).toEqual(true);
      expect(ChannelCtrl.isComponentsOverlayDisplayed).toEqual(false);
    });

    it('content overlay and component overlay values are aligned with OverlayService', () => {
      expect(ChannelCtrl.isContentOverlayDisplayed).toEqual(OverlayService.isContentOverlayDisplayed);
      expect(ChannelCtrl.isComponentsOverlayDisplayed).toEqual(OverlayService.isComponentsOverlayDisplayed);
    });

    it('setters of isContentOverlayDisplayed & isComponentOverlayDisplayed should call overlayService functions', () => {
      spyOn(OverlayService, 'showContentOverlay');
      spyOn(OverlayService, 'showComponentsOverlay');
      const arg = false;
      ChannelCtrl.isContentOverlayDisplayed = arg;
      ChannelCtrl.isComponentsOverlayDisplayed = arg;

      expect(OverlayService.showContentOverlay).toHaveBeenCalledWith(arg);
      expect(OverlayService.showComponentsOverlay).toHaveBeenCalledWith(arg);
    });
  });

  it('loads the initial page', () => {
    expect(HippoIframeService.load).toHaveBeenCalledWith('/testPath');
  });

  it('checks whether the channel is loaded', () => {
    ChannelService.hasChannel.and.returnValue(false);
    expect(ChannelCtrl.isChannelLoaded()).toBe(false);

    ChannelService.hasChannel.and.returnValue(true);
    expect(ChannelCtrl.isChannelLoaded()).toBe(true);
  });

  it('checks whether the page is loaded', () => {
    HippoIframeService.isPageLoaded.and.returnValue(false);
    expect(ChannelCtrl.isPageLoaded()).toBe(false);

    HippoIframeService.isPageLoaded.and.returnValue(true);
    expect(ChannelCtrl.isPageLoaded()).toBe(true);
  });

  it('checks whether controls are disabled', () => {
    ChannelService.hasChannel.and.returnValue(false);
    HippoIframeService.isPageLoaded.and.returnValue(false);
    expect(ChannelCtrl.isControlsDisabled()).toBe(true);

    ChannelService.hasChannel.and.returnValue(false);
    HippoIframeService.isPageLoaded.and.returnValue(true);
    expect(ChannelCtrl.isControlsDisabled()).toBe(true);

    ChannelService.hasChannel.and.returnValue(true);
    HippoIframeService.isPageLoaded.and.returnValue(false);
    expect(ChannelCtrl.isControlsDisabled()).toBe(true);

    ChannelService.hasChannel.and.returnValue(true);
    HippoIframeService.isPageLoaded.and.returnValue(true);
    expect(ChannelCtrl.isControlsDisabled()).toBe(false);
  });

  it('checks whether a channel is editable', () => {
    ChannelService.isEditable.and.returnValue(false);
    expect(ChannelCtrl.isEditable()).toBe(false);

    ChannelService.isEditable.and.returnValue(true);
    expect(ChannelCtrl.isEditable()).toBe(true);
  });

  it('gets the render variant from the page meta-data service', () => {
    PageMetaDataService.getRenderVariant.and.returnValue('variant1');
    expect(ChannelCtrl.getRenderVariant()).toBe('variant1');
  });

  it('should not be true by default (components overlay)', () => {
    expect(ChannelCtrl.isComponentsOverlayDisplayed).toEqual(false);
  });

  it('correctly shows and hides subpages', () => {
    expect(ChannelCtrl.isSubpageOpen()).toBe(false);
    HippoIframeService.reload.and.returnValue($q.when());

    ChannelCtrl.showSubpage('test');
    expect(ChannelCtrl.isSubpageOpen()).toBe(true);

    ChannelCtrl.hideSubpage();
    expect(ChannelCtrl.isSubpageOpen()).toBe(false);
    $timeout.flush();

    ChannelCtrl.showSubpage('test');
    ChannelCtrl.onSubpageError('key', { param: 'value' });
    expect(ChannelCtrl.isSubpageOpen()).toBe(false);
    expect(FeedbackService.showError).toHaveBeenCalledWith('key', { param: 'value' });

    FeedbackService.showError.calls.reset();
    ChannelCtrl.showSubpage('test');
    ChannelCtrl.onSubpageError();
    expect(ChannelCtrl.isSubpageOpen()).toBe(false);
    expect(FeedbackService.showError).not.toHaveBeenCalled();

    ChannelCtrl.showSubpage('test');
    ChannelCtrl.onSubpageSuccess('key', { param: 'value' });
    expect(ChannelCtrl.isSubpageOpen()).toBe(false);

    ChannelCtrl.showSubpage('test');
    ChannelCtrl.onSubpageSuccess();
    expect(ChannelCtrl.isSubpageOpen()).toBe(false);
  });

  it('opens the menu editor when told so', () => {
    ChannelCtrl.editMenu('testUuid');

    expect(ChannelCtrl.menuUuid).toBe('testUuid');
    expect(ChannelCtrl.currentSubpage).toBe('menu-editor');
  });

  it('opens the content editor in the right sidepanel when told so', () => {
    spyOn(CmsService, 'reportUsageStatistic');

    ChannelCtrl.editContent('testUuid');

    expect(SidePanelService.open).toHaveBeenCalledWith('right', 'testUuid');
    expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('CMSChannelsEditContent');
  });

  it('should return channel toolbar display status', () => {
    ChannelService.setToolbarDisplayed(true);
    expect(ChannelCtrl.isToolbarDisplayed()).toBe(true);
    ChannelService.setToolbarDisplayed(false);
    expect(ChannelCtrl.isToolbarDisplayed()).toBe(false);
  });
});
