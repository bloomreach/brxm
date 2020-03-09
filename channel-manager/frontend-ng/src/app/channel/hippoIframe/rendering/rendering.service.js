/*
 * Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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

import hippoIframeCss from '../../../../styles/string/hippo-iframe.scss?url';

const OVERLAY_CREATED_EVENT_NAME = 'overlay-created';
const INJECTED_CSS_CLASS = 'hippo-css';

class RenderingService {
  constructor(
    $log,
    $q,
    $rootScope,
    DomService,
    Emittery,
    HippoIframeService,
    LinkProcessorService,
    OverlayService,
    PageStructureService,
    ScrollService,
  ) {
    'ngInject';

    this.$log = $log;
    this.$q = $q;
    this.$rootScope = $rootScope;
    this.DomService = DomService;
    this.HippoIframeService = HippoIframeService;
    this.LinkProcessorService = LinkProcessorService;
    this.OverlayService = OverlayService;
    this.PageStructureService = PageStructureService;
    this.ScrollService = ScrollService;

    this.emitter = new Emittery();
  }

  init(iframeJQueryElement) {
    this.iframeJQueryElement = iframeJQueryElement;
  }

  onOverlayCreated(callback) {
    return this._on(OVERLAY_CREATED_EVENT_NAME, callback);
  }

  _on(eventName, callback) {
    return this.emitter.on(eventName, argument => this.$rootScope.$apply(() => callback(argument)));
  }

  createOverlay() {
    if (this.creatingOverlay) {
      return this.creatingOverlay;
    }

    this.ScrollService.saveScrollPosition();
    this.PageStructureService.clearParsedElements();
    this.OverlayService.clear();

    this.creatingOverlay = this._insertCss()
      .then(() => this.PageStructureService.parseElements())
      .then(() => {
        this._parseLinks();
        this.ScrollService.restoreScrollPosition();

        return this.emitter.emit(OVERLAY_CREATED_EVENT_NAME);
      })
      .finally(() => {
        this.HippoIframeService.signalPageLoadCompleted();
        delete this.creatingOverlay;
      });
    // TODO: handle error.
    // show dialog explaining that for this channel, the CM can currently not be used,
    // and return to the channel overview upon confirming?

    return this.creatingOverlay;
  }

  _insertCss() {
    try {
      if (!this.DomService.hasIframeDocument(this.iframeJQueryElement)) {
        // sometimes the iframe does not have a document, e.g. when viewing inline PDFs
        return this.$q.reject();
      }

      const iframeWindow = this.DomService.getIframeWindow(this.iframeJQueryElement);
      if (this.DomService.hasCssLink(iframeWindow, INJECTED_CSS_CLASS)) {
        return this.$q.resolve();
      }

      return this.DomService.addCssLinks(iframeWindow, [hippoIframeCss], INJECTED_CSS_CLASS);
    } catch (e) {
      return this.$q.reject();
    }
  }

  _parseLinks() {
    const iframeDocument = this.DomService.getIframeDocument(this.iframeJQueryElement);
    this.LinkProcessorService.run(iframeDocument);
  }
}

export default RenderingService;
