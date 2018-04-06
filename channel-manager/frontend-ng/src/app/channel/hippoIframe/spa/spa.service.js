/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

class SpaService {
  constructor($log, DomService, OverlayService, PageStructureService, RenderingService) {
    'ngInject';

    this.$log = $log;
    this.DomService = DomService;
    this.OverlayService = OverlayService;
    this.PageStructureService = PageStructureService;
    this.RenderingService = RenderingService;
  }

  init(iframeJQueryElement) {
    this.iframeJQueryElement = iframeJQueryElement;
  }

  detectSpa() {
    this.spa = null;

    const iframeWindow = this.DomService.getIframeWindow(this.iframeJQueryElement);
    if (iframeWindow) {
      this.spa = iframeWindow.SPA;
    }

    return this.detectedSpa();
  }

  detectedSpa() {
    return !!this.spa;
  }

  initSpa() {
    try {
      const publicApi = {
        createOverlay: () => {
          this.RenderingService.createOverlay();
        },
        syncOverlay: () => {
          this.OverlayService.sync();
        },
      };
      this.spa.init(publicApi);
    } catch (error) {
      this.$log.error('Failed to initialize Single Page Application', error);
    }
  }

  renderComponent(componentId, parameters = {}) {
    if (this.spa && angular.isFunction(this.spa.renderComponent)) {
      const component = this.PageStructureService.getComponentById(componentId);
      if (component) {
        // let the SPA render the component; if it returns false, we still render the component instead
        return this.spa.renderComponent(component.getReferenceNamespace(), parameters) !== false;
      }
      this.$log.warn(`SPA cannot render unknown component with ID '${componentId}'`);
    }
    return false;
  }
}

export default SpaService;
