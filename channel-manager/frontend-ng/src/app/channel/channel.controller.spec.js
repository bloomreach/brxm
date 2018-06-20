/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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
  let $timeout;
  let $window;
  let ChannelService;
  let SidePanelService;
  let ComponentsService;
  let FeedbackService;
  let HippoIframeService;
  let PageMetaDataService;
  let OverlayService;
  let ProjectService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      $componentController,
      _$timeout_,
      _$q_,
      _$window_,
      _FeedbackService_,
      _ChannelService_,
      _CmsService_,
      _OverlayService_,
      _ProjectService_,
    ) => {
      const resolvedPromise = _$q_.when();

      $timeout = _$timeout_;
      $q = _$q_;
      $window = _$window_;
      FeedbackService = _FeedbackService_;
      ChannelService = _ChannelService_;
      OverlayService = _OverlayService_;
      ProjectService = _ProjectService_;

      spyOn(ChannelService, 'clearChannel');
      spyOn(ChannelService, 'getInitialRenderPath').and.returnValue('/testPath');
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

      $ctrl = $componentController('channel', {
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
    it('content overlay and component overlay values are aligned with OverlayService', () => {
      expect($ctrl.isContentOverlayDisplayed).toEqual(OverlayService.isContentOverlayDisplayed);
      expect($ctrl.isComponentsOverlayDisplayed).toEqual(OverlayService.isComponentsOverlayDisplayed);
    });

    it('setters of isContentOverlayDisplayed & isComponentOverlayDisplayed should call overlayService functions', () => {
      spyOn(OverlayService, 'showContentOverlay');
      spyOn(OverlayService, 'showComponentsOverlay');
      const arg = false;
      $ctrl.isContentOverlayDisplayed = arg;
      $ctrl.isComponentsOverlayDisplayed = arg;

      expect(OverlayService.showContentOverlay).toHaveBeenCalledWith(arg);
      expect(OverlayService.showComponentsOverlay).toHaveBeenCalledWith(arg);
    });
  });

  it('loads the initial page', () => {
    $ctrl.$onInit();
    expect(HippoIframeService.load).toHaveBeenCalledWith('/testPath');
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

  it('checks whether a channel is editable', () => {
    ChannelService.isEditable.and.returnValue(false);
    expect($ctrl.isEditable()).toBe(false);

    ChannelService.isEditable.and.returnValue(true);
    expect($ctrl.isEditable()).toBe(true);
  });

  it('gets the render variant from the page meta-data service', () => {
    PageMetaDataService.getRenderVariant.and.returnValue('variant1');
    expect($ctrl.getRenderVariant()).toBe('variant1');
  });

  it('should not be true by default (components overlay)', () => {
    expect($ctrl.isComponentsOverlayDisplayed).toEqual(false);
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

  it('should delegate isContentOverlayEnabled to ProjectService', () => {
    let toggle = false;
    spyOn(ProjectService, 'isContentOverlayEnabled').and.callFake(() => {
      toggle = !toggle;
      return toggle;
    });
    expect($ctrl.isContentOverlayEnabled).toBeTruthy();
    expect($ctrl.isContentOverlayEnabled).toBeFalsy();
  });

  it('should delegate isComponentsOverlayEnabled to ProjectService', () => {
    let toggle = false;
    spyOn(ProjectService, 'isComponentsOverlayEnabled').and.callFake(() => {
      toggle = !toggle;
      return toggle;
    });
    expect($ctrl.isComponentsOverlayEnabled).toBeTruthy();
    expect($ctrl.isComponentsOverlayEnabled).toBeFalsy();
  });

  describe('reload-channel event from ExtJS', () => {
    beforeEach(() => {
      $ctrl.$onInit();
    });

    it('handles the reload-channel event from ExtJS when an item is already locked', () => {
      $window.CMS_TO_APP.publish('reload-channel', { error: 'ITEM_ALREADY_LOCKED', parameterMap: { lockedBy: 'admin' } });

      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_UPDATE_COMPONENT_ITEM_ALREADY_LOCKED', { lockedBy: 'admin' });
      expect(HippoIframeService.reload).toHaveBeenCalled();
    });

    it('handles the reload-channel event from ExtJS when an item is not found', () => {
      $window.CMS_TO_APP.publish('reload-channel', { error: 'ITEM_NOT_FOUND', parameterMap: { component: 'Banner' } });

      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_COMPONENT_DELETED', { component: 'Banner' });
      expect(HippoIframeService.reload).toHaveBeenCalled();
    });

    it('handles the reload-channel event from ExtJS when editing a component failed', () => {
      $window.CMS_TO_APP.publish('reload-channel', { error: '' });

      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_UPDATE_COMPONENT', undefined);
      expect(HippoIframeService.reload).toHaveBeenCalled();
    });

    it('is unsubscribed when the controller is destroyed', () => {
      $ctrl.$onDestroy();
      $window.CMS_TO_APP.publish('reload-channel', { error: '' });
      expect(FeedbackService.showError).not.toHaveBeenCalled();
      expect(HippoIframeService.reload).not.toHaveBeenCalled();
    });
  });
});
