/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
 */
(function () {
    "use strict";
    var oldWindowInitialize, oldWindowBindInit, oldWindowBindClean;

    oldWindowInitialize = Wicket.Window.prototype.initialize;
    Wicket.Window.prototype.initialize = function() {
        oldWindowInitialize.apply(this, arguments);
        this.settings.isFullscreen = false;
    };

    Wicket.Window.prototype.resizeLeftCropArea = function(w, h) {
        var lca;
        lca = document.getElementsByClassName('left-crop-area');
        if (lca.length ===1) {
            lca[0].style.width = w;
            lca[0].style.height = h;
        }
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
            this.resizeLeftCropArea(w.style.width, w.style.height);
            this.resizing();
        }
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
    };

    Wicket.Window.prototype.toggleFullscreen = function() {
        var w, f, width, height;

        w = this.window;
        f = this.content;

        if (this.isFullscreen) {
            //go small

            w.className = this.oldWClassname;

            w.style.width = this.oldWWidth;
            w.style.height = this.oldWHeight;
            w.style.top = this.oldWTop;
            w.style.left = this.oldWLeft;

            f.style.width = this.oldCWidth;
            f.style.height = this.oldCHeight;
            f.style.top = this.oldCTop;
            f.style.left = this.oldCLeft;

            this.resizeLeftCropArea(w.style.width, w.style.height);
            this.resizing();
            this.isFullscreen = false;
        } else {
            //go fullscreen
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

            width  = Wicket.Window.getViewportWidth();
            height = Wicket.Window.getViewportHeight();

            w.style.width = width + "px";
            w.style.height = height + "px";
            w.style.top = "0";
            w.style.left = "0";

            f.style.height = height + "px";
            f.style.width = width + "px";
            f.className = 'modal_fullscreen_content';

            this.resizeLeftCropArea(w.style.width, w.style.height);
            this.resizing();
            this.isFullscreen = true;
        }
        return this.isFullscreen;
    };

    /**
     * Returns the modal window markup with specified element identifiers and without a form element.
     */
    Wicket.Window.getMarkup = function(idWindow, idClassElement, idCaption, idContent, idTop, idTopLeft, idTopRight, idLeft, idRight, idBottomLeft, idBottomRight, idBottom, idCaptionText, isFrame) {
        var s = "<div class=\"wicket-modal\" id=\"" + idWindow + "\" style=\"top: 10px; left: 10px; width: 100px;\">" +
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
                "<a class=\"w_close\" href=\"#\"></a>" +
                "<span id=\"" + idCaptionText + "\" class=\"w_captionText\"></span>" +
                "</div>" +

                "<div class=\"w_content_2\">" +
                "<div class=\"w_content_3\">" +
                "<div class=\"w_content\">";

        if (isFrame) {
            s += "<iframe";
            if (Wicket.Browser.isIELessThan7()) {
                s += " src=\"about:blank\"";
            }
            s += " frameborder=\"0\" id=\"" + idContent + "\" allowtransparency=\"false\" style=\"height: 200px\" class=\"wicket_modal\"></iframe>";
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
