/*
 * Copyright 2008-2015 Hippo B.V. (http://www.onehippo.com)
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

/**
 * @description <p>TODO</p>
 * @namespace YAHOO.hippo
 * @requires yahoo, dom
 * @module ajaxindicator
 */

  //TODO: might register to a custom layout-processing event to extend the loading indication
  //until after the layout has processed, instead of just the postAjaxEvent

(function(window, $) {

  YAHOO.namespace('hippo');

  YAHOO.hippo.AjaxIndicator = function(elementId, loadTimeout, busyTimeout) {
    this.body = $('body');
    this.elements = $('body, #' + elementId);

    this.active = true;
    this.calls = 0;

    this.loadTimer = null;
    this.busyTimer = null;

    this.loadTimeout = loadTimeout || 10;
    this.busyTimeout = busyTimeout || 500;

    this.waitClass = 'hippo-ajax-waiting';
    this.loadClass = 'hippo-ajax-show-load';
    this.busyClass = 'hippo-ajax-show-busy';

    this.subscribe();
  };

  YAHOO.hippo.AjaxIndicator.prototype = {

    subscribe: function() {
      if (Wicket && Wicket.Event) {
        Wicket.Event.subscribe(Wicket.Event.Topic.AJAX_CALL_BEFORE_SEND, $.proxy(this.beforeAjaxCall, this));
        Wicket.Event.subscribe(Wicket.Event.Topic.AJAX_CALL_COMPLETE, $.proxy(this.afterAjaxCall, this));
      }
    },

    beforeAjaxCall: function() {
      this.calls++;

      if (this.calls === 1) {
        this.show();
      }
    },

    afterAjaxCall: function() {
      if (this.calls > 0) {
        this.calls--;
      }

      if (this.calls === 0) {
        this.hide();
      }
    },

    show: function() {
        this.body.addClass(this.waitClass);

      if (this.active) {
      if (this.loadTimer === null) {
        this.loadTimer = window.setTimeout($.proxy(function() {
          this.elements.addClass(this.loadClass);
        }, this), this.loadTimeout);
      }

      if (this.busyTimer === null) {
        this.busyTimer = window.setTimeout($.proxy(function() {
          this.elements.addClass(this.busyClass);
        }, this), this.busyTimeout);
      }
      }
    },

    hide: function() {
      if (this.active) {
        if (this.busyTimer !== null) {
          window.clearTimeout(this.busyTimer);
          this.busyTimer = null;
        }

        if (this.loadTimer !== null) {
          window.clearTimeout(this.loadTimer);
          this.loadTimer = null;
        }

        this.elements.removeClass(this.busyClass).removeClass(this.loadClass);
      }
        this.body.removeClass(this.waitClass);
    },

    setActive: function(active) {
      if (this.calls > 0) {
        if (active) {
        this.show();
      } else {
        this.hide();
      }
    }
      this.active = active;
    }
  };
}(window, jQuery));

YAHOO.register("ajaxindicator", YAHOO.hippo.AjaxIndicator, {version: "2.8.1", build: "19"});
