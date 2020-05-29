/*
 * Copyright 2015-2020 Hippo B.V. (http://www.onehippo.com)
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

class SessionService {
  constructor($http, HstService, PathService) {
    'ngInject';

    this.$http = $http;
    this.HstService = HstService;
    this.PathService = PathService;

    this._canWrite = false; // TODO: remove completely as part of CMS-13416
    this._canManageChanges = false;
    this._canDeleteChannel = false;
    this._isCrossChannelPageCopySupported = false;
    this._initCallbacks = {};
  }

  initializeContext(channelContextPath) {
    const url = this.PathService.concatPaths(channelContextPath, '_cmssessioncontext');
    return this.$http.get(url);
  }

  initializeState(channel) {
    return this.HstService.initializeSession(channel)
      .then((privileges) => {
        if (privileges) {
          this._canWrite = true; // TODO: remove completely as part of CMS-13416
          this._canManageChanges = privileges.canManageChanges;
          this._canDeleteChannel = privileges.canDeleteChannel;
          this._isCrossChannelPageCopySupported = privileges.crossChannelPageCopySupported;
        }
        this._serveInitCallbacks();
      });
  }

  hasWriteAccess() {
    return this._canWrite;
  }

  canManageChanges() {
    return this._canManageChanges;
  }

  canDeleteChannel() {
    return this._canDeleteChannel;
  }

  isCrossChannelPageCopySupported() {
    return this._isCrossChannelPageCopySupported;
  }

  registerInitCallback(id, callback) {
    this._initCallbacks[id] = callback;
  }

  unregisterInitCallback(id) {
    delete this._initCallbacks[id];
  }

  _serveInitCallbacks() {
    Object.keys(this._initCallbacks).forEach(id => this._initCallbacks[id]());
  }
}

export default SessionService;
