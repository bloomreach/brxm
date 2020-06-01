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

class HstComponentService {
  constructor(ChannelService, ConfigService, HstService) {
    'ngInject';

    this.ChannelService = ChannelService;
    this.ConfigService = ConfigService;
    this.HstService = HstService;
  }

  setPathParameter(componentId, componentVariant, parameterName, parameterValue, basePath = '') {
    let path = parameterValue.startsWith('/') ? parameterValue : `/${parameterValue}`;

    if (basePath && path.length > basePath.length && path.startsWith(basePath)) {
      path = path.substring(basePath.length);
      if (path.startsWith('/')) {
        path = path.substring(1);
      }
    }

    return this.setParameter(componentId, componentVariant, parameterName, path);
  }

  setParameter(componentId, componentVariant, parameterName, parameterValue) {
    return this.setParameters(componentId, componentVariant, { [parameterName]: parameterValue });
  }

  setParameters(componentId, componentVariant, parameters) {
    // The component variant can contain special characters (@, [, ", etc.). Since it is used as a path element
    // in the backend call, it must be URI-encoded to be parsed correctly by the backend.
    const encodedVariant = encodeURIComponent(componentVariant);

    return this.HstService.doPutForm(parameters, componentId, encodedVariant)
      .then(() => this.ChannelService.recordOwnChange());
  }

  getProperties(componentId, componentVariant) {
    // The component variant can contain special characters (@, [, ", etc.). Since it is used as a path element
    // in the backend call, it must be URI-encoded to be parsed correctly by the backend.
    const encodedVariant = encodeURIComponent(componentVariant);
    return this.HstService.doGet(componentId, encodedVariant, this.ConfigService.locale);
  }

  deleteComponent(containerId, componentId) {
    return this.HstService.doDelete(containerId, componentId);
  }
}

export default HstComponentService;
