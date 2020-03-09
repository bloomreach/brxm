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
    $log,
    $rootScope,
    CmsService,
    CommunicationService,
    ComponentRenderingService,
    ContainerService,
    CreateContentService,
    DomService,
    DragDropService,
    EditComponentService,
    EditContentService,
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
    this.$log = $log;
    this.$rootScope = $rootScope;
    this.CmsService = CmsService;
    this.CommunicationService = CommunicationService;
    this.ComponentRenderingService = ComponentRenderingService;
    this.ContainerService = ContainerService;
    this.CreateContentService = CreateContentService;
    this.DomService = DomService;
    this.DragDropService = DragDropService;
    this.EditComponentService = EditComponentService;
    this.EditContentService = EditContentService;
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
    this.onLoad = this.onLoad.bind(this);
    this._onUnload = this._onUnload.bind(this);
    this._onNewHeadContributions = this._onNewHeadContributions.bind(this);
    this._onComponentClick = this._onComponentClick.bind(this);
    this._onComponentMove = this._onComponentMove.bind(this);
    this._onDocumentCreate = this._onDocumentCreate.bind(this);
    this._onDocumentEdit = this._onDocumentEdit.bind(this);
    this._onDocumentSelect = this._onDocumentSelect.bind(this);
  }

  $onInit() {
    this.CmsService.subscribe('render-component', this._renderComponent, this);
    this.CmsService.subscribe('delete-component', this._deleteComponent, this);

    this.iframeJQueryElement.on('load', this.onLoad);
    this._offEditMenu = this.$rootScope.$on('menu:edit', (event, menuUuid) => this.onEditMenu({ menuUuid }));
    this._offComponentClick = this.$rootScope.$on('component:click', this._onComponentClick);
    this._offComponentMove = this.$rootScope.$on('iframe:component:move', this._onComponentMove);
    this._offSdkReady = this.$rootScope.$on('spa:ready', this._onSpaReady);
    this._offSdkUnload = this.$rootScope.$on('iframe:unload', this._onUnload);
    this._offNewHeadContributions = this.$rootScope.$on(
      'hippo-iframe:new-head-contributions',
      this._onNewHeadContributions,
    );
    this._offDocumentCreate = this.$rootScope.$on('document:create', this._onDocumentCreate);
    this._offDocumentEdit = this.$rootScope.$on('document:edit', this._onDocumentEdit);
    this._offDocumentSelect = this.$rootScope.$on('document:select', this._onDocumentSelect);

    const canvasJQueryElement = this.$element.find('.channel-iframe-canvas');
    const sheetJQueryElement = this.$element.find('.channel-iframe-sheet');

    this.PageStructureService.clearParsedElements();
    this.HippoIframeService.initialize(this.$element, this.iframeJQueryElement);
    this.OverlayService.init(this.iframeJQueryElement);
    this.ViewportService.init(sheetJQueryElement);
    this.DragDropService.init(this.iframeJQueryElement, canvasJQueryElement, sheetJQueryElement);
    this.SpaService.init(this.iframeJQueryElement);
    this.RenderingService.init(this.iframeJQueryElement);
  }

  $onChanges(changes) {
    if (changes.showComponentsOverlay) {
      this.OverlayService.toggleComponentsOverlay(changes.showComponentsOverlay.currentValue);
    }

    if (changes.showContentOverlay) {
      this.OverlayService.toggleContentsOverlay(changes.showContentOverlay.currentValue);
    }
  }

  $onDestroy() {
    this.CommunicationService.disconnect();
    this.OverlayService.destroy();
    this.SpaService.destroy();
    this.CmsService.unsubscribe('render-component', this._renderComponent, this);
    this.CmsService.unsubscribe('delete-component', this._deleteComponent, this);
    this._offEditMenu();
    this._offComponentClick();
    this._offComponentMove();
    this._offSdkReady();
    this._offSdkUnload();
    this._offNewHeadContributions();
    this._offDocumentCreate();
    this._offDocumentEdit();
    this._offDocumentSelect();
  }

  async onLoad() {
    if (!this._isIframeAccessible()) {
      return;
    }

    const target = this.iframeJQueryElement[0];
    const connection = this.CommunicationService.connect({ target });

    await this.DomService.addScript(
      target.contentWindow,
      this.DomService.getAssetUrl(iframeBundle),
    );
    await connection;

    this.$rootScope.$emit('hippo-iframe:load');

    if (this.SpaService.initLegacy()) {
      return;
    }

    this.RenderingService.createOverlay();
  }

  _onUnload() {
    this.CommunicationService.disconnect();
  }

  async _onSpaReady() {
    if (this._isIframeAccessible()) {
      return;
    }

    const target = this.iframeJQueryElement[0];
    const connection = this.CommunicationService.connect({ target, origin: this.SpaService.getOrigin() });

    await this.SpaService.inject(this.DomService.getAssetUrl(iframeBundle));
    await connection;

    this.$rootScope.$emit('hippo-iframe:load');
  }

  _onNewHeadContributions(event, component) {
    this.$log.info(`Updated '${component.getLabel()}' component needs additional head contributions.`);
    this.HippoIframeService.reload();
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

  _onComponentClick(event, component) {
    this.EditComponentService.startEditing(component);
  }

  _onComponentMove(event, { componentId, containerId, nextComponentId }) {
    const page = this.PageStructureService.getPage();
    if (!page) {
      return;
    }
    const component = page.getComponentById(componentId);
    const container = page.getContainerById(containerId);
    const nextComponent = page.getComponentById(nextComponentId);

    this.ContainerService.moveComponent(component, container, nextComponent);
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

  _onDocumentCreate(event, data) {
    this.CreateContentService.start(data);
  }

  _onDocumentEdit(event, uuid) {
    this.CmsService.reportUsageStatistic('CMSChannelsEditContent');

    this.EditContentService.startEditing(uuid);
  }

  _onDocumentSelect(event, data) {
    this.CmsService.reportUsageStatistic('PickContentButton');

    this.$rootScope.$evalAsync(async () => {
      if (event.defaultPrevented) {
        return;
      }

      const { path } = await this.PickerService.pickPath(data.pickerConfig, data.parameterValue);

      this._onPathPicked(data.containerItem, data.parameterName, path, data.parameterBasePath);
    });
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
