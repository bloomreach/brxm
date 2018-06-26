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

describe('hippoCm', () => {
  let $ctrl;
  let $rootScope;
  let $state;
  let $window;
  let BrowserService;
  let ChannelService;
  let CmsService;
  let HippoIframeService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      $componentController,
      _$rootScope_,
      _$window_,
      _CmsService_,
    ) => {
      $rootScope = _$rootScope_;
      $window = _$window_;
      CmsService = _CmsService_;

      $state = jasmine.createSpyObj('$state', ['defaultErrorHandler']);

      BrowserService = jasmine.createSpyObj('$state', ['isIE']);

      ChannelService = jasmine.createSpyObj('ChannelService', [
        'initializeChannel',
        'makeRenderPath',
        'matchesChannel',
        'reload',
      ]);

      HippoIframeService = jasmine.createSpyObj('HippoIframeService', [
        'getCurrentRenderPathInfo',
        'load',
        'reload',
      ]);

      $ctrl = $componentController('hippoCm', {
        $state,
        BrowserService,
        ChannelService,
        HippoIframeService,
      });
    });

    spyOn(CmsService, 'publish');
  });

  afterEach(() => {
    $('body').removeClass('ie11');
  });

  it('prevents uiRouter state transition errors from polluting the JavaScript console', () => {
    $ctrl.$onInit();
    expect($state.defaultErrorHandler).toHaveBeenCalledWith(angular.noop);
  });

  describe('"ie11" CSS class on the body', () => {
    it('is added in IE', () => {
      BrowserService.isIE.and.returnValue(true);
      $ctrl.$onInit();
      expect($('body')).toHaveClass('ie11');
    });

    it('is not added in other browsers', () => {
      BrowserService.isIE.and.returnValue(false);
      $ctrl.$onInit();
      expect($('body')).not.toHaveClass('ie11');
    });
  });

  describe('the load-channel event', () => {
    it('opens a different channel', () => {
      ChannelService.matchesChannel.and.returnValue(false);

      $ctrl.$onInit();
      $window.CMS_TO_APP.publish('load-channel', { id: 'testChannel' }, '/some/path', 'testProject');

      expect(ChannelService.initializeChannel).toHaveBeenCalledWith({ id: 'testChannel' }, '/some/path', 'testProject');
    });

    it('changes the page in the current channel', () => {
      ChannelService.matchesChannel.and.returnValue(true);
      ChannelService.makeRenderPath.and.returnValue('/different/path');
      HippoIframeService.getCurrentRenderPathInfo.and.returnValue('/path');
      spyOn($rootScope, '$apply').and.callThrough();

      $ctrl.$onInit();
      $window.CMS_TO_APP.publish('load-channel', { id: 'testChannel' }, '/different/path', 'testProject');

      expect($rootScope.$apply).toHaveBeenCalled();
      expect(HippoIframeService.load).toHaveBeenCalledWith('/different/path');
    });

    it('changes to the home page in the current channel', () => {
      ChannelService.matchesChannel.and.returnValue(true);
      ChannelService.makeRenderPath.and.returnValue('');
      HippoIframeService.getCurrentRenderPathInfo.and.returnValue('/path');
      spyOn($rootScope, '$apply').and.callThrough();

      $ctrl.$onInit();
      $window.CMS_TO_APP.publish('load-channel', { id: 'testChannel' }, '', 'testProject');

      expect($rootScope.$apply).toHaveBeenCalled();
      expect(HippoIframeService.load).toHaveBeenCalledWith('');
    });

    it('reloads a page in the current channel', () => {
      ChannelService.matchesChannel.and.returnValue(true);
      ChannelService.makeRenderPath.and.returnValue('/path');
      HippoIframeService.getCurrentRenderPathInfo.and.returnValue('/path');

      $ctrl.$onInit();
      $window.CMS_TO_APP.publish('load-channel', { id: 'testChannel' }, '/path', 'testProject');

      expect(HippoIframeService.reload).toHaveBeenCalled();
    });

    it('reloads the current page in the current channel', () => {
      ChannelService.matchesChannel.and.returnValue(true);
      HippoIframeService.getCurrentRenderPathInfo.and.returnValue('/path');

      $ctrl.$onInit();
      $window.CMS_TO_APP.publish('load-channel', { id: 'testChannel' }, null, 'testProject');

      expect(HippoIframeService.reload).toHaveBeenCalled();
    });

    it('changes to the same channel-relative path in a different channel', () => {
      ChannelService.matchesChannel.and.returnValue(true);
      ChannelService.makeRenderPath.and.returnValue('/mount-of-channel2/path');
      HippoIframeService.getCurrentRenderPathInfo.and.returnValue('/mount-of-channel1/path');
      spyOn($rootScope, '$apply').and.callThrough();

      $ctrl.$onInit();
      $window.CMS_TO_APP.publish('load-channel', { id: 'channel2' }, '/path', 'testProject');

      expect($rootScope.$apply).toHaveBeenCalled();
      expect(HippoIframeService.load).toHaveBeenCalledWith('/mount-of-channel2/path');
    });
  });

  it('reloads the current channel when ExtJs changed it', () => {
    spyOn($rootScope, '$apply').and.callThrough();

    $ctrl.$onInit();
    $window.CMS_TO_APP.publish('channel-changed-in-extjs');

    expect($rootScope.$apply).toHaveBeenCalled();
    expect(ChannelService.reload).toHaveBeenCalled();
  });

  it('initializes the loaded channel again after an app reload (e.g. by Webpack)', () => {
    $ctrl.$onInit();
    expect(CmsService.publish).toHaveBeenCalledWith('reload-channel');
  });
});
