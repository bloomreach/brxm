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

describe('PageMetaDataService', () => {
  'use strict';

  let PageMetaDataService;

  beforeEach(() => {
    module('hippo-cm.channel.page');

    inject((_PageMetaDataService_) => {
      PageMetaDataService = _PageMetaDataService_;
    });
  });

  it('has no meta-data initially', () => {
    expect(PageMetaDataService.get()).toEqual({});
  });

  it('adds data', () => {
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

  it('clears data', () => {
    PageMetaDataService.add({
      test: 1,
    });
    PageMetaDataService.clear();
    expect(PageMetaDataService.get()).toEqual({});
  });
});
