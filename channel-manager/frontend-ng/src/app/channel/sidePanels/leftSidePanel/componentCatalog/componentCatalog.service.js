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

class ComponentCatalogService {
  constructor(
    $rootScope,
    CommunicationService,
    ConfigService,
    ContainerService,
    EditComponentService,
    HippoIframeService,
    MaskService,
    PageStructureService,
    RightSidePanelService,
    SidePanelService,
  ) {
    'ngInject';

    this.$rootScope = $rootScope;
    this.CommunicationService = CommunicationService;
    this.ConfigService = ConfigService;
    this.EditComponentService = EditComponentService;
    this.ContainerService = ContainerService;
    this.HippoIframeService = HippoIframeService;
    this.MaskService = MaskService;
    this.PageStructureService = PageStructureService;
    this.RightSidePanelService = RightSidePanelService;
    this.SidePanelService = SidePanelService;

    this._onMaskClick = this._onMaskClick.bind(this);
  }

  getSelectedComponent() {
    return this.selectedComponent;
  }

  async selectComponent(component) {
    const offMaskClick = this.$rootScope.$on('mask:click', this._onMaskClick);

    this.selectedComponent = component;
    this.MaskService.mask('mask-add-component');
    this.SidePanelService.liftSidePanelAboveMask();
    this.HippoIframeService.liftIframeAboveMask();

    try {
      const { container, nextComponent } = await this.CommunicationService.toggleAddMode(true);

      await this._addComponent(container, nextComponent);
    } finally {
      offMaskClick();

      delete this.selectedComponent;
      this.MaskService.unmask();
      this.SidePanelService.lowerSidePanelBeneathMask();
      this.HippoIframeService.lowerIframeBeneathMask();
    }
  }

  _onMaskClick() {
    this.CommunicationService.toggleAddMode(false);
  }

  async _addComponent(containerId, nextComponentId) {
    await this.RightSidePanelService.close();

    let page = this.PageStructureService.getPage();
    const container = page && page.getContainerById(containerId);
    const componentId = await this.ContainerService.addComponent(this.selectedComponent, container, nextComponentId);
    delete this.selectedComponent;

    // page may have been reloaded by the ContainerService
    page = this.PageStructureService.getPage();
    const component = page && page.getComponentById(componentId);
    this.EditComponentService.startEditing(component);
  }
}

export default ComponentCatalogService;
