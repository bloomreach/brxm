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

  constructor($http, $log, CmsService, PageStructureService) {
    'ngInject';

    this.$http = $http;
    this.$log = $log;
    this.CmsService = CmsService;
    this.PageStructureService = PageStructureService;
  }

  initialize() {
    this.CmsService.subscribe('render-component', this._renderComponent, this);
  }

  _renderComponent(componentId, propertiesMap) {
    const component = this.PageStructureService.getComponent(componentId);
    if (component) {
      this._fetchHtml(component, propertiesMap).then((response) => {
        const iframeElement = component.getJQueryElement('iframeBoxElement');
        iframeElement.html(response.data);
      });
    } else {
      this.$log.warn(`Cannot render unknown component '${componentId}'`);
    }
  }

  _fetchHtml(component, propertiesMap) {

    function toUrlEncodedFormData(json) {
      const keyValuePairs = [];
      for (const property in json) {
        if (json.hasOwnProperty(property)) {
          const key = encodeURIComponent(property);
          const value = encodeURIComponent(json[property]);
          keyValuePairs.push(`${key}=${value}`);
        }
      }
      return keyValuePairs.join('&');
    }

    return this.$http({
      method: 'POST',
      url: component.getRenderUrl(),
      headers: {
        Accept: 'text/html, */*',
        'Content-Type': 'application/x-www-form-urlencoded',
      },
      data: propertiesMap,
      transformRequest: toUrlEncodedFormData,
    });
  }

}
