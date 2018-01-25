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

const PATH_PICKER_CALLBACK_ID = 'component-path-picker';

class HstComponentService {
  constructor($q, ChannelService, CmsService, HstService) {
    'ngInject';

    this.$q = $q;
    this.CmsService = CmsService;
    this.HstService = HstService;

    this.pathPickedHandler = angular.noop;

    this.CmsService.subscribe('path-picked', (callbackId, path) => {
      if (callbackId === PATH_PICKER_CALLBACK_ID) {
        this.pathPickedHandler(path);
        this.pathPickedHandler = angular.noop;
      }
    });
  }

  pickPath(componentId, parameterName, parameterValue, pickerConfig, basePath) {
    const deferred = this.$q.defer();
    this.pathPickedHandler = (path) => {
      const base = pickerConfig.isRelativePath ? basePath : '';
      this.setPathParameter(componentId, parameterName, path, base)
        .then(() => deferred.resolve())
        .catch(() => deferred.reject());
    };
    this.CmsService.publish('show-path-picker', PATH_PICKER_CALLBACK_ID, parameterValue, pickerConfig);
    return deferred.promise;
  }

  setPathParameter(componentId, parameterName, parameterValue, basePath = '') {
    let path = parameterValue.startsWith('/') ? parameterValue : `/${parameterValue}`;

    if (basePath && path.length > basePath.length) {
      path = path.substring(basePath.length);
      if (path.startsWith('/')) {
        path = path.substring(1);
      }
    }

    const params = {};
    params[parameterName] = path;

    return this.setParameter(componentId, parameterName, path);
  }

  setParameter(componentId, parameterName, parameterValue) {
    const params = {};
    params[parameterName] = parameterValue;
    return this.HstService.doPutForm(params, componentId, 'hippo-default');
  }
}

export default HstComponentService;
