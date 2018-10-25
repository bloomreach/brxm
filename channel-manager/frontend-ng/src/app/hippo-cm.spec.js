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
  let $q;
  let $rootScope;
  let $state;
  let $timeout;
  let $window;
  let BrowserService;
  let ChannelService;
  let CmsService;
  let ConfigService;
  let HippoIframeService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      $componentController,
      _$q_,
      _$rootScope_,
      _$timeout_,
      _$window_,
      _CmsService_,
    ) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      $timeout = _$timeout_;
      $window = _$window_;
      CmsService = _CmsService_;

      $state = jasmine.createSpyObj('$state', ['defaultErrorHandler', 'includes', 'go']);

      BrowserService = jasmine.createSpyObj('$state', ['isIE']);

      ChannelService = jasmine.createSpyObj('ChannelService', [
        'initializeChannel',
        'makeRenderPath',
        'matchesChannel',
        'reload',
      ]);

      ConfigService = jasmine.createSpyObj('ConfigService', [
        'isDevMode',
      ]);

      HippoIframeService = jasmine.createSpyObj('HippoIframeService', [
        'initializePath',
        'reload',
      ]);

      $ctrl = $componentController('hippoCm', {
        $state,
        BrowserService,
        ChannelService,
        ConfigService,
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
    it('opens a channel when there was no channel yet', () => {
      ChannelService.matchesChannel.and.returnValue(false);
      ChannelService.initializeChannel.and.returnValue($q.resolve());
      $state.includes.and.returnValue(false);
      spyOn($rootScope, '$apply').and.callThrough();

      $ctrl.$onInit();
      $window.CMS_TO_APP.publish('load-channel', 'testChannel', '/site', 'testProject', '/some/path');

      expect($rootScope.$apply).toHaveBeenCalled();
      expect(ChannelService.initializeChannel).toHaveBeenCalledWith('testChannel', '/site', 'testProject');
      $rootScope.$digest();
      expect($state.go).toHaveBeenCalledWith('hippo-cm.channel');
      expect(HippoIframeService.initializePath).toHaveBeenCalledWith('/some/path');
    });

    it('opens a different channel', () => {
      ChannelService.matchesChannel.and.returnValue(false);
      ChannelService.initializeChannel.and.returnValue($q.resolve());
      $state.includes.and.returnValue(true);
      spyOn($rootScope, '$apply').and.callThrough();

      $ctrl.$onInit();
      $window.CMS_TO_APP.publish('load-channel', 'testChannel', '/site', 'testProject', '/some/path');

      expect($rootScope.$apply).toHaveBeenCalled();
      expect(ChannelService.initializeChannel).toHaveBeenCalledWith('testChannel', '/site', 'testProject');
      $rootScope.$digest();
      expect($state.go).not.toHaveBeenCalled();
      expect(HippoIframeService.initializePath).toHaveBeenCalledWith('/some/path');
    });

    it('changes the page in the current channel', () => {
      ChannelService.matchesChannel.and.returnValue(true);
      spyOn($rootScope, '$apply').and.callThrough();

      $ctrl.$onInit();
      $window.CMS_TO_APP.publish('load-channel', 'testChannel', '/site', 'testProject', '/some/path');

      expect($rootScope.$apply).toHaveBeenCalled();
      expect(ChannelService.initializeChannel).not.toHaveBeenCalled();
      expect(HippoIframeService.initializePath).toHaveBeenCalledWith('/some/path');
      expect($state.go).not.toHaveBeenCalledWith('hippo-cm.channel');
    });

    it('reloads the current page in the current channel', () => {
      ChannelService.matchesChannel.and.returnValue(true);
      spyOn($rootScope, '$apply').and.callThrough();

      $ctrl.$onInit();
      $window.CMS_TO_APP.publish('load-channel', 'testChannel', '/site', 'testProject', null);

      expect($rootScope.$apply).toHaveBeenCalled();
      expect(ChannelService.initializeChannel).not.toHaveBeenCalled();
      expect(HippoIframeService.initializePath).toHaveBeenCalledWith(null);
      expect($state.go).not.toHaveBeenCalledWith('hippo-cm.channel');
    });
  });

  describe('the reload-channel event', () => {
    it('reloads the current channel and page', () => {
      ChannelService.reload.and.returnValue($q.resolve());
      spyOn($rootScope, '$apply').and.callThrough();

      $ctrl.$onInit();
      $window.CMS_TO_APP.publish('reload-channel');

      expect($rootScope.$apply).toHaveBeenCalled();
      expect(ChannelService.reload).toHaveBeenCalled();
      $rootScope.$digest();
      expect(HippoIframeService.reload).toHaveBeenCalled();
    });
  });

  it('reloads the current channel when ExtJs changed it', () => {
    spyOn($rootScope, '$apply').and.callThrough();

    $ctrl.$onInit();
    $window.CMS_TO_APP.publish('channel-changed-in-extjs');

    expect($rootScope.$apply).toHaveBeenCalled();
    expect(ChannelService.reload).toHaveBeenCalled();
  });

  describe('dev mode', () => {
    beforeEach(() => {
      ConfigService.isDevMode.and.returnValue(true);

      ChannelService.matchesChannel.and.returnValue(false);
      ChannelService.initializeChannel.and.returnValue($q.resolve());
    });

    afterEach(() => {
      delete sessionStorage.channelId;
      delete sessionStorage.channelPath;
      delete sessionStorage.channelBranch;
    });

    it('stores the loaded channel, context path, branch and path in sessionStorage', () => {
      $ctrl.$onInit();

      $window.CMS_TO_APP.publish('load-channel', 'testChannel', '/site', 'testProject', '/test/path');

      expect(sessionStorage.channelId).toBe('testChannel');
      expect(sessionStorage.channelContext).toBe('/site');
      expect(sessionStorage.channelBranch).toBe('testProject');
      expect(sessionStorage.channelPath).toBe('/test/path');
    });

    it('initializes the channel, context path, branch and path from sessionStorage', () => {
      sessionStorage.channelId = 'testChannel';
      sessionStorage.channelContext = '/site';
      sessionStorage.channelBranch = 'testProject';
      sessionStorage.channelPath = '/test/path';

      $ctrl.$onInit();
      $timeout.flush();

      expect(ChannelService.initializeChannel).toHaveBeenCalledWith('testChannel', '/site', 'testProject');
      $rootScope.$digest();
      expect(HippoIframeService.initializePath).toHaveBeenCalledWith('/test/path');
    });
  });
});
