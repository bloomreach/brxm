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

export class HippoIframeCtrl {
  constructor() {
    'ngInject';
  }

  onLoad(iframeWindow) {
    this.processNewUrl(iframeWindow.location.pathname);
  }

  processNewUrl(/* iframeUrl */) {

    // TODO: check if new URL is in current channel. If not, signal the channel switch to the outside world.
    // We may want HST to include the mount ID (and other meta data) in a comment, so we can parse that rather than
    // digesting arbitrary URLs here.

  }

}
