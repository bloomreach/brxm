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

describe('RightSidePanelService', () => {
  let RightSidePanelService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_RightSidePanelService_) => {
      RightSidePanelService = _RightSidePanelService_;
    });
  });

  it('initializes correctly', () => {
    expect(RightSidePanelService.isLoading()).toEqual(false);
    expect(RightSidePanelService.getTitle()).toEqual('');
  });

  it('manages loading', () => {
    RightSidePanelService.startLoading();
    expect(RightSidePanelService.isLoading()).toEqual(true);

    RightSidePanelService.stopLoading();
    expect(RightSidePanelService.isLoading()).toEqual(false);
  });

  it('manages the title', () => {
    RightSidePanelService.setTitle('test title');
    expect(RightSidePanelService.getTitle()).toEqual('test title');

    RightSidePanelService.clearTitle();
    expect(RightSidePanelService.getTitle()).toEqual('');
  });
});

