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

import hippoIframeCss from '../../../../styles/string/hippo-iframe.scss';

class ChannelRenderingService {
  constructor(
    $q,
    ChannelService,
    CmsService,
    ConfigService,
    DomService,
    DragDropService,
    HippoIframeService,
    HstCommentsProcessorService,
    LinkProcessorService,
    OverlayService,
    PageMetaDataService,
    PageStructureService,
    ProjectService,
  ) {
    'ngInject';

    this.$q = $q;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.ConfigService = ConfigService;
    this.DomService = DomService;
    this.DragDropService = DragDropService;
    this.HippoIframeService = HippoIframeService;
    this.HstCommentsProcessorService = HstCommentsProcessorService;
    this.LinkProcessorService = LinkProcessorService;
    this.OverlayService = OverlayService;
    this.PageMetaDataService = PageMetaDataService;
    this.PageStructureService = PageStructureService;
    this.ProjectService = ProjectService;
  }

  init(iframeJQueryElement) {
    this.iframeJQueryElement = iframeJQueryElement;
  }

  createOverlay() {
    this.PageStructureService.clearParsedElements();
    this._insertCss().then(() => {
      if (this._isIframeDomPresent()) {
        this._parseHstComments();
        this.updateDragDrop();
        this._updateChannelIfSwitched();
        this._parseLinks();
        this.HippoIframeService.signalPageLoadCompleted();
      }
    }, () => {
      // stop progress indicator
      this.HippoIframeService.signalPageLoadCompleted();
    });
    // TODO: handle error.
    // show dialog explaining that for this channel, the CM can currently not be used,
    // and return to the channel overview upon confirming?
  }

  _insertCss() {
    try {
      const iframeDom = this._getIframeDom();
      if (!iframeDom) {
        return this.$q.reject();
      }
      const iframeWindow = iframeDom.defaultView;
      this.DomService.addCss(iframeWindow, hippoIframeCss);
      return this.$q.resolve();
    } catch (e) {
      return this.$q.reject();
    }
  }

  _isIframeDomPresent() {
    return !!this._getIframeDom();
  }

  _getIframeDom() {
    return this.iframeJQueryElement.contents()[0];
  }

  _parseHstComments() {
    this.HstCommentsProcessorService.run(
      this._getIframeDom(),
      this.PageStructureService.registerParsedElement.bind(this.PageStructureService),
    );
    this.PageStructureService.attachEmbeddedLinks();
  }

  updateDragDrop() {
    if (this.OverlayService.isComponentsOverlayDisplayed) {
      this.DragDropService.enable()
        .then(() => {
          this.OverlayService.attachComponentMouseDown((e, component) => this.DragDropService.startDragOrClick(e, component));
        });
    } else {
      this.DragDropService.disable();
      this.OverlayService.detachComponentMouseDown();
    }
  }

  _updateChannelIfSwitched() {
    const channelToLoad = this._getChannelToLoad();
    if (channelToLoad !== null) {
      const path = this.PageMetaDataService.getPathInfo();
      this.CmsService.publish('load-channel', channelToLoad, path);
    }
  }

  _getChannelToLoad() {
    const channelIdFromService = this.ChannelService.getId();
    const channelIdFromPage = this.PageMetaDataService.getChannelId();

    if (channelIdFromService === channelIdFromPage) {
      return null;
    }

    if (this.ConfigService.projectsEnabled) {
      const projectId = this.ChannelService.selectedProjectId;
      return this.ProjectService.getBaseChannelId(projectId ? channelIdFromService : channelIdFromPage);
    }
    return channelIdFromPage;
  }

  _parseLinks() {
    const iframeDom = this._getIframeDom();
    const protocolAndHost = `${iframeDom.location.protocol}//${iframeDom.location.host}`;
    const internalLinkPrefixes = this.ChannelService.getPreviewPaths().map(path => protocolAndHost + path);

    this.LinkProcessorService.run(iframeDom, internalLinkPrefixes);
  }
}

export default ChannelRenderingService;
