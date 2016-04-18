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

/* eslint-disable prefer-const */

describe('ChannelSiteMapService', () => {
  'use strict';

  let $q;
  let $rootScope;
  let ChannelSiteMapService;
  let HstService;
  let FeedbackService;

  beforeEach(() => {
    module('hippo-cm');

    inject((_$q_, _$rootScope_, _ChannelSiteMapService_, _HstService_, _FeedbackService_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      ChannelSiteMapService = _ChannelSiteMapService_;
      HstService = _HstService_;
      FeedbackService = _FeedbackService_;
    });

    spyOn(HstService, 'getSiteMap');
    spyOn(FeedbackService, 'showError');
  });

  it('initializes an empty sitemap', () => {
    expect(ChannelSiteMapService.get()).toEqual([]);
  });

  it('retrieves the sitemap from the HST service', () => {
    const siteMap = ['dummy'];
    HstService.getSiteMap.and.returnValue($q.when(siteMap));
    ChannelSiteMapService.load('siteMapId');
    $rootScope.$digest();

    expect(HstService.getSiteMap).toHaveBeenCalledWith('siteMapId');
    expect(ChannelSiteMapService.get()).toBe(siteMap);
    expect(FeedbackService.showError).not.toHaveBeenCalled();
  });

  it('flashes a toast when the sitemap cannot be retrieved', () => {
    HstService.getSiteMap.and.returnValue($q.reject());
    ChannelSiteMapService.load('siteMapId');
    $rootScope.$digest();

    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_SITEMAP_RETRIEVAL_FAILED');
    expect(ChannelSiteMapService.get()).toEqual([]);
  });

  it('clears the existing sitemap when the sitemap cannot be retrieved', () => {
    HstService.getSiteMap.and.returnValue($q.when(['dummy']));
    ChannelSiteMapService.load('siteMapId');
    $rootScope.$digest();

    HstService.getSiteMap.and.returnValue($q.reject());
    ChannelSiteMapService.load('siteMapId2');
    $rootScope.$digest();

    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_SITEMAP_RETRIEVAL_FAILED');
    expect(ChannelSiteMapService.get()).toEqual([]);
  });
});
