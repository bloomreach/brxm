/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

window.OpenUi = new class {

  constructor() {
    this.cmsOrigin = window.location.origin;
    this.cmsBaseUrl = this.cmsOrigin + window.location.pathname;
    this.antiCache = window.Hippo.antiCache;
    this.classes = {};
  }

  registerClass(classDefinition) {
    const className = classDefinition.name;
    if (this.classes[className]) {
      throw new Error(`OpenUI class '${className}' is already registered`);
    }
    this.classes[className] = classDefinition;
  };

  _instantiateClass(className, parameters) {
    if (!this.classes[className]) {
      throw new Error(`Cannot instantiate class '${className}'. Has it been registered with 'OpenUi.registerClass()'?`);
    }

    return new this.classes[className](parameters);
  }

  showExtension(className, parameters) {
    const openUiParent = this._instantiateClass(className, parameters);

    const methods = openUiParent.getMethods();
    const connection = this._connectToChild(parameters, methods);

    this._destroyConnectionWhenIframeDies(connection, openUiParent);

    openUiParent.onConnect(connection);
  }

  _destroyConnectionWhenIframeDies(connection, openUiParent) {
    connection.promise.catch(error => {
      if (error.code !== Penpal.ERR_CONNECTION_DESTROYED) {
        console.warn('Unexpected error while destroying Penpal connection:', error);
      }
    });
    HippoAjax.registerDestroyFunction(connection.iframe, () => {
      connection.emitter.clearListeners();
      connection.destroy();
      openUiParent.onDestroy();
    });
  }

  _connectToChild(parameters, methods) {
    const {
      dialogOptions,
      extensionUrl,
      iframeParentId,
    } = parameters;

    const iframeUrl = this._getIframeUrl(extensionUrl, dialogOptions && dialogOptions.url);
    const iframe = this._createIframe(iframeUrl);

    const iframeParentElement = document.getElementById(iframeParentId);
    iframeParentElement.appendChild(iframe);

    const emitter = new Emitter();
    const connection = Penpal.connectToChild({
      iframe: iframe,
      methods: Object.assign({
        emitEvent: emitter.emit.bind(emitter),
        getProperties: this._getProperties.bind(this, parameters),
        openDialog: this.openDialog.bind(this, parameters),
      }, methods)
    });
    Object.assign(connection, { emitter, iframe });
    return connection;
  }

  _getIframeUrl(extensionUrl, extensionDialogUrl) {
    extensionUrl = this._getExtensionUrl(extensionUrl, this.cmsBaseUrl);

    return extensionDialogUrl
      ? this._getExtensionUrl(extensionDialogUrl, extensionUrl.href)
      : extensionUrl;
  }

  _createIframe(url) {
    const iframe = document.createElement('iframe');

    iframe.classList.add('openui-iframe');
    iframe.src = url;

    // Don't allow an extension to change the URL of the top-level window: sandbox the iframe and DON'T include:
    // - allow-top-navigation
    // - allow-top-navigation-by-user-activation
    iframe.sandbox = 'allow-forms allow-popups allow-popups-to-escape-sandbox allow-same-origin allow-scripts';

    return iframe;
  }

  _getExtensionUrl(url, base) {
    const absUrl = new URL(url, base);
    this._addQueryParameters(absUrl);
    return absUrl;
  }

  _addQueryParameters(url) {
    url.searchParams.append('br.antiCache', this.antiCache);
    url.searchParams.append('br.parentOrigin', this.cmsOrigin);
  }

  _getProperties(parameters) {
    const {
      cmsLocale,
      cmsTimeZone,
      cmsVersion,
      extensionConfig,
      userDisplayName,
      userFirstName,
      userId,
      userLastName,
    } = parameters;

    return {
      baseUrl: this.cmsBaseUrl,
      extension: {
        config: extensionConfig,
      },
      locale: cmsLocale,
      styling: 'classic',
      timeZone: cmsTimeZone,
      user: {
        id: userId,
        firstName: userFirstName,
        lastName: userLastName,
        displayName: userDisplayName,
      },
      version: cmsVersion,
    }
  }

  openDialog(parameters, options) {
    if (this.dialog) {
      return Promise.reject({
        code: 'DialogExists',
        message: 'A dialog already exists'
      });
    }

    if (!parameters.dialogUrl) {
      throw new Error('Cannot open dialog, parameter "dialogUrl" is not defined.');
    }

    return new Promise((resolve, reject) => {
      this.dialog = {
        resolve,
        reject,
      };

      Wicket.Ajax.post({
        u: parameters.dialogUrl,
        ep: options,
        fh: [(jqEvent, attributes) => reject(new Error(`Failed to open dialog. Server responded with status ${attributes.status}.`))],
      });

    });
  }

  closeDialog(result) {
    if (!this.dialog) {
      return;
    }

    this.dialog.resolve(result);
    delete this.dialog;
  }

  cancelDialog() {
    if (!this.dialog) {
      return;
    }

    this.dialog.reject({
      code: 'DialogCanceled',
      message: 'The dialog is canceled',
    });
    delete this.dialog;
  }
};

class Emitter {
  constructor() {
    this._events = new Map();
  }

  on(eventName, listener) {
    this._assertEventName(eventName);
    this._assertListener(listener);
    this._getListeners(eventName).add(listener);
  }

  emit(eventName, eventData) {
    this._assertEventName(eventName);
    this._getListeners(eventName)
      .forEach(listener => listener(eventData));
  }

  clearListeners() {
    this._events.clear();
  }

  _getListeners(eventName) {
    if (!this._events.has(eventName)) {
      this._events.set(eventName, new Set());
    }
    return this._events.get(eventName);
  }

  _assertEventName(eventName) {
    if (typeof eventName !== 'string') {
      throw new TypeError('eventName must be a string');
    }
  }

  _assertListener(listener) {
    if (typeof listener !== 'function') {
      throw new TypeError('listener must be a function');
    }
  }
}
