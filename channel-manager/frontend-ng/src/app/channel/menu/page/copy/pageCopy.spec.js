/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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

describe('PageCopyComponent', () => {
  let $compile;
  let $componentController;
  let $ctrl;
  let $log;
  let $q;
  let $rootScope;
  let $translate;
  let ChannelService;
  let FeedbackService;
  let HippoIframeService;
  let SessionService;
  let SiteMapItemService;
  let SiteMapService;

  let channels = [];
  let pageModel;
  let siteMapItem;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      _$compile_,
      _$componentController_,
      _$log_,
      _$q_,
      _$rootScope_,
      _$translate_,
      _ChannelService_,
      _FeedbackService_,
      _HippoIframeService_,
      _SessionService_,
      _SiteMapItemService_,
      _SiteMapService_,
    ) => {
      $compile = _$compile_;
      $componentController = _$componentController_;
      $log = _$log_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      $translate = _$translate_;
      ChannelService = _ChannelService_;
      FeedbackService = _FeedbackService_;
      HippoIframeService = _HippoIframeService_;
      SessionService = _SessionService_;
      SiteMapItemService = _SiteMapItemService_;
      SiteMapService = _SiteMapService_;
    });

    pageModel = {
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

    siteMapItem = {
      id: 'siteMapItemId',
      parentLocation: {
        id: null,
      },
      name: 'name',
    };

    channels = [
      {
        contextPath: '/site',
        hostGroup: 'dev-localhost',
        id: 'channelA',
        mountId: 'channelMountA',
        name: 'Channel A',
      },
      {
        contextPath: '/site',
        hostGroup: 'dev-localhost',
        id: 'channelB',
        mountId: 'channelMountB',
        name: 'Channel B',
      },
      {
        contextPath: '/intranet',
        hostGroup: 'dev-localhost',
        id: 'channelC',
        mountId: 'channelMountC',
        name: 'Channel C',
      },
    ];

    spyOn($log, 'info');
    spyOn($translate, 'instant').and.callFake(key => key);
    spyOn(SessionService, 'isCrossChannelPageCopySupported').and.returnValue(true);
    spyOn(ChannelService, 'getPageModifiableChannels').and.returnValue(channels);
    spyOn(ChannelService, 'initializeChannel').and.returnValue();
    spyOn(ChannelService, 'getNewPageModel').and.returnValue($q.when(pageModel));
    spyOn(ChannelService, 'getSiteMapId').and.returnValue('siteMapId');
    spyOn(ChannelService, 'checkChanges').and.returnValue($q.resolve());
    spyOn(ChannelService, 'getId').and.returnValue('channelB');
    spyOn(FeedbackService, 'showError');
    spyOn(FeedbackService, 'showErrorResponse');
    spyOn(HippoIframeService, 'initializePath');
    spyOn(HippoIframeService, 'load');
    spyOn(SiteMapItemService, 'get').and.returnValue(siteMapItem);
    spyOn(SiteMapItemService, 'isEditable').and.returnValue(true);
    spyOn(SiteMapItemService, 'updateItem');
    spyOn(SiteMapService, 'copy').and.returnValue($q.when({ renderPathInfo: '/destination/path' }));
    spyOn(SiteMapService, 'load');

    $ctrl = $componentController('pageCopy', null, {
      onDone: jasmine.createSpy('onDone'),
    });
  });

  describe('$element tests', () => {
    let $element;
    let $scope;

    function compileComponentAndGetController() {
      $scope = $rootScope.$new();
      $scope.onDone = jasmine.createSpy('onDone');
      $element = angular.element('<page-copy on-done="onDone()"> </page-copy>');
      $compile($element)($scope);
      $scope.$digest();

      return $element.controller('pageCopy');
    }

    it('calls "onDone" when clicking cancel', () => {
      compileComponentAndGetController();

      $element.find('.qa-discard').click();
      expect($scope.onDone).toHaveBeenCalled();
    });
  });

  it('initializes correctly when cross channel copy is disabled', () => {
    SessionService.isCrossChannelPageCopySupported.and.returnValue(false);
    $ctrl.$onInit();
    $rootScope.$digest();

    expect(ChannelService.getNewPageModel).toHaveBeenCalled();
    expect(ChannelService.getPageModifiableChannels).not.toHaveBeenCalled();

    expect($ctrl.isCrossChannelCopyAvailable).toBeFalsy();
    expect($ctrl.channels).toEqual([]);
    expect($ctrl.locations).toBe(pageModel.locations);
    expect($ctrl.location).toBe(pageModel.locations[0]);
    expect($ctrl.lastPathInfoElement).toBe('');
    expect($translate.instant).toHaveBeenCalledWith('SUBPAGE_PAGE_COPY_TITLE', { pageName: 'name' });
  });

  describe('cross channel copy', () => {
    it('initializes with the current channel', () => {
      $ctrl.$onInit();
      $rootScope.$digest();

      // we're on channel B and channels A, B and C are available, select B
      expect($ctrl.channels).toBe(channels);
      expect($ctrl.channel).toBe(channels[1]);
      expect($ctrl.isCrossChannelCopyAvailable).toBe(true);
      expect(ChannelService.getNewPageModel.calls.mostRecent().args).toEqual(['channelMountB']);
    });

    it('initializes with the first available channel if current channel is not available', () => {
      // we're on channel D and only channel A, B and C are available, select A
      ChannelService.getId.and.returnValue('channelD');
      $ctrl.$onInit();
      $rootScope.$digest();

      expect($ctrl.channel).toBe(channels[0]);
      expect($ctrl.isCrossChannelCopyAvailable).toBe(true);
      expect(ChannelService.getNewPageModel.calls.mostRecent().args).toEqual(['channelMountA']);
    });

    it('initializes with the only available channel if current channel is not available', () => {
      // we're on channel D and only channel C is available, select C
      ChannelService.getPageModifiableChannels.and.returnValue([channels[2]]);
      $ctrl.$onInit();
      $rootScope.$digest();

      expect($ctrl.channel).toBe(channels[2]);
      expect($ctrl.isCrossChannelCopyAvailable).toBe(true);
      expect(ChannelService.getNewPageModel.calls.mostRecent().args).toEqual(['channelMountC']);
    });

    it('does not enable cross-channel copying if the current channel is the only available channel', () => {
      // we're on channel C and only channel C is available, no cross-channel copying available
      ChannelService.getPageModifiableChannels.and.returnValue([channels[2]]);
      ChannelService.getId.and.returnValue('channelC');
      $ctrl.$onInit();
      $rootScope.$digest();

      expect($ctrl.channel).toBeUndefined();
      expect($ctrl.isCrossChannelCopyAvailable).toBeFalsy();
      expect(ChannelService.getNewPageModel.calls.mostRecent().args).toEqual([undefined]);
    });

    it('does not enable cross-channel copying if no channels are available', () => {
      // we're on channel C, but no channels are available, no cross-channel copying available
      ChannelService.getPageModifiableChannels.and.returnValue([]);
      $ctrl.$onInit();
      $rootScope.$digest();

      expect($ctrl.channel).toBeUndefined();
      expect($ctrl.isCrossChannelCopyAvailable).toBeFalsy();
      expect(ChannelService.getNewPageModel.calls.mostRecent().args).toEqual([undefined]);
    });
  });

  it('flashes a toast when the retrieval of the locations fails', () => {
    ChannelService.getNewPageModel.and.returnValue($q.reject());
    $ctrl.$onInit();
    $rootScope.$digest();

    expect($ctrl.locations).toEqual([]);
    expect($ctrl.location).toBeUndefined();
    expect(FeedbackService.showErrorResponse)
      .toHaveBeenCalledWith(undefined, 'ERROR_PAGE_LOCATIONS_RETRIEVAL_FAILED');
  });

  it('successfully retrieves the locations for the selected channel', () => {
    $ctrl.$onInit();
    $rootScope.$digest();

    // location is undefined when no locations are available
    ChannelService.getNewPageModel.and.returnValue($q.when({ }));
    $ctrl.lastPathInfoElement = 'keepUnchanged';
    $ctrl.channelChanged();
    expect(ChannelService.getNewPageModel).toHaveBeenCalledWith('channelMountB');
    $rootScope.$digest();
    expect($ctrl.locations).toEqual([]);
    expect($ctrl.location).toBeUndefined();
    expect($ctrl.lastPathInfoElement).toBe('keepUnchanged');

    // "null" location matches
    ChannelService.getNewPageModel.and.returnValue($q.when(pageModel));
    [,, $ctrl.channel] = channels;
    $ctrl.channelChanged();
    expect(ChannelService.getNewPageModel).toHaveBeenCalledWith('channelMountC');
    $rootScope.$digest();
    expect($ctrl.locations).toBe(pageModel.locations);
    expect($ctrl.location).toBe(pageModel.locations[0]);

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
    $ctrl.channelChanged();
    $rootScope.$digest();
    expect($ctrl.locations).toBe(locationsNoIdMatch.locations);
    expect($ctrl.location).toBe(locationsNoIdMatch.locations[0]);

    // a specific location matches, select that one
    siteMapItem.parentLocation.id = 'nested';
    ChannelService.getNewPageModel.and.returnValue($q.when(pageModel));
    $ctrl.channelChanged();
    $rootScope.$digest();
    expect($ctrl.locations).toBe(pageModel.locations);
    expect($ctrl.location).toBe(pageModel.locations[2]);
  });

  it('successfully copies the page inside the current channel', () => {
    $ctrl.$onInit();
    $rootScope.$digest();

    SiteMapService.copy.and.returnValue($q.when({ renderPathInfo: '/render/path' }));
    $ctrl.lastPathInfoElement = 'test';
    $ctrl.copy();

    const headers = {
      siteMapItemUUId: 'siteMapItemId',
      targetName: 'test',
      mountId: 'channelMountB',
    };
    expect(SiteMapService.copy).toHaveBeenCalledWith('siteMapId', headers);
    $rootScope.$digest();

    expect(HippoIframeService.load).toHaveBeenCalledWith('/render/path');
    expect(SiteMapService.load).toHaveBeenCalledWith('siteMapId');
    expect(ChannelService.checkChanges).toHaveBeenCalled();
    expect($ctrl.onDone).toHaveBeenCalled();
  });

  it('successfully copies the page inside the current channel - without cross-channel copy support', () => {
    SessionService.isCrossChannelPageCopySupported.and.returnValue(false);
    $ctrl.$onInit();
    $rootScope.$digest();

    SiteMapService.copy.and.returnValue($q.when({ renderPathInfo: '/render/path' }));
    $ctrl.lastPathInfoElement = 'test';
    [, $ctrl.location] = pageModel.locations;
    $ctrl.copy();

    const headers = {
      siteMapItemUUId: 'siteMapItemId',
      targetName: 'test',
      targetSiteMapItemUUID: pageModel.locations[1].id,
    };
    expect(SiteMapService.copy).toHaveBeenCalledWith('siteMapId', headers);
    $rootScope.$digest();

    expect(HippoIframeService.load).toHaveBeenCalledWith('/render/path');
    expect(SiteMapService.load).toHaveBeenCalledWith('siteMapId');
    expect(ChannelService.checkChanges).toHaveBeenCalled();
    expect($ctrl.onDone).toHaveBeenCalled();
  });

  it('successfully copies the page if the page element contains non ISO-8859-1 elements', () => {
    SessionService.isCrossChannelPageCopySupported.and.returnValue(false);
    $ctrl.$onInit();
    $rootScope.$digest();

    SiteMapService.copy.and.returnValue($q.when({ renderPathInfo: '/render/path' }));
    $ctrl.lastPathInfoElement = 'a 你 好';
    [, $ctrl.location] = pageModel.locations;
    $ctrl.copy();

    const headers = {
      siteMapItemUUId: 'siteMapItemId',
      targetName: encodeURIComponent('a 你 好'),
      targetSiteMapItemUUID: pageModel.locations[1].id,
    };
    expect(SiteMapService.copy).toHaveBeenCalledWith('siteMapId', headers);
    $rootScope.$digest();

    expect(HippoIframeService.load).toHaveBeenCalledWith('/render/path');
    expect(SiteMapService.load).toHaveBeenCalledWith('siteMapId');
    expect(ChannelService.checkChanges).toHaveBeenCalled();
    expect($ctrl.onDone).toHaveBeenCalled();
  });

  it('successfully copies the page to another channel', () => {
    $ctrl.$onInit();
    $rootScope.$digest();

    const copyReturn = {
      contextPath: '/another',
      renderPathInfo: '/render/path',
      pathInfo: 'path',
    };

    SiteMapService.copy.and.returnValue($q.when(copyReturn));
    [,, $ctrl.channel] = channels;
    [,, $ctrl.location] = pageModel.locations;
    $ctrl.lastPathInfoElement = 'test';

    ChannelService.initializeChannel.and.returnValue($q.resolve());

    $ctrl.copy();

    const headers = {
      siteMapItemUUId: 'siteMapItemId',
      targetName: 'test',
      targetSiteMapItemUUID: pageModel.locations[2].id,
      mountId: 'channelMountC',
    };
    expect(SiteMapService.copy).toHaveBeenCalledWith('siteMapId', headers);
    $rootScope.$digest();
    expect(ChannelService.initializeChannel)
      .toHaveBeenCalledWith($ctrl.channel.id, $ctrl.channel.contextPath, $ctrl.channel.hostGroup);
    $rootScope.$digest();
    expect(HippoIframeService.initializePath).toHaveBeenCalledWith(copyReturn.pathInfo);
    expect($ctrl.onDone).toHaveBeenCalled();
  });

  it('fails to copy a page', () => {
    $ctrl.$onInit();
    $rootScope.$digest();

    SiteMapService.copy.and.returnValue($q.reject());
    $ctrl.copy();
    $rootScope.$digest();
    expect(FeedbackService.showErrorResponse)
      .toHaveBeenCalledWith(undefined, 'ERROR_PAGE_COPY_FAILED', $ctrl.errorMap);

    const response = { key: 'value' };
    SiteMapService.copy.and.returnValue($q.reject(response));
    $ctrl.copy();
    $rootScope.$digest();
    expect(FeedbackService.showErrorResponse)
      .toHaveBeenCalledWith(response, 'ERROR_PAGE_COPY_FAILED', $ctrl.errorMap);
  });
});
