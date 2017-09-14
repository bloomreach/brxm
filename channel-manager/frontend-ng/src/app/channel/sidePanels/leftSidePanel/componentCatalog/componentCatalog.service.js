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

class ComponentCatalogService {
  constructor(
    $log,
    SidePanelService,
    HippoIframeService,
    MaskService,
    OverlayService,
    PageStructureService,
    FeedbackService,
  ) {
    'ngInject';

    this.$log = $log;

    this.SidePanelService = SidePanelService;
    this.HippoIframeService = HippoIframeService;
    this.MaskService = MaskService;
    this.OverlayService = OverlayService;
    this.PageStructureService = PageStructureService;
    this.FeedbackService = FeedbackService;
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
    this.OverlayService.offContainerClick();
    this.MaskService.removeClickHandler();
  }

  selectComponent(component) {
    this.selectedComponent = component;
    this.MaskService.mask('mask-add-component');
    this.SidePanelService.liftSidePanelAboveMask();
    this.HippoIframeService.liftIframeAboveMask();
    this.OverlayService.enableAddMode();
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
    this.OverlayService.offContainerClick();
    this.MaskService.removeClickHandler();
  }

  _handleContainerClick(event, container) {
    const containerOverlayElement = event.target;

    if (!container.isDisabled()) {
      this.addComponentToContainer(this.selectedComponent, containerOverlayElement);
      delete this.selectedComponent;
    } else {
      // If container is disabled dont do anything
      event.stopPropagation();
    }
  }

  addComponentToContainer(component, containerOverlayElement) {
    const container = this.PageStructureService.getContainerByOverlayElement(containerOverlayElement);

    this.PageStructureService.addComponentToContainer(component, container).then((newComponent) => {
      if (this.PageStructureService.containsNewHeadContributions(newComponent.getContainer())) {
        this.$log.info(`New '${newComponent.getLabel()}' component needs additional head contributions, reloading page`);
        this.HippoIframeService.reload().then(() => {
          this.PageStructureService.showComponentProperties(newComponent);
        });
      } else {
        this.PageStructureService.showComponentProperties(newComponent);
      }
    }).catch(() => {
      this.FeedbackService.showError('ERROR_ADD_COMPONENT', {
        component: component.label,
      });
    }).finally(() => {
      if (this.OverlayService.toggleOverlayByComponent) {
        this.OverlayService.toggleOverlayByComponent = false;
        this.OverlayService.showComponentsOverlay(false);
      }
    });
  }
}

export default ComponentCatalogService;
