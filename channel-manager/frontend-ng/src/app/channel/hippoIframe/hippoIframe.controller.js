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
    CmsService,
    ContainerService,
    DragDropService,
    FeedbackService,
    HippoIframeService,
    HstComponentService,
    OverlayService,
    PageStructureService,
    PickerService,
    RenderingService,
    SpaService,
    ViewportService,
  ) {
    'ngInject';

    this.$element = $element;

    this.CmsService = CmsService;
    this.ContainerService = ContainerService;
    this.DragDropService = DragDropService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
    this.HstComponentService = HstComponentService;
    this.OverlayService = OverlayService;
    this.PageStructureService = PageStructureService;
    this.PickerService = PickerService;
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

    this.OverlayService.onSelectDocument(this._selectDocument.bind(this));

    const sheetJQueryElement = this.$element.find('.channel-iframe-sheet');
    this.ViewportService.init(sheetJQueryElement);

    const canvasJQueryElement = $element.find('.channel-iframe-canvas');
    this.DragDropService.init(this.iframeJQueryElement, canvasJQueryElement, sheetJQueryElement);

    this.SpaService.init(this.iframeJQueryElement);
    this.RenderingService.init(this.iframeJQueryElement);
  }

  $onInit() {
    this.CmsService.subscribe('render-component', this._renderComponent, this);
    this.CmsService.subscribe('delete-component', this._deleteComponent, this);
    this.DragDropService.onDrop(this._moveComponent.bind(this));
  }

  $onChanges(changes) {
    if (changes.showComponentsOverlay) {
      this.OverlayService.showComponentsOverlay(changes.showComponentsOverlay.currentValue);
      if (this.HippoIframeService.pageLoaded) {
        this.RenderingService.updateDragDrop();
      }
    }

    if (changes.showContentOverlay) {
      this.OverlayService.showContentOverlay(changes.showContentOverlay.currentValue);
    }
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

  _selectDocument(component, parameterName, parameterValue, pickerConfig, parameterBasePath) {
    return this.PickerService.pickPath(pickerConfig, parameterValue)
      .then(({ path }) => {
        const componentId = component.getId();
        const componentName = component.getLabel();
        const componentVariant = component.getRenderVariant();

        return this.HstComponentService.setPathParameter(componentId, componentVariant, parameterName, path, parameterBasePath)
          .then(() => {
            this.PageStructureService.renderComponent(componentId);
            this.FeedbackService.showNotification('NOTIFICATION_DOCUMENT_SELECTED_FOR_COMPONENT', { componentName });
          })
          .catch((response) => {
            const defaultErrorKey = 'ERROR_DOCUMENT_SELECTED_FOR_COMPONENT';
            const defaultErrorParams = { componentName };
            const errorMap = { ITEM_ALREADY_LOCKED: 'ERROR_DOCUMENT_SELECTED_FOR_COMPONENT_ALREADY_LOCKED' };

            this.FeedbackService.showErrorResponse(response && response.data, defaultErrorKey, errorMap, defaultErrorParams);

            // probably the container got locked by another user, so reload the page to show new locked containers
            this.HippoIframeService.reload();
          });
      });
  }
}

export default HippoIframeCtrl;
