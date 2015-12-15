/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

const BROWSER_SYNC_URL = '//localhost:3000/browser-sync/browser-sync-client.2.10.0.js';

export class IFrameService {

  constructor ($window, $log) {
    'ngInject';

    this.$window = $window;
    this.$log = $log;
    this.iframePanelId = this.getParentIFramePanelId();
    this.isActive = this.iframePanelId !== null;
  }

  getParentIFramePanelId () {
    const search = this.$window.location.search;

    if (search.length > 0) {
      const parameters = search.substring(1).split('&');

      for (let i = 0, length = parameters.length; i < length; i++) {
        const keyValue = parameters[i].split('=');
        if (keyValue[0] === 'parentExtIFramePanelId') {
          return keyValue[1];
        }
      }
    }

    return null;
  }

  getParentIFramePanel () {
    const iframePanel = this.$window.parent.Ext.getCmp(this.iframePanelId);

    if (!angular.isObject(iframePanel)) {
      throw new Error(`Unknown iframe panel id: '${this.iframePanelId}'`);
    }

    return iframePanel;
  }

  publish (event, value) {
    if (this.isActive) {
      return this.getParentIFramePanel().iframeToHost.publish(event, value);
    }
  }

  subscribe (event, callback, scope) {
    if (this.isActive) {
      return this.getParentIFramePanel().hostToIFrame.subscribe(event, callback, scope);
    }
  }

  getConfig () {
    if (this.isActive) {
      const iframePanel = this.getParentIFramePanel();
      const config = iframePanel.initialConfig.iframeConfig;

      if (config === undefined) {
        throw new Error('Parent iframe panel does not contain iframe configuration');
      }

      return config;
    } else {
      return {};
    }
  }

  addScriptToBody (scriptUrl) {
    const body = this.$window.document.getElementsByTagName('body')[0];
    const script = document.createElement('script');
    script.type = 'text/javascript';
    script.src = scriptUrl;
    body.appendChild(script);
  }

  enableBrowserSync () {
    if (this.getConfig().debug) {
      this.addScriptToBody(BROWSER_SYNC_URL);
      this.$log.info(`iframe #${this.iframePanelId} has browserSync enabled via ${BROWSER_SYNC_URL}`);
    }
  }
}
