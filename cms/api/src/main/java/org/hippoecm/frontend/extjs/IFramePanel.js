/*
 * Copyright 2015-2019 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
(function () {
  'use strict';

  Ext.ns('Hippo');

  Hippo.IFramePanel = Ext.extend(Ext.Panel, {

    frameId: null,
    hostToIFrame: null,
    iframeToHost: null,
    currentLocation: null,
    previousLocation: null,
    resizeTask: null,

    constructor (config) {
      this.frameId = Ext.id();
      this.hostToIFrame = Hippo.createMessageBus('host-to-iframe');
      this.iframeToHost = Hippo.createMessageBus('iframe-to-host');

      this.addEvents(
        'locationchanged'
      );

      this.iframeToHost.subscribe('user-activity', Hippo.UserActivity.report);

      Hippo.IFramePanel.superclass.constructor.call(this, Ext.apply(config, {
        border: false,
        layout: 'fit',
        items: {
          xtype: 'box',
          id: this.frameId,
          autoEl: {
            tag: 'iframe',
            frameborder: 0,
            src: config.url || 'about:blank'
          },
          listeners: {
            'afterrender': {
              fn: function (iframe) {
                iframe.el.addListener('load', this._onFrameLoad, this);
              },
              scope: this,
              single: true
            }
          }
        }
      }));
      this.on('afterrender', this._onAfterRender, this);
      this.on('resize', this._onResize, this);
    },

    _onFrameLoad () {
      var frameWindow = this._getFrameWindow();

      if (frameWindow !== null) {
        frameWindow.addEventListener('unload', this._detachFrame.bind(this));
      }

      this.previousLocation = this.currentLocation;
      this.currentLocation = this._getFrameLocation();

      this.fireEvent('locationchanged');
    },

    _connectToChild () {
      if (!window.Hippo.SubApp){
        window.Hippo.SubApp = {};
      }
      window.Hippo.SubApp['channelmanager-iframe'] = {};
      window.Hippo.Cms = {};

      window.Hippo.Cms.showMask = function() {
        window.Hippo.AppToNavApp.showMask();
      };

      window.Hippo.Cms.hideMask = function() {
        window.Hippo.AppToNavApp.hideMask();
      };

      window.Hippo.Cms.updateNavLocation = function(location){
        window.Hippo.AppToNavApp.updateNavLocation(location);
      };

      if (window.parent === window) { // cms is top window
        return;
      }
      var iFrameElement, subAppConnectConfig, promise;
      iFrameElement = this._getFrame(); // get iframe element of perspective
      // Config object sent to subapp when connecting.
      subAppConnectConfig = {
        iframe: iFrameElement,
        methods: window.Hippo.Cms
      };
      promise = window.bloomreach['navapp-communication']
        .connectToChild(subAppConnectConfig);
      promise.then( function(childApi){Object.assign(window.Hippo.SubApp['channelmanager-iframe'], childApi);
      }, function(error){
        console.error(error);
      });
    },

    _getFrameLocation () {
      var frameDocument, href;

      frameDocument = this._getFrameDocument();

      if (frameDocument && frameDocument.location !== undefined) {
        href = frameDocument.location.href;
        if (href !== undefined && href !== '' && href !== 'about:blank') {
          return href;
        }
      }
      return this._getFrameDom().src;
    },

    _getFrameDocument () {
      var frame = this._getFrame();
      return frame ? frame.contentDocument : null;
    },

    _getFrame () {
      return document.getElementById(this.frameId);
    },

    _getFrameDom () {
      return Ext.getDom(this.frameId);
    },

    _onAfterRender () {
      Hippo.UserActivity.report();
    },

    _onResize () {
      // throttle the number of 'resize' events send to the iframe
      if (this.resizeTask === null) {
        this.resizeTask = new Ext.util.DelayedTask(this._doResize.createDelegate(this));
      }
      this.resizeTask.delay(25);
    },

    _doResize () {
      this.hostToIFrame.publish('resize');
    },

    setLocation (url) {
      this.previousLocation = this.currentLocation;
      this._getFrameDom().src = url;
      this._connectToChild();
    },

    _detachFrame () {
      this.currentLocation = null;
      if (this.hostToIFrame) {
        this.hostToIFrame.unsubscribeAll();
      }
    },

    getLocation () {
      return this._getFrameLocation();
    },

    goBack () {
      if (!Ext.isEmpty(this.previousLocation)) {
        this.setLocation(this.previousLocation);
        this.previousLocation = null;
        return true;
      }
      return false;
    },

    getElement (id) {
      var frameDocument = this._getFrameDocument();
      return frameDocument ? frameDocument.getElementById(id) : null;
    },

    getFrameElement () {
      return Ext.getCmp(this.frameId).el;
    },

    reload () {
      var frameDocument = this._getFrameDocument();
      if (frameDocument) {
        frameDocument.location.reload(true);
      }
    },

    createHeadFragment () {
      // create an object to add elements to the iframe head using a DOM document fragment when possible

      var self, frameDocument, documentFragment, api;

      self = this;
      frameDocument = this._getFrameDocument();

      function getHead () {
        var headElements, head;

        headElements = frameDocument.getElementsByTagName('head');

        if (Ext.isEmpty(headElements)) {
          head = frameDocument.createElement('head');
          frameDocument.getElementsByTagName('html')[0].appendChild(head);
        } else {
          head = headElements[0];
        }
        return head;
      }

      function addElement (tagName, text, attributes) {
        var element, textNode;

        element = frameDocument.createElement(tagName);

        textNode = frameDocument.createTextNode(text);
        element.appendChild(textNode);

        Ext.iterate(attributes, function (attribute, value) {
          element[attribute] = value;
        });

        if (documentFragment === undefined) {
          documentFragment = self._getFrameDocument().createDocumentFragment();
        }
        documentFragment.appendChild(element);
      }

      api = {

        addScript: function (text, title) {
          addElement('script', text, {
            type: 'text/javascript',
            title: title || 'inline'
          });
          return api;
        },

        addStyleSheet: function (text, title) {
          addElement('style', text, {
            type: 'text/css',
            title: title
          });
          return api;
        },

        flush: function () {
          if (documentFragment !== undefined) {
            getHead().appendChild(documentFragment);
            documentFragment = undefined;
          }
        }

      };

      return api;
    },

    mask () {
      this.el.mask();
      this.on('locationchanged', this.el.mask, this.el);
    },

    unmask () {
      this.el.unmask();
      this.un('locationchanged', this.el.mask, this.el);
    },

    getScrollPosition () {
      var frameWindow = this._getFrameWindow();
      if (frameWindow !== null) {
        return {
          x: frameWindow.pageXOffset,
          y: frameWindow.pageYOffset
        };
      } else {
        return {
          x: 0,
          y: 0
        };
      }
    },

    _getFrameWindow () {
      var frame = this._getFrame();
      return frame ? frame.contentWindow : null;
    },

    scrollBy (x, y) {
      var frameWindow = this._getFrameWindow();
      if (frameWindow !== null) {
        frameWindow.scrollBy(x, y);
      }
    },

    getCookies () {
      var result = {},
        frameDocument = this._getFrameDocument();

      if (frameDocument) {
        Ext.each(frameDocument.cookie.split(';'), function (keyValue) {
          var equalsIndex, key, value;

          equalsIndex = keyValue.indexOf('=');
          key = keyValue.substr(0, equalsIndex).trim();
          value = keyValue.substr(equalsIndex + 1).trim();
          result[key] = value;
        }, this);
      }

      return result;
    },

  });

  Ext.reg('Hippo.IFramePanel', Hippo.IFramePanel);
}());
