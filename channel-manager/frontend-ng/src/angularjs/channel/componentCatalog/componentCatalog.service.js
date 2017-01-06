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
  constructor(MaskService, HippoIframeService, OverlayService) {
    'ngInject';

    this.MaskService = MaskService;
    this.HippoIframeService = HippoIframeService;
    this.OverlayService = OverlayService;
  }

  selectComponent(component) {
    this.MaskService.mask();
    this.OverlayService.mask();
    this.HippoIframeService.liftIframeAboveMask();

    this.OverlayService.onContainerClick(() => {
      this.addComponentToContainer(component);
    });

    this.MaskService.onClick(() => {
      this.MaskService.unmask();
      this.MaskService.removeClickHandler();
      this.OverlayService.unmask();
      this.OverlayService.offContainerClick();
      this.HippoIframeService.lowerIframeBeneathMask();
    });
  }

  addComponentToContainer(component) {
    console.log(component);
  }
}

export default ComponentCatalogService;
