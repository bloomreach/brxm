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
    var region, pickerList, pickerListDetails, pickerListDatatable,
    minimumDialogWidth, minimumDialogHeight, deltaWidth, deltaHeight, defaultExpansion = 1, oppositeExpansion = -1,
    oldWindowInitialize, oldWindowBindInit, oldWindowBindClean, oldShow,
    oldOnResizeBottomRight, oldOnResizeBottomLeft, oldOnResizeBottom, oldOnResizeLeft, oldOnResizeRight,
    oldOnResizeTopRight, oldOnResizeTopLeft, oldOnResizeTop;

    oldOnResizeBottomRight = Wicket.Window.prototype.onResizeBottomRight;
	oldOnResizeBottomLeft = Wicket.Window.prototype.onResizeBottomLeft;
	oldOnResizeBottom = Wicket.Window.prototype.onResizeBottom;
	oldOnResizeLeft = Wicket.Window.prototype.onResizeLeft;
	oldOnResizeRight = Wicket.Window.prototype.onResizeRight;
	oldOnResizeTopRight = Wicket.Window.prototype.onResizeTopRight;
	oldOnResizeTopLeft = Wicket.Window.prototype.onResizeTopLeft;
	oldOnResizeTop = Wicket.Window.prototype.onResizeTop;

    oldWindowInitialize = Wicket.Window.prototype.initialize;
    Wicket.Window.prototype.initialize = function() {
        oldWindowInitialize.apply(this, arguments);
        this.settings.isFullscreen = false;

        this.event = {
            beforeFullScreen: new YAHOO.util.CustomEvent('beforeFullScreen'),
            afterFullScreen: new YAHOO.util.CustomEvent('afterFullScreen'),
            beforeInitScreen: new YAHOO.util.CustomEvent('beforeInitScreen'),
            afterInitScreen: new YAHOO.util.CustomEvent('afterInitScreen'),
            resizeFullScreen: new YAHOO.util.CustomEvent('resizeFullScreen')
        };
    };

    Wicket.Window.prototype.onWindowResize = function(e) {
        var w, f, width, height;
        if (this.isFullscreen) {
            w = this.window;
            f = this.content;

            width  = Wicket.Window.getViewportWidth();
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

    Wicket.Window.prototype.onResizeLeft = function(object, deltaX, deltaY) {
        this.resize(object, deltaX, 0, oldOnResizeLeft, oppositeExpansion, defaultExpansion);
        return this.res;
    };

    Wicket.Window.prototype.onResizeTop = function(object, deltaX, deltaY) {
        this.resize(object, 0, deltaY, oldOnResizeTop, defaultExpansion, oppositeExpansion);
        return this.res;
    };

    Wicket.Window.prototype.onResizeRight = function(object, deltaX, deltaY) {
        this.resize(object, deltaX, 0, oldOnResizeRight, defaultExpansion, defaultExpansion);
        return this.res;
    };

    Wicket.Window.prototype.onResizeBottom = function(object, deltaX, deltaY) {
        this.resize(object, 0, deltaY, oldOnResizeBottom, defaultExpansion, defaultExpansion);
        return this.res;
    };

    Wicket.Window.prototype.onResizeTopLeft = function(object, deltaX, deltaY) {
        this.resize(object, deltaX, deltaY, oldOnResizeTopLeft, oppositeExpansion, oppositeExpansion);
		return this.res;
	};

	Wicket.Window.prototype.onResizeTopRight = function(object, deltaX, deltaY) {
        this.resize(object, deltaX, deltaY, oldOnResizeTopRight, defaultExpansion, oppositeExpansion);
		return this.res;
	};

    Wicket.Window.prototype.onResizeBottomRight = function(object, deltaX, deltaY) {
        this.resize(object, deltaX, deltaY, oldOnResizeBottomRight, defaultExpansion, defaultExpansion);
        return this.res;
    };

    Wicket.Window.prototype.onResizeBottomLeft = function(object, deltaX, deltaY) {
        this.resize(object, deltaX, deltaY, oldOnResizeBottomLeft, oppositeExpansion, defaultExpansion);
        return this.res;
    };

    Wicket.Window.prototype.resize = function(object, deltaX, deltaY, method, widthExpansion, heightExpansion) {
        this.calculateDimensions(widthExpansion * deltaX, heightExpansion * deltaY);
        method.apply(this, [object, widthExpansion * deltaWidth, heightExpansion * deltaHeight]);
        this.readComponents();
        this.resizePickerList();
        this.resizePickerListDatatable();
    };

    Wicket.Window.prototype.getInitialDimensions = function() {
        minimumDialogHeight = Wicket.Window.current.settings.height;
        minimumDialogWidth = Wicket.Window.current.settings.width;
    };

    Wicket.Window.prototype.calculateDimensions = function(deltaX, deltaY) {
        deltaHeight = (Wicket.Window.current.height + deltaY < minimumDialogHeight) ? 0 : deltaY;
        deltaWidth = (Wicket.Window.current.width + deltaX < minimumDialogWidth) ? 0 : deltaX;
    };

    Wicket.Window.prototype.readComponents = function() {
        YAHOO.util.Dom.getElementBy(function(el) {
            pickerListDatatable = YAHOO.hippo.WidgetManager.getWidget(el.id);
        }, 'table', Wicket.Window.current.right);
        if (pickerListDatatable != null) {
            pickerListDetails = YAHOO.util.Dom.getAncestorBy(pickerListDatatable.el, function(node) {
                return YAHOO.lang.isValue(node.className) && node.className === 'hippo-picker-list-details';
            });
            pickerList = pickerListDetails.parentElement;
            region = YAHOO.util.Dom.getRegion(pickerListDetails);
        }
    };

    Wicket.Window.prototype.resizePickerList = function() {
        if (pickerList != null && pickerListDetails != null) {
            pickerList.style.height = pickerList.clientHeight + deltaHeight + 'px';
            pickerListDetails.style.height = pickerListDetails.clientHeight + deltaHeight + 'px';
        }
    };

    Wicket.Window.prototype.resizePickerListDatatable = function() {
        if (region != null && pickerListDatatable != null) {
            pickerListDatatable.resize({
                wrap: {
                    w: region.width,
                    h: region.height
                }
            });
        }
    };

    Wicket.Window.prototype.restoreDatatableHeight = function() {
        this.readComponents();
        if (pickerList != null && pickerListDetails != null) {
            pickerListDetails.style.height = pickerList.clientHeight + 'px';
        }
        this.resizePickerListDatatable();
    };

    oldWindowBindInit = Wicket.Window.prototype.bindInit;
    Wicket.Window.prototype.bindInit = function() {
        oldWindowBindInit.apply(this, arguments);

        //register window resize listener
        if (YAHOO.util.Event) {
            YAHOO.util.Event.on(window, 'resize', this.onWindowResize, this, true);
        }
    };

    oldWindowBindClean = Wicket.Window.prototype.bindClean;
    Wicket.Window.prototype.bindClean = function() {
        //HippoAjax cleanup hook.
        if (YAHOO.hippo.HippoAjax) {
            YAHOO.hippo.HippoAjax.cleanupElement(this.window);
        }

        oldWindowBindClean.apply(this, arguments);

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

    oldShow  = Wicket.Window.prototype.show;
    Wicket.Window.prototype.show = function() {
      oldShow.apply(this, arguments);
      if (this.settings.titleTooltip !== null) {
        this.captionText.setAttribute('title', this.settings.titleTooltip);
      }
      this.getInitialDimensions();
    };

    Wicket.Window.prototype.toggleFullscreen = function() {
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
            width  = Wicket.Window.getViewportWidth();
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
    Wicket.Window.getMarkup = function(idWindow, idClassElement, idCaption, idContent, idTop, idTopLeft, idTopRight, idLeft, idRight, idBottomLeft, idBottomRight, idBottom, idCaptionText, isFrame) {
        var s = "<div class=\"wicket-modal\" id=\"" + idWindow + "\" role=\"dialog\" aria-labelledBy=\""+idCaptionText+"\" style=\"top: 10px; left: 10px; width: 100px;\">" +
                "<div id=\"" + idClassElement + "\">" +
                "<div class=\"w_top_1\">" +
                "<div class=\"w_topLeft\" id=\"" + idTopLeft + "\"></div>" +
                "<div class=\"w_topRight\" id=\"" + idTopRight + "\"></div>" +
                "<div class=\"w_top\" id='" + idTop + "'></div>" +
                "</div>" +
                "<div class=\"w_left\" id='" + idLeft + "'>" +
                "<div class=\"w_right_1\">" +
                "<div class=\"w_right\" id='" + idRight + "'>" +
                "<div class=\"w_content_1\" onmousedown=\"Wicket.Event.stop(event);\">" +
                "<div class=\"w_caption\"  id=\"" + idCaption + "\">" +
                "<a class=\"w_close\" style=\"z-index:1\" href=\"#\"></a>" +
                "<span id=\"" + idCaptionText + "\" class=\"w_captionText\"></span>" +
                "</div>" +

                "<div class=\"w_content_2\">" +
                "<div class=\"w_content_3\">" +
                "<div class=\"w_content\">";

        if (isFrame) {
            s += "<iframe frameborder=\"0\" id=\"" + idContent + "\" allowtransparency=\"false\" style=\"height: 200px\" class=\"wicket_modal\"></iframe>";
        } else {
            s += "<div id='" + idContent + "' class='w_content_container'></div>";
        }

        s +=    "</div>" +
                "</div>" +
                "</div>" +
                "</div>" +
                "</div>" +
                "</div>" +
                "</div>" +
                "<div class=\"w_bottom_1\" id=\"" + idBottom + "\">" +
                "<div class=\"w_bottomRight\"  id=\"" + idBottomRight + "\">" +
                "</div>" +
                "<div class=\"w_bottomLeft\" id=\"" + idBottomLeft + "\">" +
                "</div>" +
                "<div class=\"w_bottom\" id=\"" + idBottom + "\">" +
                "</div>" +
                "</div>" +
                "</div>" +
                "</div>";

        return s;
    };

    //Simply refresh if the user wants to
    Wicket.Window.unloadConfirmation = false;

}());
