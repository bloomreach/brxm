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

    this.CommunicationService = CommunicationService;
    this.ConfigService = ConfigService;
    this.EditComponentService = EditComponentService;
    this.ContainerService = ContainerService;
    this.HippoIframeService = HippoIframeService;
    this.MaskService = MaskService;
    this.PageStructureService = PageStructureService;
    this.RightSidePanelService = RightSidePanelService;
    this.SidePanelService = SidePanelService;
  }

  getSelectedComponent() {
    return this.selectedComponent;
  }

  async selectComponent(component) {
    this.selectedComponent = component;
    this.MaskService.mask('mask-add-component');
    this.SidePanelService.liftSidePanelAboveMask();
    this.HippoIframeService.liftIframeAboveMask();
    this.MaskService.onClick(() => this.CommunicationService.toggleAddMode(false));

    try {
      const { container, nextComponent } = await this.CommunicationService.toggleAddMode(true);

      await this._addComponent(container, nextComponent);
    } finally {
      delete this.selectedComponent;
      this.MaskService.unmask();
      this.SidePanelService.lowerSidePanelBeneathMask();
      this.HippoIframeService.lowerIframeBeneathMask();
      this.MaskService.removeClickHandler();
    }
  }

  async _addComponent(containerId, nextComponentId) {
    if (!this.ConfigService.relevancePresent) {
      await this.RightSidePanelService.close();
    }

    const page = this.PageStructureService.getPage();
    const container = page && page.getContainerById(containerId);
    const componentId = await this.ContainerService.addComponent(this.selectedComponent, container, nextComponentId);
    delete this.selectedComponent;

    if (!this.ConfigService.relevancePresent) {
      const component = page && page.getComponentById(componentId);
      this.EditComponentService.startEditing(component);
    }
  }
}

export default ComponentCatalogService;
