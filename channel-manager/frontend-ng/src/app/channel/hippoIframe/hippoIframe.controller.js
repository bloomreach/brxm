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

class HippoIframeCtrl {
  constructor(
    $element,
    $log,
    $rootScope,
    iframeAsset,
    ChannelService,
    CmsService,
    CommunicationService,
    ContainerService,
    CreateContentService,
    DomService,
    EditComponentService,
    EditContentService,
    FeedbackService,
    HippoIframeService,
    HstComponentService,
    PageStructureService,
    PickerService,
    RpcService,
    ScrollService,
    SpaService,
    ViewportService,
  ) {
    'ngInject';

    this.$element = $element;
    this.$log = $log;
    this.$rootScope = $rootScope;
    this.iframeAsset = iframeAsset;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.CommunicationService = CommunicationService;
    this.ContainerService = ContainerService;
    this.CreateContentService = CreateContentService;
    this.DomService = DomService;
    this.EditComponentService = EditComponentService;
    this.EditContentService = EditContentService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
    this.HstComponentService = HstComponentService;
    this.PageStructureService = PageStructureService;
    this.PickerService = PickerService;
    this.RpcService = RpcService;
    this.ScrollService = ScrollService;
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
    this._onDragStart = this._onDragStart.bind(this);
    this._onDragStop = this._onDragStop.bind(this);
    this._onMenuEdit = this._onMenuEdit.bind(this);
  }

  $onInit() {
    this.CmsService.subscribe('render-component', this._onComponentRender, this);
    this.CmsService.subscribe('delete-component', this._onComponentDelete, this);

    this.iframeJQueryElement.on('load', this.onLoad);
    this._offComponentClick = this.$rootScope.$on('iframe:component:click', this._onComponentClick);
    this._offComponentMove = this.$rootScope.$on('iframe:component:move', this._onComponentMove);
    this._offSdkReady = this.$rootScope.$on('spa:ready', this._onSpaReady);
    this._offSdkUnload = this.$rootScope.$on('iframe:unload', this._onUnload);
    this._offNewHeadContributions = this.$rootScope.$on(
      'page:new-head-contributions',
      this._onNewHeadContributions,
    );
    this._offDocumentCreate = this.$rootScope.$on('iframe:document:create', this._onDocumentCreate);
    this._offDocumentEdit = this.$rootScope.$on('iframe:document:edit', this._onDocumentEdit);
    this._offDocumentSelect = this.$rootScope.$on('iframe:document:select', this._onDocumentSelect);
    this._offDragStart = this.$rootScope.$on('iframe:drag:start', this._onDragStart);
    this._offDragStop = this.$rootScope.$on('iframe:drag:stop', this._onDragStop);
    this._offMenuEdit = this.$rootScope.$on('iframe:menu:edit', this._onMenuEdit);

    const canvasJQueryElement = this.$element.find('.channel-iframe-canvas');
    const sheetJQueryElement = this.$element.find('.channel-iframe-sheet');

    this.HippoIframeService.initialize(this.$element, this.iframeJQueryElement);
    this.ViewportService.init(sheetJQueryElement);
    this.ScrollService.init(this.iframeJQueryElement, canvasJQueryElement, sheetJQueryElement);
    this.SpaService.init(this.iframeJQueryElement);
    this.RpcService.initialize(this.iframeJQueryElement[0].contentWindow);
  }

  $onChanges(changes) {
    if (changes.showComponentsOverlay) {
      this.CommunicationService.toggleComponentsOverlay(changes.showComponentsOverlay.currentValue);
    }

    if (changes.showContentOverlay) {
      this.CommunicationService.toggleContentsOverlay(changes.showContentOverlay.currentValue);
    }
  }

  $onDestroy() {
    this.CommunicationService.disconnect();
    this.SpaService.destroy();
    this.RpcService.destroy();
    this.CmsService.unsubscribe('render-component', this._onComponentRender, this);
    this.CmsService.unsubscribe('delete-component', this._onComponentDelete, this);
    this._offComponentClick();
    this._offComponentMove();
    this._offSdkReady();
    this._offSdkUnload();
    this._offNewHeadContributions();
    this._offDocumentCreate();
    this._offDocumentEdit();
    this._offDocumentSelect();
    this._offDragStart();
    this._offDragStop();
    this._offMenuEdit();
  }

  async onLoad() {
    const target = this.iframeJQueryElement[0];

    if (!this.DomService.isFrameAccessible(target)) {
      return;
    }

    const connection = this.CommunicationService.connect({ target });

    await this.DomService.addScript(
      target.contentWindow,
      this.HippoIframeService.getAssetUrl(this.iframeAsset),
    );
    await connection;

    this._sync();

    if (this.SpaService.initLegacy()) {
      return;
    }

    this.PageStructureService.parseElements(true);
  }

  _onUnload() {
    this.ScrollService.disable();
    this.CommunicationService.disconnect();
  }

  async _onSpaReady() {
    const target = this.iframeJQueryElement[0];

    if (this.DomService.isFrameAccessible(target)) {
      return;
    }

    const connection = this.CommunicationService.connect({ target, origin: this.ChannelService.getOrigin() });

    await this.SpaService.inject(this.HippoIframeService.getAssetUrl(this.iframeAsset));
    await connection;

    this._sync();
  }

  _sync() {
    this.CommunicationService.toggleComponentsOverlay(this.showComponentsOverlay);
    this.CommunicationService.toggleContentsOverlay(this.showContentOverlay);
  }

  async _onNewHeadContributions(event, component, callback) {
    this.$log.info(`Updated '${component.getLabel()}' component needs additional head contributions.`);
    await this.HippoIframeService.reload();

    if (callback) {
      callback(component);
    }
  }

  _onComponentClick(event, componentId) {
    const page = this.PageStructureService.getPage();
    const component = page && page.getComponentById(componentId);
    if (!component) {
      return;
    }

    this.EditComponentService.startEditing(component);
  }

  async _onComponentDelete(componentId) {
    const page = this.PageStructureService.getPage();
    const component = page && page.getComponentById(componentId);
    if (!component) {
      this.$log.warn(`Cannot delete unknown component with id '${componentId}'`);
      return;
    }

    try {
      await this.ContainerService.deleteComponent(component);
    } catch (error) {
      this.EditComponentService.startEditing(component);
    }
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

  _onComponentRender(componentId, propertiesMap) {
    const page = this.PageStructureService.getPage();
    const component = page && page.getComponentById(componentId);

    if (!component) {
      this.$log.warn(`Cannot render unknown component with ID '${componentId}'.`);

      return;
    }

    this.ContainerService.renderComponent(component, propertiesMap);
  }

  getSrc() {
    return this.HippoIframeService.getSrc();
  }

  isIframeLifted() {
    return this.HippoIframeService.isIframeLifted;
  }

  isXPage() {
    const page = this.PageStructureService.getPage();
    return page && page.getMeta().isXPage();
  }

  _onDocumentCreate(event, config) {
    const data = this._getButtonData(config);

    this.CreateContentService.start(data);
  }

  _onDocumentEdit(event, uuid) {
    this.CmsService.reportUsageStatistic('CMSChannelsEditContent');

    this.EditContentService.startEditing(uuid);
  }

  _onDocumentSelect(event, config) {
    this.CmsService.reportUsageStatistic('PickContentButton');

    this.$rootScope.$evalAsync(async () => {
      if (event.defaultPrevented) {
        return;
      }

      const data = this._getButtonData(config);
      const { path } = await this.PickerService.pickPath(data.pickerConfig, data.parameterValue);

      this._onPathPicked(data.containerItem, data.parameterName, path, data.parameterBasePath);
    });
  }

  _onDragStart() {
    this.ScrollService.enable();
    this.$element.find('.channel-iframe-canvas')
      .addClass('hippo-dragging');
  }

  _onDragStop() {
    this.ScrollService.disable();
    this.$element.find('.channel-iframe-canvas')
      .removeClass('hippo-dragging');
  }

  _onMenuEdit(event, menuUuid) {
    this.$rootScope.$evalAsync(
      () => this.onEditMenu({ menuUuid }),
    );
  }

  _onPathPicked(component, parameterName, path, parameterBasePath) {
    const componentId = component.getId();
    const componentName = component.getLabel();
    const componentVariant = component.getRenderVariant();

    return this.HstComponentService.setPathParameter(
      componentId, componentVariant, parameterName, path, parameterBasePath,
    )
      .then(() => {
        this.ContainerService.renderComponent(component);
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

  _getButtonData(config) {
    const { containerItemId, isParameterValueRelativePath, ...data } = config;
    const parameterBasePath = isParameterValueRelativePath
      ? this.ChannelService.getChannel().contentRoot
      : '';
    const page = this.PageStructureService.getPage();
    const containerItem = page && page.getComponentById(containerItemId);

    return { ...data, containerItem, parameterBasePath };
  }
}

export default HippoIframeCtrl;
