/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export default class OpenUiService {
  constructor($log, ConfigService, ExtensionService, Penpal) {
    'ngInject';

    this.$log = $log;
    this.ConfigService = ConfigService;
    this.ExtensionService = ExtensionService;
    this.Penpal = Penpal;
  }

  connect(options) {
    const connection = this.Penpal.connectToChild(options);

    // Don't allow an extension to change the URL of the top-level window: sandbox the iframe and DON'T include:
    // - allow-top-navigation
    // - allow-top-navigation-by-user-activation
    angular.element(connection.iframe)
      .attr(
        'sandbox',
        'allow-forms allow-popups allow-popups-to-escape-sandbox allow-same-origin allow-scripts',
      );

    return connection.promise;
  }

  async initialize(extensionId, options) {
    const extension = this.ExtensionService.getExtension(extensionId);

    try {
      return await this.connect({
        url: this.ExtensionService.getExtensionUrl(extension),
        ...options,
        methods: {
          ...options.methods,
          getProperties: this.getProperties.bind(this, extension),
        },
      });
    } catch (error) {
      this.$log.warn(`Extension '${extension.displayName}' failed to connect with the client library.`, error);

      throw error;
    }
  }

  getProperties(extension) {
    return {
      baseUrl: this.ConfigService.getCmsOrigin() + this.ConfigService.getCmsContextPath(),
      extension: {
        config: extension.config,
      },
      locale: this.ConfigService.locale,
      timeZone: this.ConfigService.timeZone,
      user: {
        id: this.ConfigService.cmsUser,
        firstName: this.ConfigService.cmsUserFirstName,
        lastName: this.ConfigService.cmsUserLastName,
        displayName: this.ConfigService.cmsUserDisplayName,
      },
      version: this.ConfigService.cmsVersion,
    };
  }
}
