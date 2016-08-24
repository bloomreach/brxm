/*
 * Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
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

// TODO: Rename this to ExtApiService

export class CmsService {

  constructor($window, $log) {
    'ngInject';

    this.$window = $window;
    this.$log = $log;
    this.iframePanelId = this.getParentIFramePanelId();
  }

  getParentIFramePanelId() {
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

    throw new Error('Request parameter \'parentExtIFramePanelId\' not found in IFrame url');
  }

  getParentIFramePanel() {
    const iframePanel = this.$window.parent.Ext.getCmp(this.iframePanelId);

    if (!angular.isObject(iframePanel)) {
      throw new Error(`Unknown iframe panel id: '${this.iframePanelId}'`);
    }

    return iframePanel;
  }

  publish(...args) {
    const iframeToHost = this.getParentIFramePanel().iframeToHost;
    return iframeToHost.publish.apply(iframeToHost, args);
  }

  subscribe(topic, callback, scope) {
    const hostToIFrame = this.getParentIFramePanel().hostToIFrame;
    return scope ? hostToIFrame.subscribe(topic, callback, scope) : hostToIFrame.subscribe(topic, callback);
  }

  subscribeOnce(topic, callback, scope) {
    const hostToIFrame = this.getParentIFramePanel().hostToIFrame;
    return scope ? hostToIFrame.subscribeOnce(topic, callback, scope) : hostToIFrame.subscribeOnce(topic, callback);
  }

  unsubscribe(topic, callback, scope) {
    const hostToIFrame = this.getParentIFramePanel().hostToIFrame;
    return hostToIFrame.unsubscribe(topic, callback, scope);
  }

  getConfig() {
    const iframePanel = this.getParentIFramePanel();
    const config = iframePanel.initialConfig.iframeConfig;

    if (angular.isUndefined(config)) {
      throw new Error('Parent iframe panel does not contain iframe configuration');
    }

    return config;
  }
}
