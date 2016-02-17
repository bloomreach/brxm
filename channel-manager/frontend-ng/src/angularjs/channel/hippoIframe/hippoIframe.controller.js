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

function constructUrl(location, ...fragments) {
  let url = `${location.protocol}//${location.host}`;
  fragments.forEach((fragment) => {
    if (angular.isString(fragment) && fragment.length) {
      const urlAndSlash = url.charAt(url.length - 1) === '/' ? url : `${url}/`;
      const noSlashAndFragment = fragment.charAt(0) === '/' ? fragment.substring(1) : fragment;
      url = urlAndSlash + noSlashAndFragment;
    }
  });
  return url;
}

export class HippoIframeCtrl {
  constructor(linkProcessorService, hstCommentsProcessorService, HST_CONSTANT, ChannelService) {
    'ngInject';

    this.linkProcessorService = linkProcessorService;
    this.hstCommentsProcessorService = hstCommentsProcessorService;
    this.HST = HST_CONSTANT;
    this.ChannelService = ChannelService;
  }

  onLoad() {
    this.parseHstComments();
    this.parseLinks();
  }

  parseHstComments() {
    const processHstComment = (commentElement, json) => {
      switch (json[this.HST.TYPE]) {
        case this.HST.TYPE_PAGE_META_DATA:
          const channelId = json[this.HST.CHANNEL_ID];
          if (channelId !== this.ChannelService.getId()) {
            this.ChannelService.switchToChannel(channelId);
          }
          break;
        default:
          break;
      }
    };
    const iframeDom = this.iframe.contents()[0];

    this.hstCommentsProcessorService.run(iframeDom, processHstComment);
  }

  parseLinks() {
    const iframeDom = this.iframe.contents()[0];
    const channel = this.ChannelService.channel;
    const internalLinkPrefix = constructUrl(iframeDom.location, channel.contextPath, channel.cmsPreviewPrefix);

    this.linkProcessorService.run(iframeDom, internalLinkPrefix);
  }

}
