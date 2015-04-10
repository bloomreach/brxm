/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
(function(window, $) {
  "use strict";

  window.Hippo = window.Hippo || {};

  if (!Hippo.UserMenu) {

    Hippo.UserMenu = function(linkSelector, menuSelector) {
      var link = $(linkSelector),
          menu = $(menuSelector),
          timeout = null,
          delay = 300,
          isRelated = function(e, el) {
            var $related = $(e.relatedTarget);
            return $related.is(el) || $related.closest(el).length;
          },
          hideMenuAfterDelay = function() {
            timeout = window.setTimeout(function() {
              menu.hide();
              timeout = null;
            }, delay);
          },
          aboutToHideMenu = function() {
            return timeout !== null;
          },
          cancelHideMenu = function() {
            if (timeout !== null) {
              window.clearTimeout(timeout);
              timeout = null;
            }
          };

      if (link === null || link.length === 0) {
        throw new Error('Could not find link element for Hippo.UserMenu with selector ' + linkSelector);
      }

      if (menu === null || menu.length === 0) {
        throw new Error('Could not find menu element for Hippo.UserMenu with selector ' + menuSelector);
      }

      link.mouseenter(function(e) {
        if (isRelated(e, menu)) {
          // coming form the menu - abort show
          return;
        }
        cancelHideMenu();
        menu.show();
      });
      link.mouseleave(function(e) {
        if (aboutToHideMenu() || isRelated(e, menu)) {
          // leaving for the menu or hide timeout already set - abort hide
          return;
        }
        hideMenuAfterDelay();
      });

      menu.mouseenter(cancelHideMenu);
      menu.mouseleave(function(e) {
        if (aboutToHideMenu() || isRelated(e, link)) {
          // leaving for the link or hide timeout already set - abort hide;
          return;
        }
        hideMenuAfterDelay();
      });
    };
  }
}(window, jQuery));
