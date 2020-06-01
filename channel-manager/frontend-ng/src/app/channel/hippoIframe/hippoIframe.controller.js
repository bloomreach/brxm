/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

import './hippoIframe.scss';

class HippoIframeCtrl {
  constructor(
    $element,
    $log,
    $scope,
    $translate,
    CmsService,
    ContainerService,
    DialogService,
    DragDropService,
    HippoIframeService,
    OverlayService,
    PageStructureService,
    RenderingService,
    SpaService,
    ViewportService,
  ) {
    'ngInject';

    this.$element = $element;
    this.$log = $log;
    this.$scope = $scope;
    this.$translate = $translate;

    this.CmsService = CmsService;
    this.ContainerService = ContainerService;
    this.DialogService = DialogService;
    this.DragDropService = DragDropService;
    this.HippoIframeService = HippoIframeService;
    this.OverlayService = OverlayService;
    this.PageStructureService = PageStructureService;
    this.RenderingService = RenderingService;
    this.SpaService = SpaService;
    this.ViewportService = ViewportService;

    this.PageStructureService.clearParsedElements();

    this.iframeJQueryElement = this.$element.find('iframe');

    this.iframeJQueryElement.on('load', () => this.onLoad());

    this.HippoIframeService.initialize(this.$element, this.iframeJQueryElement);

    this.OverlayService.init(this.iframeJQueryElement);

    this.OverlayService.onEditMenu((menuUuid) => {
      this.onEditMenu({ menuUuid });
    });

    const sheetJQueryElement = this.$element.find('.channel-iframe-sheet');
    this.ViewportService.init(sheetJQueryElement);

    const canvasJQueryElement = $element.find('.channel-iframe-canvas');
    this.DragDropService.init(this.iframeJQueryElement, canvasJQueryElement, sheetJQueryElement);

    this.SpaService.init(this.iframeJQueryElement);
    this.RenderingService.init(this.iframeJQueryElement);
  }

  $onInit() {
    this.$scope.$watch('iframe.showComponentsOverlay', (value) => {
      this.OverlayService.showComponentsOverlay(value);
      if (this.HippoIframeService.pageLoaded) {
        this.RenderingService.updateDragDrop();
      }
    });
    this.$scope.$watch('iframe.showContentOverlay', (value) => {
      this.OverlayService.showContentOverlay(value);
    });

    this.CmsService.subscribe('render-component', this._renderComponent, this);
    this.CmsService.subscribe('delete-component', this._deleteComponent, this);
    this.DragDropService.onDrop(this._moveComponent.bind(this));
  }

  $onDestroy() {
    this.CmsService.unsubscribe('render-component', this._renderComponent, this);
    this.CmsService.unsubscribe('delete-component', this._deleteComponent, this);
    this.DragDropService.offDrop();
  }

  onLoad() {
    if (this.SpaService.detectSpa()) {
      this.SpaService.initSpa();
    } else {
      this.RenderingService.createOverlay();
    }
  }

  _renderComponent(componentId, propertiesMap) {
    if (!this.SpaService.renderComponent(componentId, propertiesMap)) {
      this.PageStructureService.renderComponent(componentId, propertiesMap);
    }
  }

  _moveComponent(component, targetContainer, targetContainerNextComponent) {
    return this.ContainerService.moveComponent(component, targetContainer, targetContainerNextComponent);
  }

  _deleteComponent(componentId) {
    this.ContainerService.deleteComponent(componentId);
  }

  getSrc() {
    return this.HippoIframeService.getSrc();
  }

  isIframeLifted() {
    return this.HippoIframeService.isIframeLifted;
  }
}

export default HippoIframeCtrl;
