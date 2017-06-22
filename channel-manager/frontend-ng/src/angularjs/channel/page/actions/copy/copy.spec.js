/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

describe('PageActionCopy', () => {
  let $element;
  let $q;
  let $log;
  let $scope;
  let $rootScope;
  let $compile;
  let $translate;
  let ChannelService;
  let FeedbackService;
  let HippoIframeService;
  let SessionService;
  let SiteMapService;
  let SiteMapItemService;
  let siteMapItem;
  let channels = [];
  const pageModel = {
    locations: [
      {
        id: null,
        location: 'localhost/',
      },
      {
        id: 'root-level',
        location: 'localhost/foo/',
      },
      {
        id: 'nested',
        location: 'localhost/foo/bar/',
      },
    ],
  };

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$q_, _$log_, _$rootScope_, _$compile_, _$translate_, _ChannelService_, _FeedbackService_,
            _HippoIframeService_, _SessionService_, _SiteMapService_, _SiteMapItemService_) => {
      $q = _$q_;
      $log = _$log_;
      $rootScope = _$rootScope_;
      $compile = _$compile_;
      $translate = _$translate_;
      ChannelService = _ChannelService_;
      FeedbackService = _FeedbackService_;
      HippoIframeService = _HippoIframeService_;
      SessionService = _SessionService_;
      SiteMapService = _SiteMapService_;
      SiteMapItemService = _SiteMapItemService_;
    });

    siteMapItem = {
      id: 'siteMapItemId',
      parentLocation: {
        id: null,
      },
      name: 'name',
    };

    channels = [
      {
        id: 'channelA',
        mountId: 'channelMountA',
        name: 'Channel A',
      },
      {
        id: 'channelB',
        mountId: 'channelMountB',
        name: 'Channel B',
      },
      {
        id: 'channelC',
        mountId: 'channelMountC',
        name: 'Channel C',
      },
    ];

    spyOn($translate, 'instant').and.callFake(key => key);
    spyOn($log, 'info');
    spyOn(SessionService, 'isCrossChannelPageCopySupported').and.returnValue(true);
    spyOn(ChannelService, 'getPageModifiableChannels').and.returnValue(channels);
    spyOn(ChannelService, 'loadChannel').and.returnValue($q.when());
    spyOn(ChannelService, 'getNewPageModel').and.returnValue($q.when(pageModel));
    spyOn(ChannelService, 'getSiteMapId').and.returnValue('siteMapId');
    spyOn(ChannelService, 'recordOwnChange');
    spyOn(ChannelService, 'getId').and.returnValue('channelB');
    spyOn(FeedbackService, 'showError');
    spyOn(FeedbackService, 'showErrorResponse');
    spyOn(HippoIframeService, 'load');
    spyOn(SiteMapItemService, 'get').and.returnValue(siteMapItem);
    spyOn(SiteMapItemService, 'isEditable').and.returnValue(true);
    spyOn(SiteMapItemService, 'updateItem');
    spyOn(SiteMapService, 'copy').and.returnValue($q.when({ renderPathInfo: '/destination/path' }));
    spyOn(SiteMapService, 'load');
  });

  function compileDirectiveAndGetController() {
    $scope = $rootScope.$new();
    $scope.onDone = jasmine.createSpy('onDone');
    $element = angular.element('<page-copy on-done="onDone()"> </page-copy>');
    $compile($element)($scope);
    $scope.$digest();

    return $element.controller('pageCopy');
  }

  it('initializes correctly when cross channel copy is disabled', () => {
    SessionService.isCrossChannelPageCopySupported.and.returnValue(false);

    const PageCopyCtrl = compileDirectiveAndGetController();

    expect(ChannelService.getNewPageModel).toHaveBeenCalled();
    expect(ChannelService.getPageModifiableChannels).not.toHaveBeenCalled();

    expect(PageCopyCtrl.isCrossChannelCopyAvailable).toBeFalsy();
    expect(PageCopyCtrl.channels).toEqual([]);
    expect(PageCopyCtrl.locations).toBe(pageModel.locations);
    expect(PageCopyCtrl.location).toBe(pageModel.locations[0]);
    expect(PageCopyCtrl.lastPathInfoElement).toBe('');
    expect($translate.instant).toHaveBeenCalledWith('SUBPAGE_PAGE_COPY_TITLE', { pageName: 'name' });
  });

  it('initializes correctly when cross channel copy is enabled', () => {
    // we're on channel B and channels A, B and C are available, select B
    let PageCopyCtrl = compileDirectiveAndGetController();
    expect(PageCopyCtrl.channels).toBe(channels);
    expect(PageCopyCtrl.channel).toBe(channels[1]);
    expect(PageCopyCtrl.isCrossChannelCopyAvailable).toBe(true);
    expect(ChannelService.getNewPageModel.calls.mostRecent().args).toEqual(['channelMountB']);

    // we're on channel D and only channel A, B and C are available, select A
    ChannelService.getId.and.returnValue('channelD');
    PageCopyCtrl = compileDirectiveAndGetController();
    expect(PageCopyCtrl.channel).toBe(channels[0]);
    expect(PageCopyCtrl.isCrossChannelCopyAvailable).toBe(true);
    expect(ChannelService.getNewPageModel.calls.mostRecent().args).toEqual(['channelMountA']);

    // we're on channel D and only channel C is available, select C
    ChannelService.getPageModifiableChannels.and.returnValue([channels[2]]);
    PageCopyCtrl = compileDirectiveAndGetController();
    expect(PageCopyCtrl.channel).toBe(channels[2]);
    expect(PageCopyCtrl.isCrossChannelCopyAvailable).toBe(true);
    expect(ChannelService.getNewPageModel.calls.mostRecent().args).toEqual(['channelMountC']);

    // we're on channel C and only channel C is available, no cross-channel copying available
    ChannelService.getId.and.returnValue('channelC');
    PageCopyCtrl = compileDirectiveAndGetController();
    expect(PageCopyCtrl.channel).toBeUndefined();
    expect(PageCopyCtrl.isCrossChannelCopyAvailable).toBeFalsy();
    expect(ChannelService.getNewPageModel.calls.mostRecent().args).toEqual([undefined]);

    // we're on channel C, but no channels are available, no cross-channel copying available
    ChannelService.getPageModifiableChannels.and.returnValue([]);
    PageCopyCtrl = compileDirectiveAndGetController();
    expect(PageCopyCtrl.channel).toBeUndefined();
    expect(PageCopyCtrl.isCrossChannelCopyAvailable).toBeFalsy();
    expect(ChannelService.getNewPageModel.calls.mostRecent().args).toEqual([undefined]);
  });

  it('flashes a toast when the retrieval of the locations fails', () => {
    ChannelService.getNewPageModel.and.returnValue($q.reject());

    const PageCopyCtrl = compileDirectiveAndGetController();

    expect(PageCopyCtrl.locations).toEqual([]);
    expect(PageCopyCtrl.location).toBeUndefined();
    expect(FeedbackService.showErrorResponse)
      .toHaveBeenCalledWith(undefined, 'ERROR_PAGE_LOCATIONS_RETRIEVAL_FAILED');
  });

  it('successfully retrieves the locations for the selected channel', () => {
    const PageCopyCtrl = compileDirectiveAndGetController();

    // location is undefined when no locations are available
    ChannelService.getNewPageModel.and.returnValue($q.when({ }));
    PageCopyCtrl.lastPathInfoElement = 'keepUnchanged';
    PageCopyCtrl.channelChanged();
    expect(ChannelService.getNewPageModel).toHaveBeenCalledWith('channelMountB');
    $rootScope.$digest();
    expect(PageCopyCtrl.locations).toEqual([]);
    expect(PageCopyCtrl.location).toBeUndefined();
    expect(PageCopyCtrl.lastPathInfoElement).toBe('keepUnchanged');

    // "null" location matches
    ChannelService.getNewPageModel.and.returnValue($q.when(pageModel));
    PageCopyCtrl.channel = channels[2];
    PageCopyCtrl.channelChanged();
    expect(ChannelService.getNewPageModel).toHaveBeenCalledWith('channelMountC');
    $rootScope.$digest();
    expect(PageCopyCtrl.locations).toBe(pageModel.locations);
    expect(PageCopyCtrl.location).toBe(pageModel.locations[0]);

    // no location matches, select first one
    const locationsNoIdMatch = {
      locations: [
        {
          id: 'no-match',
          location: 'localhost/foo/',
        },
        {
          id: 'no-match-either',
          location: 'localhost/bar/',
        },
      ],
    };
    ChannelService.getNewPageModel.and.returnValue($q.when(locationsNoIdMatch));
    PageCopyCtrl.channelChanged();
    $rootScope.$digest();
    expect(PageCopyCtrl.locations).toBe(locationsNoIdMatch.locations);
    expect(PageCopyCtrl.location).toBe(locationsNoIdMatch.locations[0]);

    // a specific location matches, select that one
    siteMapItem.parentLocation.id = 'nested';
    ChannelService.getNewPageModel.and.returnValue($q.when(pageModel));
    PageCopyCtrl.channelChanged();
    $rootScope.$digest();
    expect(PageCopyCtrl.locations).toBe(pageModel.locations);
    expect(PageCopyCtrl.location).toBe(pageModel.locations[2]);
  });

  it('calls the callback when navigating back', () => {
    compileDirectiveAndGetController();

    $element.find('.qa-button-back').click();
    expect($scope.onDone).toHaveBeenCalled();
  });

  it('successfully copies the page inside the current channel', () => {
    const PageCopyCtrl = compileDirectiveAndGetController();

    SiteMapService.copy.and.returnValue($q.when({ renderPathInfo: '/render/path' }));
    PageCopyCtrl.lastPathInfoElement = 'test';
    PageCopyCtrl.copy();

    const headers = {
      siteMapItemUUId: 'siteMapItemId',
      targetName: 'test',
      mountId: 'channelMountB',
    };
    expect(SiteMapService.copy).toHaveBeenCalledWith('siteMapId', headers);
    $rootScope.$digest();

    expect(HippoIframeService.load).toHaveBeenCalledWith('/render/path');
    expect(SiteMapService.load).toHaveBeenCalledWith('siteMapId');
    expect(ChannelService.recordOwnChange).toHaveBeenCalled();
    expect($scope.onDone).toHaveBeenCalled();
  });

  it('successfully copies the page inside the current channel - without cross-channel copy support', () => {
    SessionService.isCrossChannelPageCopySupported.and.returnValue(false);
    const PageCopyCtrl = compileDirectiveAndGetController();

    SiteMapService.copy.and.returnValue($q.when({ renderPathInfo: '/render/path' }));
    PageCopyCtrl.lastPathInfoElement = 'test';
    PageCopyCtrl.location = pageModel.locations[1];
    PageCopyCtrl.copy();

    const headers = {
      siteMapItemUUId: 'siteMapItemId',
      targetName: 'test',
      targetSiteMapItemUUID: pageModel.locations[1].id,
    };
    expect(SiteMapService.copy).toHaveBeenCalledWith('siteMapId', headers);
    $rootScope.$digest();

    expect(HippoIframeService.load).toHaveBeenCalledWith('/render/path');
    expect(SiteMapService.load).toHaveBeenCalledWith('siteMapId');
    expect(ChannelService.recordOwnChange).toHaveBeenCalled();
    expect($scope.onDone).toHaveBeenCalled();
  });

  it('successfully copies the page into another channel', () => {
    const PageCopyCtrl = compileDirectiveAndGetController();

    SiteMapService.copy.and.returnValue($q.when({ renderPathInfo: '/render/path' }));
    PageCopyCtrl.channel = channels[2];
    PageCopyCtrl.location = pageModel.locations[2];
    PageCopyCtrl.lastPathInfoElement = 'test';
    PageCopyCtrl.copy();

    const headers = {
      siteMapItemUUId: 'siteMapItemId',
      targetName: 'test',
      targetSiteMapItemUUID: pageModel.locations[2].id,
      mountId: 'channelMountC',
    };
    expect(SiteMapService.copy).toHaveBeenCalledWith('siteMapId', headers);
    $rootScope.$digest();
    expect(ChannelService.loadChannel).toHaveBeenCalled();
    $rootScope.$digest();
    expect(HippoIframeService.load).toHaveBeenCalledWith('/render/path');
    expect(SiteMapService.load).not.toHaveBeenCalled();
    expect(ChannelService.recordOwnChange).not.toHaveBeenCalled();
    expect($scope.onDone).toHaveBeenCalled();
  });

  it('successfully copies a page into another channel, but the channel switch fails', () => {
    const PageCopyCtrl = compileDirectiveAndGetController();

    SiteMapService.copy.and.returnValue($q.when({ renderPathInfo: '/render/path' }));
    ChannelService.loadChannel.and.returnValue($q.reject());
    PageCopyCtrl.channel = channels[2];
    PageCopyCtrl.copy();
    $rootScope.$digest(); // copy-success
    $rootScope.$digest(); // channel switch-failure
    expect($scope.onDone).toHaveBeenCalled();
    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_CHANNEL_SWITCH_FAILED');
  });

  it('fails to copy a page', () => {
    const PageCopyCtrl = compileDirectiveAndGetController();

    SiteMapService.copy.and.returnValue($q.reject());
    PageCopyCtrl.copy();
    $rootScope.$digest();
    expect(FeedbackService.showErrorResponse)
      .toHaveBeenCalledWith(undefined, 'ERROR_PAGE_COPY_FAILED', PageCopyCtrl.errorMap);

    const response = { key: 'value' };
    SiteMapService.copy.and.returnValue($q.reject(response));
    PageCopyCtrl.copy();
    $rootScope.$digest();
    expect(FeedbackService.showErrorResponse)
      .toHaveBeenCalledWith(response, 'ERROR_PAGE_COPY_FAILED', PageCopyCtrl.errorMap);
  });
});
