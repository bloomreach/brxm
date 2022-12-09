/*
 * Copyright 2018-2022 Hippo B.V. (http://www.onehippo.com)
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
  let PageStructureService;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.siteMapListing');

    inject(($componentController, _PageStructureService_) => {
      $ctrl = $componentController('siteMapListing');
      PageStructureService = _PageStructureService_;
    });

    spyOn(PageStructureService, 'getPage');
  });

  it('returns the currentRenderPathInfo from service', () => {
    $ctrl.getCurrentPathInfo();
    expect(PageStructureService.getPage).toHaveBeenCalled();
  });
});
