/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

import hippoIframeCss from '../../../styles/string/hippo-iframe.scss';

class HippoIframeCtrl {
  constructor(
    $q,
    $log,
    $element,
    $scope,
    $translate,
    ChannelService,
    CmsService,
    ConfigService,
    DialogService,
    DomService,
    DragDropService,
    hstCommentsProcessorService,
    linkProcessorService,
    OverlayService,
    PageStructureService,
    PageMetaDataService,
    ScalingService,
    ViewportService,
    HippoIframeService
  ) {
    'ngInject';

    this.$q = $q;
    this.$log = $log;
    this.$translate = $translate;
    this.linkProcessorService = linkProcessorService;
    this.hstCommentsProcessorService = hstCommentsProcessorService;
    this.CmsService = CmsService;
    this.ConfigService = ConfigService;
    this.ChannelService = ChannelService;
    this.DialogService = DialogService;
    this.DomService = DomService;
    this.OverlayService = OverlayService;
    this.PageStructureService = PageStructureService;
    this.PageMetaDataService = PageMetaDataService;
    this.DragDropService = DragDropService;
    this.HippoIframeService = HippoIframeService;
    this.ScalingService = ScalingService;

    this.PageStructureService.clearParsedElements();

    this.iframeJQueryElement = $element.find('iframe');
    this.iframeJQueryElement.on('load', () => this.onLoad());

    HippoIframeService.initialize(this.iframeJQueryElement);

    OverlayService.init(this.iframeJQueryElement);
    OverlayService.onEditMenu((menuUuid) => {
      this.onEditMenu({ menuUuid });
    });

    ViewportService.init(this.iframeJQueryElement);

    const canvasJQueryElement = $element.find('.channel-iframe-canvas');

    ScalingService.init($element, canvasJQueryElement, this.iframeJQueryElement);
    DragDropService.init(this.iframeJQueryElement, canvasJQueryElement);

    const deleteComponentHandler = componentId => this.deleteComponent(componentId);
    CmsService.subscribe('delete-component', deleteComponentHandler);
    $scope.$on('$destroy', () => CmsService.unsubscribe('delete-component', deleteComponentHandler));

    $scope.$watch('iframe.editMode', () => {
      this._toggleOverlay();
      this._updateDragDrop();
    });
  }

  onLoad() {
    this.PageStructureService.clearParsedElements();
    this._insertCss().then(() => {
      if (this._isIframeDomPresent()) {
        this.ScalingService.onIframeReady();
        this._parseHstComments();
        this._updateDragDrop();
        this._updateChannelIfSwitched().then(() => {
          if (this._isIframeDomPresent()) {
            this._parseLinks();
            this.HippoIframeService.signalPageLoadCompleted();
          }
        });
      }
    }, () => {
      // stop progress indicator
      this.HippoIframeService.signalPageLoadCompleted();
    });
    // TODO: handle error.
    // show dialog explaining that for this channel, the CM can currently not be used,
    // and return to the channel overview upon confirming?
  }

  deleteComponent(componentId) {
    const selectedComponent = this.PageStructureService.getComponentById(componentId);
    if (!selectedComponent) {
      this.$log.warn(`Cannot delete unknown component with id:'${componentId}'`);
      return;
    }
    this._confirmDelete(selectedComponent).then(
      this._doDelete(componentId),
      () => this.PageStructureService.showComponentProperties(selectedComponent)
    );
  }

  _doDelete(componentId) {
    return () => this.PageStructureService.removeComponentById(componentId)
      .then(({ oldContainer, newContainer }) => this.DragDropService.replaceContainer(oldContainer, newContainer))
      .finally(() => this.CmsService.publish('destroy-component-properties-window'));
  }

  _confirmDelete(selectedComponent) {
    const confirm = this.DialogService.confirm()
      .textContent(this.$translate.instant('CONFIRM_DELETE_COMPONENT_MESSAGE', {
        component: selectedComponent.getLabel(),
      }))
      .ok(this.$translate.instant('DELETE'))
      .cancel(this.$translate.instant('CANCEL'));

    return this.DialogService.show(confirm);
  }

  _toggleOverlay() {
    const mode = this.editMode ? 'edit' : 'view';
    this.OverlayService.setMode(mode);
  }

  _updateDragDrop() {
    if (this.editMode) {
      this.DragDropService.enable()
      .then(() => {
        this.OverlayService.attachComponentMouseDown((e, component) => this.DragDropService.startDragOrClick(e, component));
      });
    } else {
      this.DragDropService.disable();
      this.OverlayService.detachComponentMouseDown();
    }
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

  _parseHstComments() {
    this.hstCommentsProcessorService.run(
      this._getIframeDom(),
      this.PageStructureService.registerParsedElement.bind(this.PageStructureService)
    );
    this.PageStructureService.attachEmbeddedLinks();
  }

  _updateChannelIfSwitched() {
    const channelId = this.PageMetaDataService.getChannelId();
    if (channelId && channelId !== this.ChannelService.getId()) {
      return this.ChannelService.switchToChannel(this.PageMetaDataService.getContextPath(), channelId);
    }

    return this.$q.resolve();
  }

  _isIframeDomPresent() {
    return !!this._getIframeDom();
  }

  _getIframeDom() {
    return this.iframeJQueryElement.contents()[0];
  }

  _parseLinks() {
    const iframeDom = this._getIframeDom();
    const protocolAndHost = `${iframeDom.location.protocol}//${iframeDom.location.host}`;
    const internalLinkPrefixes = this.ChannelService.getPreviewPaths().map(path => protocolAndHost + path);

    this.linkProcessorService.run(iframeDom, internalLinkPrefixes);
  }

  getContainers() {
    return this.editMode ? this.PageStructureService.getContainers() : [];
  }

  getSrc() {
    return this.HippoIframeService.getSrc();
  }

  isIframeLifted() {
    return this.HippoIframeService.isIframeLifted;
  }
}

export default HippoIframeCtrl;
