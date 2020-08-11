/*
 * Copyright 2020 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import { Injectable } from '@angular/core';

import { AppState } from './app-state';

@Injectable({
  providedIn: 'root',
})
export class NavigatorService {
  constructor(private readonly state: AppState) {
    this.registerPopHistoryStateHandler();
  }

  navigate(replaceState = true): void {
    if (replaceState) {
      this.state.historyReplaceStateCount++;
      window.history.replaceState(
        { replaceStateCount: this.state.historyReplaceStateCount },
        '',
        `/some-url${this.state.historyReplaceStateCount};replace-state`,
      );

      return;
    }

    this.state.historyPushStateCount++;
    window.history.pushState(
      { pushStateCount: this.state.historyPushStateCount },
      '',
      `/some-url${this.state.historyPushStateCount};push-state`,
    );
  }

  reloadPage(): void {
    window.location.href = `${window.location.href}/something`;
  }

  private registerPopHistoryStateHandler(): void {
    window.addEventListener('popstate', event => {
      const state = event.state;

      if (state && state.replaceStateCount) {
        this.state.historyReplaceStateCount = state.replaceStateCount;

        return;
      }

      if (state && state.pushStateCount) {
        this.state.historyPushStateCount = state.pushStateCount;

        return;
      }

      this.state.historyReplaceStateCount = 0;
      this.state.historyPushStateCount = 0;
    });
  }
}
