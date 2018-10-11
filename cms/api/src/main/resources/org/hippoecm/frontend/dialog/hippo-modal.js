/*
 *  Copyright 2012-2018 Hippo B.V. (http://www.onehippo.com)
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

/**
 *  Customisations for Wicket.Window
 *
 *  - Added fullscreen support
 *  - Register for a HippoAjax cleanup callback
 *  - Custom getMarkup impl that does not contain a form element
 *  - Added resizable support
 */
(function () {
  "use strict";
  var
    defaultExpansion = 1,
    oppositeExpansion = -1,
    _super = {};

  [
   'initialize',
   'bindInit',
   'bindClean',
   'show',
   'onResizeBottomRight',
   'onResizeBottomLeft',
   'onResizeBottom',
   'onResizeLeft',
   'onResizeRight',
   'onResizeTopRight',
   'onResizeTopLeft',
   'onResizeTop'
  ].forEach(function(property) {
    _super[property] = Wicket.Window.prototype[property];
  });

  Wicket.Window.prototype.initialize = function () {
    _super.initialize.apply(this, arguments);
    this.settings.isFullscreen = false;

    this.event = {
      beforeFullScreen: new YAHOO.util.CustomEvent('beforeFullScreen'),
      afterFullScreen: new YAHOO.util.CustomEvent('afterFullScreen'),
      beforeInitScreen: new YAHOO.util.CustomEvent('beforeInitScreen'),
      afterInitScreen: new YAHOO.util.CustomEvent('afterInitScreen'),
      resizeFullScreen: new YAHOO.util.CustomEvent('resizeFullScreen')
    };
  };

  Wicket.Window.prototype.onWindowResize = function (e) {
    var w, f, width, height;
    if (this.isFullscreen) {
      w = this.window;
      f = this.content;

      width = Wicket.Window.getViewportWidth();
      height = Wicket.Window.getViewportHeight();

      w.style.width = width + "px";
      w.style.height = height + "px";
      w.style.top = "0";
      w.style.left = "0";

      f.style.height = height + "px";
      f.style.width = width + "px";

      this.event.resizeFullScreen.fire({w: width, h: height});

      this.resizing();
    }
  };

  Wicket.Window.prototype.onResizeLeft = function (object, deltaX, deltaY) {
    this.resize(object, deltaX, 0, _super.onResizeLeft, oppositeExpansion, defaultExpansion);
    return this.res;
  };

  Wicket.Window.prototype.onResizeTop = function (object, deltaX, deltaY) {
    this.resize(object, 0, deltaY, _super.onResizeTop, defaultExpansion, oppositeExpansion);
    return this.res;
  };

  Wicket.Window.prototype.onResizeRight = function (object, deltaX, deltaY) {
    this.resize(object, deltaX, 0, _super.onResizeRight, defaultExpansion, defaultExpansion);
    return this.res;
  };

  Wicket.Window.prototype.onResizeBottom = function (object, deltaX, deltaY) {
    this.resize(object, 0, deltaY, _super.onResizeBottom, defaultExpansion, defaultExpansion);
    return this.res;
  };

  Wicket.Window.prototype.onResizeTopLeft = function (object, deltaX, deltaY) {
    this.resize(object, deltaX, deltaY, _super.onResizeTopLeft, oppositeExpansion, oppositeExpansion);
    return this.res;
  };

  Wicket.Window.prototype.onResizeTopRight = function (object, deltaX, deltaY) {
    this.resize(object, deltaX, deltaY, _super.onResizeTopRight, defaultExpansion, oppositeExpansion);
    return this.res;
  };

  Wicket.Window.prototype.onResizeBottomRight = function (object, deltaX, deltaY) {
    this.resize(object, deltaX, deltaY, _super.onResizeBottomRight, defaultExpansion, defaultExpansion);
    return this.res;
  };

  Wicket.Window.prototype.onResizeBottomLeft = function (object, deltaX, deltaY) {
    this.resize(object, deltaX, deltaY, _super.onResizeBottomLeft, oppositeExpansion, defaultExpansion);
    return this.res;
  };

  Wicket.Window.prototype.resize = function (object, deltaX, deltaY, method, widthExpansion, heightExpansion) {
    if (!this.resizer) {
      return;
    }

    var delta = this.resizer.calculateDelta(widthExpansion * deltaX, heightExpansion * deltaY);
    method.apply(this, [object, widthExpansion * delta.width, heightExpansion * delta.height]);

    this.resizer.resize(delta);
  };

  Wicket.Window.prototype.bindInit = function () {
    _super.bindInit.apply(this, arguments);

    //register window resize listener
    if (YAHOO.util.Event) {
      YAHOO.util.Event.on(window, 'resize', this.onWindowResize, this, true);
    }
  };

  Wicket.Window.prototype.bindClean = function () {
    //HippoAjax cleanup hook.
    if (YAHOO.hippo.HippoAjax) {
      YAHOO.hippo.HippoAjax.cleanupElement(this.window);
    }

    _super.bindClean.apply(this, arguments);

    this.resizer = null;

    //unregister window resize listener
    if (YAHOO.util.Event) {
      YAHOO.util.Event.removeListener(window, 'resize', this.onWindowResize);
    }

    this.event.beforeFullScreen.unsubscribeAll();
    this.event.afterFullScreen.unsubscribeAll();
    this.event.beforeInitScreen.unsubscribeAll();
    this.event.afterInitScreen.unsubscribeAll();
    this.event.resizeFullScreen.unsubscribeAll();
  };

  Wicket.Window.prototype.show = function () {
    _super.show.apply(this, arguments);

    if (this.settings.titleTooltip !== null) {
      this.captionText.setAttribute('title', this.settings.titleTooltip);
    }

    if (this.settings.resizable) {
      this.resizer = new WicketWindowResizer();
    }
  };

  Wicket.Window.prototype.toggleFullscreen = function () {
    var w, f, width, height;

    w = this.window;
    f = this.content;

    if (this.isFullscreen) {
      //go small
      this.event.beforeInitScreen.fire();

      w.className = this.oldWClassname;

      w.style.width = this.oldWWidth;
      w.style.height = this.oldWHeight;
      w.style.top = this.oldWTop;
      w.style.left = this.oldWLeft;

      f.style.width = this.oldCWidth;
      f.style.height = this.oldCHeight;
      f.style.top = this.oldCTop;
      f.style.left = this.oldCLeft;

      this.event.afterInitScreen.fire();

      this.resizing();
      this.isFullscreen = false;
    } else {
      //go fullscreen
      this.event.beforeFullScreen.fire();

      this.oldWClassname = w.className;
      w.className = w.className + ' ' + 'modal_fullscreen';

      //save previous dimensions
      this.oldWWidth = w.style.width;
      this.oldWHeight = w.style.height;
      this.oldWTop = w.style.top;
      this.oldWLeft = w.style.left;

      this.oldCWidth = f.style.width;
      this.oldCHeight = f.style.height;
      this.oldCTop = f.style.top;
      this.oldCLeft = f.style.left;

      //calculate new dimensions
      width = Wicket.Window.getViewportWidth();
      height = Wicket.Window.getViewportHeight();

      w.style.width = width + "px";
      w.style.height = height + "px";
      w.style.top = "0";
      w.style.left = "0";

      f.style.width = width + "px";
      f.style.height = height + "px";
      f.className = 'modal_fullscreen_content';

      this.event.afterFullScreen.fire({w: width, h: height});

      this.resizing();
      this.isFullscreen = true;
    }
    return this.isFullscreen;
  };

  /**
   * Returns the modal window markup with specified element identifiers and without a form element.
   */
  Wicket.Window.getMarkup = function (idWindow, idClassElement, idCaption, idContent, idTop, idTopLeft, idTopRight, idLeft, idRight, idBottomLeft, idBottomRight, idBottom, idCaptionText, isFrame) {
    var contentHtml = isFrame
      ? '<iframe frameborder="0" id="' + idContent + '" allowtransparency="false" style="height: 200px" class="wicket_modal"></iframe>'
      : '<div id="' + idContent + '" class="w_content_container"></div>';

    var markup =
      '<div class="wicket-modal" id="' + idWindow + '" role="dialog" aria-labelledBy="' + idCaptionText + '" style="top: 10px; left: 10px; width: 100px;">' +
        '<div id="' + idClassElement + '">' +
          '<div class="w_top_1">' +
            '<div class="w_topLeft" id="' + idTopLeft + '"></div>' +
            '<div class="w_topRight" id="' + idTopRight + '"></div>' +
            '<div class="w_top" id="' + idTop + '"></div>' +
          '</div>' +
          '<div class="w_left" id="' + idLeft + '">' +
            '<div class="w_right_1">' +
              '<div class="w_right" id="' + idRight + '">' +
                '<div class="w_content_1" onmousedown="Wicket.Event.stop(event);">' +
                  '<div class="w_caption"  id="' + idCaption + '">' +
                    '<a class="w_close" style="z-index:1" href="#"></a>' +
                    '<span id="' + idCaptionText + '" class="w_captionText"></span>' +
                  '</div>' +
                  '<div class="w_content_2">' +
                    '<div class="w_content_3">' +
                      '<div class="w_content">' +
                        contentHtml +
                      '</div>' +
                    '</div>' +
                  '</div>' +
                '</div>' +
              '</div>' +
            '</div>' +
          '</div>' +
          '<div class="w_bottom_1" id="' + idBottom + '">' +
            '<div class="w_bottomRight"  id="' + idBottomRight + '"></div>' +
            '<div class="w_bottomLeft" id="' + idBottomLeft + '"></div>' +
            '<div class="w_bottom" id="' + idBottom + '"></div>' +
          '</div>' +
        '</div>' +
      '</div>';

    return markup;
  };

  //Simply refresh if the user wants to
  Wicket.Window.unloadConfirmation = false;

  function getChildByClassName(parent, className) {
    var children;
    if (!parent) {
      return null;
    }

    children = parent.getElementsByClassName(className);
    if (children.length === 0) {
      return null;
    }

    return children[0];
  }

  function WicketWindowResizer() {
    this.initialWidth = Wicket.Window.current.settings.width;
    this.initialHeight = this.getCurrentHeight();
  }

  WicketWindowResizer.prototype.getCurrentHeight = function () {
    if (Wicket.Window.current.settings.height !== null) {
      return Wicket.Window.current.settings.height;
    }

    // in this case, the dialog is configured with height=auto so we need to calculate it ourselves
    return YAHOO.util.Dom.getRegion(Wicket.Window.current.content).height;
  };

  WicketWindowResizer.prototype.calculateDelta = function (deltaX, deltaY) {
    return {
      height: (this.getCurrentHeight() + deltaY < this.initialHeight) ? 0 : deltaY,
      width: (Wicket.Window.current.width + deltaX < this.initialWidth) ? 0 : deltaX,
    };
  };

  WicketWindowResizer.prototype.resize = function (delta) {
    this.readComponents();
    this.resizePickerList(delta);
    this.resizePickerListDatatable();
  };

  WicketWindowResizer.prototype.readComponents = function () {
    var pickerListTableElement;

    this.picker = getChildByClassName(Wicket.Window.current.right, 'hippo-picker');
    if (!this.picker) {
      return;
    }

    this.pickerList = getChildByClassName(this.picker, 'hippo-picker-list-details');
    if (!this.pickerList) {
      return;
    }

    pickerListTableElement = this.pickerList.querySelector('table');
    if (pickerListTableElement) {
      this.pickerListWidget = YAHOO.hippo.WidgetManager.getWidget(pickerListTableElement.id);
    }
  };

  WicketWindowResizer.prototype.resizePickerList = function (delta) {
    if (this.picker !== null && this.pickerList !== null) {
      this.picker.style.height = this.picker.clientHeight + delta.height + 'px';
      this.pickerList.style.height = this.pickerList.clientHeight + delta.height + 'px';
    }
  };

  WicketWindowResizer.prototype.resizePickerListDatatable = function () {
    var region;

    if (this.pickerList === null || this.pickerListWidget === null) {
      return;
    }

    region = YAHOO.util.Dom.getRegion(this.pickerList);
    if (region === null) {
      return;
    }

    this.pickerListWidget.resize({
      wrap: {
        w: region.width,
        h: region.height
      }
    });
  };

}());
