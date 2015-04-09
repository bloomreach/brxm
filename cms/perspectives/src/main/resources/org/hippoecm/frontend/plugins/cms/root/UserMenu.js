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

    var UserMenuImpl = function () {
    };

    UserMenuImpl.prototype = {
      render : function(linkSelector, menuSelector) {
        var link = $(linkSelector),
            menu = $(menuSelector);

        if (link === null || link.length === 0) {
          throw new Error('Could not find link element for Hippo.UserMenu with selector ' + linkSelector);
        }

        if (menu === null || menu.length === 0) {
          throw new Error('Could not find menu element for Hippo.UserMenu with selector ' + menuSelector);
        }

        link.hover(function(e) {
          var $related = $(e.relatedTarget);
          if ($related.is(menu) || $related.closest(menu).length) {
            // coming form the menu - abort show
            return;
          }
          menu.show();

        }, function(e) {
          var $related = $(e.relatedTarget);
          if ($related.is(menu) || $related.closest(menu).length) {
            // leaving for the menu - abort hide
            return;
          }
          menu.hide();
        });

        menu.mouseleave(function(e) {
          var $related = $(e.relatedTarget);
          if ($related.is(link) || $related.closest(link).length) {
            // leaving for the link - abort hide;
            return;
          }
          menu.hide();
        });
      }
    };

    Hippo.UserMenu = new UserMenuImpl();
  }
}(window, jQuery));
