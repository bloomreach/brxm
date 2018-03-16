/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

describe('siteMapListingController', () => {
  let $ctrl;
  let HippoIframeService;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.siteMapListing');

    inject(($componentController, _HippoIframeService_) => {
      $ctrl = $componentController('siteMapListing');
      HippoIframeService = _HippoIframeService_;
    });

    spyOn(HippoIframeService, 'getCurrentRenderPathInfo');
    spyOn(HippoIframeService, 'load');
  });

  it('calculates the track-by hash of a site map item', () => {
    expect($ctrl.getSiteMapItemHash({
      pageTitle: 'Title',
      pathInfo: '/title',
      name: 'not used',
    })).toEqual('/title\0Title');

    expect($ctrl.getSiteMapItemHash({
      pathInfo: '/title',
      name: 'title',
    })).toEqual('/title\0title');
  });

  it('asks the HippoIframeService to load the requested siteMap item', () => {
    const siteMapItem = {
      renderPathInfo: 'dummy',
    };
    $ctrl.showPage(siteMapItem);

    expect(HippoIframeService.load).toHaveBeenCalledWith('dummy');
  });

  it('compares the site map item\'s renderPathInfo to the current one', () => {
    HippoIframeService.getCurrentRenderPathInfo.and.returnValue('/current/path');
    const siteMapItem = {
      renderPathInfo: '/current/path',
    };
    expect($ctrl.isActiveSiteMapItem(siteMapItem)).toBe(true);

    siteMapItem.renderPathInfo = '/other/path';
    expect($ctrl.isActiveSiteMapItem(siteMapItem)).toBe(false);
  });
});
