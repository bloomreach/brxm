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

OpenUi = new class {

  constructor() {
    this.cmsOrigin = window.location.origin;
    this.cmsBaseUrl = this.cmsOrigin + window.location.pathname;
    this.antiCache = window.Hippo.antiCache;
    this.classes = {};
    this.instances = {};
  }

  registerClass(classDefinition) {
    const className = classDefinition.name;
    if (this.classes[className]) {
      throw new Error(`OpenUI class '${className}' is already registered`);
    }
    this.classes[className] = classDefinition;
  };

  getInstance(id) {
    if (!this.instances[id]) {
      throw new Error(`Cannot retrieve instance '${id}' of OpenUI field. Has it been instantiated or has it already been destroyed?`);
    }
    return this.instances[id];
  }

  _instantiateClass(className, parameters) {
    if (!this.classes[className]) {
      throw new Error(`Cannot instantiate class '${className}'. Has it been registered with 'OpenUi.registerClass()'?`);
    }

    const instance = new this.classes[className](parameters);
    this.instances[parameters.iframeParentId] = instance;
    return instance;
  }

  showExtension(className, parameters) {
    const openUiParent = this._instantiateClass(className, parameters);

    const methods = openUiParent.getMethods();
    const connection = this._connectToChild(parameters, methods);

    this._destroyConnectionWhenIframeDies(connection, openUiParent);

    openUiParent.onConnect(connection);
  }

  _destroyConnectionWhenIframeDies(connection, openUiParent) {
    HippoAjax.registerDestroyFunction(connection.iframe, () => {
      const instanceId = openUiParent.parameters.iframeParentId;
      delete this.instances[instanceId];

      try {
        connection.destroy();
      } catch (error) {
        if (error.code !== Penpal.ERR_CONNECTION_DESTROYED) {
          console.warn('Unexpected error while destroying connection with document field extension:', error);
        }
      } finally {
        openUiParent.onDestroy();
      }
    });
  }

  _connectToChild(parameters, methods) {
    const {
      extensionUrl,
      iframeParentId,
    } = parameters;

    const iframeUrl = this._getIframeUrl(extensionUrl);
    const iframeParentElement = document.getElementById(iframeParentId);

    const iframe = document.createElement('iframe');

    iframe.classList.add('openui-iframe');

    // Don't allow an extension to change the URL of the top-level window: sandbox the iframe and DON'T include:
    // - allow-top-navigation
    // - allow-top-navigation-by-user-activation
    iframe.sandbox = 'allow-forms allow-popups allow-popups-to-escape-sandbox allow-same-origin allow-scripts';

    return Penpal.connectToChild({
      url: iframeUrl,
      iframe: iframe,
      appendTo: iframeParentElement,
      methods: {
        getProperties: () => this._getProperties(parameters),
        ...methods,
      }
    });
  }

  _getIframeUrl(url) {
    const iframeUrl = new URL(url, this.cmsOrigin);
    iframeUrl.searchParams.append('br.antiCache', this.antiCache);
    iframeUrl.searchParams.append('br.parentOrigin', this.cmsOrigin);
    return iframeUrl;
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
};
