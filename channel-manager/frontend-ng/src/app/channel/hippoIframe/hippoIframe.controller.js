/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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
import iframeBundle from '../../iframe';

class HippoIframeCtrl {
  constructor(
    $element,
    $rootScope,
    CmsService,
    ComponentRenderingService,
    ContainerService,
    DomService,
    DragDropService,
    EditComponentService,
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
    this.$rootScope = $rootScope;
    this.CmsService = CmsService;
    this.ComponentRenderingService = ComponentRenderingService;
    this.ContainerService = ContainerService;
    this.DomService = DomService;
    this.DragDropService = DragDropService;
    this.EditComponentService = EditComponentService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
    this.HstComponentService = HstComponentService;
    this.OverlayService = OverlayService;
    this.PageStructureService = PageStructureService;
    this.PickerService = PickerService;
    this.RenderingService = RenderingService;
    this.SpaService = SpaService;
    this.ViewportService = ViewportService;

    this.iframeJQueryElement = this.$element.find('iframe');
    this._onSpaReady = this._onSpaReady.bind(this);
  }

  $onInit() {
    this.CmsService.subscribe('render-component', this._renderComponent, this);
    this.CmsService.subscribe('delete-component', this._deleteComponent, this);
    this._offClick = this.DragDropService.onClick(this._clickComponent.bind(this));
    this._offDrop = this.DragDropService.onDrop(this._moveComponent.bind(this));

    this.iframeJQueryElement.on('load', () => this.onLoad());
    this._offSdkReady = this.$rootScope.$on('spa:ready', this._onSpaReady);

    const canvasJQueryElement = this.$element.find('.channel-iframe-canvas');
    const sheetJQueryElement = this.$element.find('.channel-iframe-sheet');

    this.PageStructureService.clearParsedElements();
    this.HippoIframeService.initialize(this.$element, this.iframeJQueryElement);
    this.OverlayService.init(this.iframeJQueryElement);
    this.OverlayService.onEditMenu(menuUuid => this.onEditMenu({ menuUuid }));
    this.OverlayService.onSelectDocument(this._selectDocument.bind(this));
    this.ViewportService.init(sheetJQueryElement);
    this.DragDropService.init(this.iframeJQueryElement, canvasJQueryElement, sheetJQueryElement);
    this.SpaService.init(this.iframeJQueryElement);
    this.RenderingService.init(this.iframeJQueryElement);
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
    this._offClick();
    this._offDrop();
    this._offSdkReady();
  }

  async onLoad() {
    if (!this._isIframeAccessible()) {
      return;
    }

    // TODO: call penpal connect to child
    await this.DomService.addScript(
      this.iframeJQueryElement[0].contentWindow,
      this.DomService.getAssetUrl(iframeBundle),
    );

    this.$rootScope.$broadcast('hippo-iframe:load');

    if (this.SpaService.initLegacy()) {
      return;
    }

    this.RenderingService.createOverlay();
  }

  async _onSpaReady() {
    if (this._isIframeAccessible()) {
      return;
    }

    this.$rootScope.$broadcast('hippo-iframe:load');
  }

  /**
   * Checks whether the iframe DOM is accessible from the Channel Manager.
   * At first, it tries to access the document from the content window,
   * and in case if it a cross-origin website, an exception will be thrown.
   * In case, when the document was not loaded due to some error, the document's body will be empty.
   *
   * @see https://stackoverflow.com/a/12381504
   */
  _isIframeAccessible() {
    let html;

    try {
      const { document } = this.iframeJQueryElement[0].contentWindow;
      html = document.body.innerHTML;
    } catch (error) {} // eslint-disable-line no-empty

    return html != null;
  }

  _renderComponent(componentId, propertiesMap) {
    this.ComponentRenderingService.renderComponent(componentId, propertiesMap);
  }

  _clickComponent(component) {
    this.EditComponentService.startEditing(component);
  }

  _moveComponent([component, targetContainer, targetContainerNextComponent]) {
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
      .then(({ path }) => this._onPathPicked(component, parameterName, path, parameterBasePath));
  }

  _onPathPicked(component, parameterName, path, parameterBasePath) {
    const componentId = component.getId();
    const componentName = component.getLabel();
    const componentVariant = component.getRenderVariant();

    return this.HstComponentService.setPathParameter(
      componentId, componentVariant, parameterName, path, parameterBasePath,
    )
      .then(() => {
        this.ComponentRenderingService.renderComponent(componentId);
        this.FeedbackService.showNotification('NOTIFICATION_DOCUMENT_SELECTED_FOR_COMPONENT', { componentName });
      })
      .catch((response) => {
        const defaultErrorKey = 'ERROR_DOCUMENT_SELECTED_FOR_COMPONENT';
        const defaultErrorParams = { componentName };
        const errorMap = { ITEM_ALREADY_LOCKED: 'ERROR_DOCUMENT_SELECTED_FOR_COMPONENT_ALREADY_LOCKED' };

        this.FeedbackService.showErrorResponse(
          response && response.data, defaultErrorKey, errorMap, defaultErrorParams,
        );

        // probably the container got locked by another user, so reload the page to show new locked containers
        this.HippoIframeService.reload();
      });
  }
}

export default HippoIframeCtrl;
