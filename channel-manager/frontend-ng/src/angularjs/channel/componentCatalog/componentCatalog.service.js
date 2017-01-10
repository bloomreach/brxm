/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
  constructor($log, MaskService, HippoIframeService, OverlayService, PageStructureService) {
    'ngInject';

    this.$log = $log;

    this.MaskService = MaskService;
    this.HippoIframeService = HippoIframeService;
    this.OverlayService = OverlayService;
    this.PageStructureService = PageStructureService;
  }

  selectComponent(component) {
    this.MaskService.mask('mask-add-component');
    this.OverlayService.enableAddMode();
    this.HippoIframeService.liftIframeAboveMask();

    this.OverlayService.onContainerClick((target) => {
      this.addComponentToContainer(component, target);
    });

    this.MaskService.onClick(() => {
      this.MaskService.unmask();
      this.MaskService.removeClickHandler();
      this.OverlayService.disableAddMode();
      this.OverlayService.offContainerClick();
      this.HippoIframeService.lowerIframeBeneathMask();
    });
  }

  addComponentToContainer(component, target) {
    const container = this.PageStructureService.getContainerByOverlayElement(target);
    if (container) {
      this.PageStructureService.addComponentToContainer(component, container).then((newComponent) => {
        if (this.PageStructureService.containsNewHeadContributions(newComponent.getContainer())) {
          this.$log.info(`New '${newComponent.getLabel()}' component needs additional head contributions, reloading page`);
          this.HippoIframeService.reload().then(() => {
            this.PageStructureService.showComponentProperties(newComponent);
          });
        } else {
          this.PageStructureService.showComponentProperties(newComponent);
        }
      });
    } else {
      this.$log.debug(`
          Cannot add catalog item ${component.id} because container cannot be found for the overlay element
          or has been locked by a different user
          `, target);
    }
  }
}

export default ComponentCatalogService;
