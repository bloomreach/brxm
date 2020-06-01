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

  it('gets filtered sitemap-items via the onFilter callback', () => {
    const items = ['one', 'two'];
    $ctrl.onFilter(items);

    expect($ctrl.filteredItems).toEqual(items);
  });

  it('returns the index of the active sitemap item in the filtered items', () => {
    $ctrl.filteredItems = [{ renderPathInfo: 'one' }, { renderPathInfo: 'two' }];

    HippoIframeService.getCurrentRenderPathInfo.and.returnValue('one');
    expect($ctrl.activeItemIndex).toEqual(0);

    HippoIframeService.getCurrentRenderPathInfo.and.returnValue('two');
    expect($ctrl.activeItemIndex).toEqual(1);

    HippoIframeService.getCurrentRenderPathInfo.and.returnValue('three');
    expect($ctrl.activeItemIndex).toEqual(-1);
  });
});
