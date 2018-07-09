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

class RenderingService {
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
      this._parseHstComments();
      this.updateDragDrop();
      this._updateChannelIfSwitched();
      this._parseLinks();
      this.HippoIframeService.signalPageLoadCompleted();
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
      if (!this.DomService.hasIframeDocument(this.iframeJQueryElement)) {
        // sometimes the iframe does not have a document, e.g. when viewing inline PDFs
        return this.$q.reject();
      }
      const iframeWindow = this.DomService.getIframeWindow(this.iframeJQueryElement);
      this.DomService.addCss(iframeWindow, hippoIframeCss);
      return this.$q.resolve();
    } catch (e) {
      return this.$q.reject();
    }
  }

  _parseHstComments() {
    this.HstCommentsProcessorService.run(
      this.DomService.getIframeDocument(this.iframeJQueryElement),
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
    const channelIdFromService = this.ChannelService.getId();
    const channelIdFromPage = this.PageMetaDataService.getChannelId();

    if (channelIdFromService !== channelIdFromPage) {
      if (this.ProjectService.isBranch() && !this.ProjectService.hasBranchOfProject(channelIdFromPage)) {
        // Current channel is a branch, but new channel has no branch of that project
        // therefore load core
        this.ChannelService.initializeChannel(channelIdFromPage, this.ProjectService.core.id);
      } else {
        // otherwise load new channel within current project
        this.ChannelService.initializeChannel(channelIdFromPage, this.ProjectService.selectedProject.id);
      }
    }
  }

  _parseLinks() {
    const iframeDocument = this.DomService.getIframeDocument(this.iframeJQueryElement);
    const protocolAndHost = `${iframeDocument.location.protocol}//${iframeDocument.location.host}`;
    const internalLinkPrefixes = this.ChannelService.getPreviewPaths().map(path => protocolAndHost + path);

    this.LinkProcessorService.run(iframeDocument, internalLinkPrefixes);
  }
}

export default RenderingService;
