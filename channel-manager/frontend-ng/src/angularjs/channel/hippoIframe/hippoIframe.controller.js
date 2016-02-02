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
  constructor(hstCommentsProcessorService, HST_CONSTANT) {
    'ngInject';

    this.hstCommentsProcessorService = hstCommentsProcessorService;
    this.HST = HST_CONSTANT;
  }

  onLoad() {
    this.parseHstComments();
  }

  parseHstComments() {
    const processHstComment = (commentElement, json) => {
      switch (json[this.HST.TYPE]) {
        case this.HST.TYPE_PAGE_META_DATA:
          this.path = json[this.HST.PATH_INFO];
          if (json[this.HST.MOUNT_ID] !== this.mountId) {
            this.onChannelSwitch({ mountId: json[this.HST.MOUNT_ID] });
          }
          break;
        default:
          break;
      }
    };
    const iframeDom = this.iframe.contents()[0];

    this.hstCommentsProcessorService.run(iframeDom, processHstComment);
  }

}
