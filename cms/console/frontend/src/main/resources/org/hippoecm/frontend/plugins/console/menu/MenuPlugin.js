/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

(function(win, doc) {
  $(doc).ready(function() {

    if (!win.Hippo) {
      win.Hippo = {};
    }

    if (!win.Hippo.ConsoleMenuPlugin) {
      var J_KEY = 74;
      var ESCAPE = 27;
      var LEFT = 37;
      var RIGHT = 39;
      var UP = 38;
      var DOWN = 40;
      var ENTER = 13;
      var SPACE = 32;

      var pinnedOpen = null;

      function ConsoleMenuPlugin() {
        this.focusItems = [
          '.hippo-console-menu-advanced > li:not(.dropdown) a',
          '.hippo-console-menu-advanced > li.dropdown',
          '.hippo-console-menu-logout > li > a',
          '.hippo-console-tree-add > li > a[href]',
          '.hippo-console-save > li > a'
        ];
      }

      ConsoleMenuPlugin.prototype = {
        render: function () {
          this.setTabIndex();

          var menu = $('.hippo-console-menu-advanced');
          if ($.data(menu[0], 'rendered')) {
            // guard against wicket redraws
            return;
          }
          $.data(menu[0], 'rendered', true);

          // attach keyboard listeners to advanced menu items
          $('> li.dropdown', menu).each(function () {
            var $el = $(this);
            var items = $('.dropdown-content > li', $el);
            var selected = -1;

            function isValidIndex(index) {
              return index > -1 && index < items.length;
            }

            function deselect() {
              if (isValidIndex(selected)) {
                $(items[selected]).removeClass('highlight');
              }
            }

            function select(index) {
              if (isValidIndex(index)) {
                deselect();
                selected = index;
                $(items[selected]).addClass('highlight');
              }
            }

            function clickSelection() {
              if (isValidIndex(selected)) {
                $('a', items[selected]).click();
                return true;
              }
              return false;
            }

            $el.hover(function() {
              if (pinnedOpen) {
                pinnedOpen.blur();
              }
              $el.addClass('focus');
            }, function() {
              if (!pinnedOpen) {
                $el.removeClass('focus');
              }
            });
            $el.focus(function() {
              if (pinnedOpen) {
                pinnedOpen.blur();
              }
              pinnedOpen = $(this);
              $el.addClass('focus');
            });
            $el.blur(function() {
              pinnedOpen = null;
              $el.removeClass('focus');
              deselect();
              selected = -1;
            });

            $el.on('keydown', function (event) {

              switch (event.keyCode) {
                case LEFT:
                  $el.prev().focus();
                  event.stopPropagation();
                  break;

                case RIGHT:
                  $el.next().focus();
                  event.stopPropagation();
                  break;

                case UP:
                  select(selected - 1);
                  event.stopPropagation();
                  break;

                case DOWN:
                  select(selected + 1);
                  event.stopPropagation();
                  break;

                case SPACE:
                case ENTER:
                  if (clickSelection()) {
                    event.stopPropagation();
                  }
                  break;

                default:
                  break;
              }
            });
          });
        },

        focus: function () {
          $('.hippo-console-menu-advanced li.dropdown').first().focus();
        },

        blur: function () {
          $('.hippo-console-menu-advanced li:focus').blur();
        },

        setTabIndex: function() {
          var tabIndex = 1;
          function addTabIndex() {
            $(this).attr('tabindex', tabIndex++);
          }
          // DOM order is not as the user will expect it (logout menu comes before the advanced menu)
          // so we can't use jQuery multiple selector here
          for (var i = 0; i < this.focusItems.length; i++) {
            $(this.focusItems[i]).each(addTabIndex);
          }
        }
      };

      win.Hippo.ConsoleMenuPlugin = new ConsoleMenuPlugin();

      $(doc).keyup(function (e) {
        if (e.keyCode == ESCAPE) {
          // blur menu on escape
          Hippo.ConsoleMenuPlugin.blur();
        } else if (e.altKey && e.keyCode == J_KEY) {
          // focus menu on alt+j
          Hippo.ConsoleMenuPlugin.focus();
        }
      });

    }
  });

  // In some cases a window resize event draws an expanded dropdown menu in the wrong position.
  // To fix this we force a browser redraw on the expanded dropdown while resizing.
  var focusAfterResize;
  var focusAfterResizeTimer;

  $(win).resize(function() {
    focusAfterResize = $('.hippo-console-menu-advanced li:focus').get(0);

    if (focusAfterResizeTimer) {
      win.clearTimeout(focusAfterResizeTimer);
    }

    focusAfterResizeTimer = win.setTimeout(function() {
      $(focusAfterResize).focus();
      focusAfterResize = null;
    }, 200);
  });
})(window, document);
