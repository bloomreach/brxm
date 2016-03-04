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

describe('PageMetaDataService', function () {
  'use strict';

  var PageMetaDataService;

  beforeEach(function () {
    module('hippo-cm.channel.page');

    inject(function (_PageMetaDataService_) {
      PageMetaDataService = _PageMetaDataService_;
    });
  });

  it('has no meta-data initially', function () {
    expect(PageMetaDataService.get()).toEqual({});
  });

  it('adds data', function () {
    PageMetaDataService.add({
      foo: 1,
      bar: 2,
    });
    PageMetaDataService.add({
      foo: 3,
    });
    expect(PageMetaDataService.get()).toEqual({
      foo: 3,
      bar: 2,
    });
  });

  it('clears data', function () {
    PageMetaDataService.add({
      test: 1,
    });
    PageMetaDataService.clear();
    expect(PageMetaDataService.get()).toEqual({});
  });

  it('returns the latest mount id', function () {
    PageMetaDataService.add({
      'HST-Mount-Id': 'testMountId',
    });
    expect(PageMetaDataService.getMountId()).toEqual('testMountId');

    PageMetaDataService.add({
      'HST-Mount-Id': 'anotherMountId',
    });
    expect(PageMetaDataService.getMountId()).toEqual('anotherMountId');
  });

  it('returns whether there is preview configuration', function () {
    PageMetaDataService.add({
      'HST-Site-HasPreviewConfig': 'true',
    });
    expect(PageMetaDataService.hasPreviewConfiguration()).toEqual(true);
  });

  it('returns whether there is no preview configuration', function () {
    expect(PageMetaDataService.hasPreviewConfiguration()).toEqual(false);
    PageMetaDataService.add({
      'HST-Site-HasPreviewConfig': 'false',
    });
    expect(PageMetaDataService.hasPreviewConfiguration()).toEqual(false);
  });

});
