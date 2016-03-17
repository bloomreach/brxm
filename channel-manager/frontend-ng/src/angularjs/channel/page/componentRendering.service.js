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

export class ComponentRenderingService {

  constructor($http, $log, CmsService, PageStructureService, RenderingService) {
    'ngInject';

    this.$http = $http;
    this.$log = $log;
    this.CmsService = CmsService;
    this.PageStructureService = PageStructureService;
    this.RenderingService = RenderingService;
  }

  initialize() {
    this.CmsService.subscribe('render-component', this._renderComponent, this);
  }

  _renderComponent(componentId, propertiesMap) {
    const component = this.PageStructureService.getComponentById(componentId);
    if (component) {
      this.RenderingService.fetchComponentMarkup(component, propertiesMap).then((response) => {
        this.PageStructureService.updateComponent(component, response.data);
      });
    } else {
      this.$log.warn(`Cannot render unknown component '${componentId}'`);
    }
  }
}
