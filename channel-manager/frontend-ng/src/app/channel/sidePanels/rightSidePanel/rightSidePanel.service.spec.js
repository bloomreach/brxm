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
  let $timeout;
  let RightSidePanelService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$timeout_, _RightSidePanelService_) => {
      $timeout = _$timeout_;
      RightSidePanelService = _RightSidePanelService_;
    });
  });

  it('initializes correctly', () => {
    expect(RightSidePanelService.isLoading()).toBe(false);
    expect(RightSidePanelService.getTitle()).toEqual('');
    expect(RightSidePanelService.getContext()).toEqual('');
    expect(RightSidePanelService.loadingPromise).toBeNull();
  });

  describe('startLoading', () => {
    it('starts after a timeout', () => {
      RightSidePanelService.startLoading();

      expect(RightSidePanelService.isLoading()).toBe(false);
      expect(RightSidePanelService.loadingPromise).not.toBeNull();

      $timeout.flush();
      expect(RightSidePanelService.loadingPromise).toBeNull();
      expect(RightSidePanelService.isLoading()).toBe(true);
    });

    it('does not set a timeout when already loading', () => {
      RightSidePanelService.startLoading();
      $timeout.flush();

      RightSidePanelService.startLoading();
      expect(RightSidePanelService.loadingPromise).toBeNull();
      expect(RightSidePanelService.isLoading()).toBe(true);
    });
  });

  describe('stopLoading', () => {
    it('stops directly', () => {
      RightSidePanelService.startLoading();
      $timeout.flush();
      RightSidePanelService.stopLoading();
      expect(RightSidePanelService.loadingPromise).toBeNull();
      expect(RightSidePanelService.isLoading()).toBe(false);
    });

    it('cancels the timeout', () => {
      RightSidePanelService.startLoading();
      RightSidePanelService.stopLoading();
      expect(RightSidePanelService.loadingPromise).toBeNull();
      expect(RightSidePanelService.isLoading()).toBe(false);

      $timeout.flush();
      expect(RightSidePanelService.isLoading()).toBe(false);

      $timeout.verifyNoPendingTasks();
    });
  });

  it('manages the title', () => {
    RightSidePanelService.setTitle('test title');
    expect(RightSidePanelService.getTitle()).toEqual('test title');

    RightSidePanelService.clearTitle();
    expect(RightSidePanelService.getTitle()).toEqual('');
  });

  it('manages the context', () => {
    RightSidePanelService.setContext('test');
    expect(RightSidePanelService.getContext()).toEqual('test');

    RightSidePanelService.clearContext();
    expect(RightSidePanelService.getContext()).toEqual('');
  });
});

