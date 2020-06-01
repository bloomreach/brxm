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

import dialogTemplate from './openuiDialog/openuiDialog.html';

export default class OpenUiService {
  constructor($document, $log, ConfigService, DialogService, Emittery, ExtensionService, Penpal) {
    'ngInject';

    this.$document = $document;
    this.$log = $log;
    this.ConfigService = ConfigService;
    this.DialogService = DialogService;
    this.Emittery = Emittery;
    this.ExtensionService = ExtensionService;
    this.Penpal = Penpal;
  }

  _createIframe(url) {
    const iframe = this.$document[0].createElement('iframe');

    // Don't allow an extension to change the URL of the top-level window: sandbox the iframe and DON'T include:
    // - allow-top-navigation
    // - allow-top-navigation-by-user-activation
    iframe.sandbox = 'allow-forms allow-popups allow-popups-to-escape-sandbox allow-same-origin allow-scripts';
    iframe.src = url;

    return iframe;
  }

  initialize(extensionId, options) {
    const extension = this.ExtensionService.getExtension(extensionId);
    const extensionUrl = options.url || this.ExtensionService.getExtensionUrl(extension);

    const iframe = this._createIframe(extensionUrl);
    options.appendTo.appendChild(iframe);

    try {
      const emitter = new this.Emittery();
      const connection = this.Penpal.connectToChild({
        ...options,
        iframe,
        methods: {
          ...options.methods,
          getProperties: this.getProperties.bind(this, extension),
          emitEvent: emitter.emit.bind(emitter),
          openDialog: dialogOptions => this.openDialog(dialogOptions, extensionId),
        },
      });

      return Object.assign(connection, { emitter, iframe });
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
      styling: 'material',
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

  /**
   * Opens a dialog.
   * Note that we cannot throw Errors because in that case Penpal does not transfer the code property correctly.
   */
  async openDialog(dialogOptions, extensionId) {
    if (this.isDialogOpen) {
      throw { code: 'DialogExists', message: 'A dialog already exists' }; // eslint-disable-line no-throw-literal
    }

    try {
      this.isDialogOpen = true;
      return await this.DialogService.show(angular.extend({
        clickOutsideToClose: true,
        locals: { dialogOptions, extensionId },
        template: dialogTemplate,
        controller: 'OpenuiDialogCtrl',
        controllerAs: '$ctrl',
      }));
    } catch (error) {
      throw { code: 'DialogCanceled', message: 'The dialog is canceled' }; // eslint-disable-line no-throw-literal
    } finally {
      this.isDialogOpen = false;
    }
  }
}
