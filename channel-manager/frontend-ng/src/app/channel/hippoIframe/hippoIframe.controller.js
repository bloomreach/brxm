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
    $element,
    $log,
    $q,
    $scope,
    $translate,
    ChannelService,
    ConfigService,
    CmsService,
    DialogService,
    DomService,
    DragDropService,
    HippoIframeService,
    OverlayService,
    PageMetaDataService,
    PageStructureService,
    ProjectService,
    ViewportService,
    hstCommentsProcessorService,
    linkProcessorService,
  ) {
    'ngInject';

    this.$element = $element;
    this.$log = $log;
    this.$q = $q;
    this.$scope = $scope;
    this.$translate = $translate;

    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.ConfigService = ConfigService;
    this.DialogService = DialogService;
    this.DomService = DomService;
    this.DragDropService = DragDropService;
    this.HippoIframeService = HippoIframeService;
    this.OverlayService = OverlayService;
    this.PageMetaDataService = PageMetaDataService;
    this.PageStructureService = PageStructureService;
    this.ProjectService = ProjectService;
    this.ViewportService = ViewportService;
    this.hstCommentsProcessorService = hstCommentsProcessorService;
    this.linkProcessorService = linkProcessorService;

    this.PageStructureService.clearParsedElements();

    this.iframeJQueryElement = this.$element.find('iframe');

    this.iframeJQueryElement.on('load', () => this.onLoad());

    this.HippoIframeService.initialize(this.iframeJQueryElement);

    this.OverlayService.init(this.iframeJQueryElement);

    this.OverlayService.onEditMenu((menuUuid) => {
      this.onEditMenu({ menuUuid });
    });

    this.OverlayService.onCreateContent((options) => {
      this.onCreateContent({ options });
    });

    this.OverlayService.onEditContent((contentUuid) => {
      this.onEditContent({ contentUuid });
    });

    const sheetJQueryElement = this.$element.find('.channel-iframe-sheet');
    this.ViewportService.init(sheetJQueryElement);

    const canvasJQueryElement = $element.find('.channel-iframe-canvas');
    this.DragDropService.init(this.iframeJQueryElement, canvasJQueryElement, sheetJQueryElement);

    const deleteComponentHandler = componentId => this.deleteComponent(componentId);
    this.CmsService.subscribe('delete-component', deleteComponentHandler);
    this.$scope.$on('$destroy', () => this.CmsService.unsubscribe('delete-component', deleteComponentHandler));
  }

  $onInit() {
    this.$scope.$watch('iframe.showComponentsOverlay', (value) => {
      this.OverlayService.showComponentsOverlay(value);
      this._updateDragDrop();
    });
    this.$scope.$watch('iframe.showContentOverlay', (value) => {
      this.OverlayService.showContentOverlay(value);
    });
  }

  onLoad() {
    this.PageStructureService.clearParsedElements();
    this._insertCss().then(() => {
      if (this._isIframeDomPresent()) {
        this._parseHstComments();
        this._updateDragDrop();
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

  deleteComponent(componentId) {
    const selectedComponent = this.PageStructureService.getComponentById(componentId);
    if (!selectedComponent) {
      this.$log.warn(`Cannot delete unknown component with id:'${componentId}'`);
      return;
    }
    this._confirmDelete(selectedComponent).then(
      this._doDelete(componentId),
      () => this.PageStructureService.showComponentProperties(selectedComponent),
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

  _updateDragDrop() {
    if (this.showComponentsOverlay) {
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
      this.PageStructureService.registerParsedElement.bind(this.PageStructureService),
    );
    this.PageStructureService.attachEmbeddedLinks();
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
    return this.showComponentsOverlay ? this.PageStructureService.getContainers() : [];
  }

  getContentLinks() {
    return !this.showComponentsOverlay ? this.PageStructureService.getContentLinks() : [];
  }

  getEditMenuLinks() {
    return this.showComponentsOverlay ? this.PageStructureService.getEditMenuLinks() : [];
  }

  getSrc() {
    return this.HippoIframeService.getSrc();
  }

  isIframeLifted() {
    return this.HippoIframeService.isIframeLifted;
  }

  _resetOverlayToggles() {
    this.showComponentsOverlay = false;
    this.showContentOverlay = true;
  }
}

export default HippoIframeCtrl;
