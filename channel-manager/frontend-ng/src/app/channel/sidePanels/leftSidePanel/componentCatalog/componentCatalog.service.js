/*
 * Copyright 2016-2019 Hippo B.V. (http://www.onehippo.com)
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

class ComponentCatalogService {
  constructor(
    $log,
    ConfigService,
    ContainerService,
    EditComponentService,
    FeedbackService,
    HippoIframeService,
    MaskService,
    OverlayService,
    PageStructureService,
    RightSidePanelService,
    SidePanelService,
  ) {
    'ngInject';

    this.$log = $log;

    this.ConfigService = ConfigService;
    this.EditComponentService = EditComponentService;
    this.ContainerService = ContainerService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
    this.MaskService = MaskService;
    this.OverlayService = OverlayService;
    this.PageStructureService = PageStructureService;
    this.RightSidePanelService = RightSidePanelService;
    this.SidePanelService = SidePanelService;
  }

  getSelectedComponent() {
    return this.selectedComponent;
  }

  _enableAddModeMask() {
    this.MaskService.mask('mask-add-component');
    this.SidePanelService.liftSidePanelAboveMask();
    this.HippoIframeService.liftIframeAboveMask();
    this.OverlayService.enableAddMode();
  }

  _disableAddModeMask() {
    this.MaskService.resetMaskClass();

    this.SidePanelService.lowerSidePanelBeneathMask();
    this.HippoIframeService.lowerIframeBeneathMask();
    this.OverlayService.disableAddMode();
    this.OverlayService.offComponentClick();
    this.OverlayService.offContainerClick();
    this.MaskService.removeClickHandler();
  }

  selectComponent(component) {
    this.selectedComponent = component;
    this.MaskService.mask('mask-add-component');
    this.SidePanelService.liftSidePanelAboveMask();
    this.HippoIframeService.liftIframeAboveMask();
    this.OverlayService.enableAddMode();
    this.OverlayService.onComponentClick(this._handleComponentClick.bind(this));
    this.OverlayService.onContainerClick(this._handleContainerClick.bind(this));
    this.MaskService.onClick(this._handleMaskClick.bind(this));
  }

  _handleMaskClick() {
    if (this.OverlayService.toggleOverlayByComponent) {
      this.OverlayService.toggleOverlayByComponent = false;
      this.OverlayService.showComponentsOverlay(false);
    }
    delete this.selectedComponent;
    this.MaskService.unmask();
    this.SidePanelService.lowerSidePanelBeneathMask();
    this.HippoIframeService.lowerIframeBeneathMask();
    this.OverlayService.disableAddMode();
    this.OverlayService.offComponentClick();
    this.OverlayService.offContainerClick();
    this.MaskService.removeClickHandler();
  }

  async _handleComponentClick(event, clickedComponent) {
    const container = clickedComponent.getContainer();
    const clickedComponentIndex = container.items.findIndex(item => item === clickedComponent);

    if (container.isDisabled()) {
      event.stopPropagation();
      return;
    }

    const addedComponentId = await this._addComponent(container.getId());
    await this._positionNewComponent(event, clickedComponentIndex, addedComponentId);
  }

  async _positionNewComponent(event, clickedComponentIndex, addedComponentId) {
    const addedComponent = this.PageStructureService.getComponentById(addedComponentId);
    const container = addedComponent.getContainer();
    const components = container.items;
    const shouldPlaceBefore = event.target.classList.contains('hippo-overlay-element-component-drop-area-before');
    const nextComponent = shouldPlaceBefore ? components[clickedComponentIndex] : components[clickedComponentIndex + 1];

    return this.ContainerService.moveComponent(addedComponent, container, nextComponent);
  }

  _handleContainerClick(event, container) {
    if (container.isDisabled()) {
      event.stopPropagation();
      return;
    }

    this._addComponent(container.getId());
  }

  async _addComponent(containerId) {
    if (!this.ConfigService.relevancePresent) {
      await this.RightSidePanelService.close();
    }

    const container = this.PageStructureService.getContainerById(containerId);
    const componentId = await this.ContainerService.addComponent(this.selectedComponent, container);
    delete this.selectedComponent;

    if (!this.ConfigService.relevancePresent) {
      const component = this.PageStructureService.getComponentById(componentId);
      this.EditComponentService.startEditing(component);
    }

    return componentId;
  }
}

export default ComponentCatalogService;
