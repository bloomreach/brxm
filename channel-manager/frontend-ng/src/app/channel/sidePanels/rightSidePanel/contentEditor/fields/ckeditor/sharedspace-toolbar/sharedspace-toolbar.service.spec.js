/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

describe('Sharedspace toolbar service', () => {
  let SharedSpaceToolbarService;

  function mockTriggerCallback() {
    angular.noop('mock');
  }

  function init() {
    SharedSpaceToolbarService.registerTriggerCallback(mockTriggerCallback);
  }

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_SharedSpaceToolbarService_) => {
      SharedSpaceToolbarService = _SharedSpaceToolbarService_;
    });
  });

  it('should register trigger callback', () => {
    expect(SharedSpaceToolbarService.triggerCallback).toEqual(angular.noop);
    init();
    expect(SharedSpaceToolbarService.triggerCallback).toEqual(mockTriggerCallback);
  });

  it('should call trigger callback with true (show toolbar)', () => {
    init();
    spyOn(SharedSpaceToolbarService, 'triggerCallback');
    SharedSpaceToolbarService.showToolbar({
      hasBottomToolbar: true,
    });
    expect(SharedSpaceToolbarService.triggerCallback).toHaveBeenCalledWith(true, { hasBottomToolbar: true });
  });

  it('should call trigger callback with false (hide toolbar)', () => {
    init();
    spyOn(SharedSpaceToolbarService, 'triggerCallback');
    SharedSpaceToolbarService.hideToolbar();
    expect(SharedSpaceToolbarService.triggerCallback).toHaveBeenCalledWith(false);
  });
});
