/*
 * Copyright 2013-2014 Hippo B.V. (http://www.onehippo.com)
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
(function($) {
    "use strict";

    var iframe = parent.Ext.getCmp('pageEditorIFrame'),
        iframePosition = [0, 0],
        iframeToolbarHeight = 0,
        ExtDragDropMgr = parent.Ext.dd.DragDropMgr,
        ExtEventObject = parent.Ext.EventObjectImpl;

    $(window).unload(function() {
        iframe = null;
        ExtDragDropMgr = null;
        ExtEventObject = null;
    });

    function pageXOffset() {
        var result = window.pageXOffset;
        if (result === undefined) {
            result = document.body.scrollLeft;
        }
        return result;
    }

    function pageYOffset() {
        var result = window.pageYOffset;
        if (result === undefined) {
            result = document.body.scrollTop;
        }
        return result;
    }

    function createExtEvent(jQueryEvent) {
        var extEvent = new ExtEventObject(jQueryEvent);
        extEvent.xy[0] += iframePosition[0] - pageXOffset();
        extEvent.xy[1] += iframePosition[1] - pageYOffset() + iframeToolbarHeight;
        return extEvent;
    }

    function onMouseMove(event) {
        ExtDragDropMgr.handleMouseMove(createExtEvent(event));
    }

    function onMouseUp(event) {
        ExtDragDropMgr.handleMouseUp(createExtEvent(event));
    }

    iframe.hostToIFrame.subscribe('enablemouseevents', function() {
        var body = $('body');

        iframePosition = iframe.getPosition();
        iframeToolbarHeight = iframe.getTopToolbar().getHeight();

        body.bind('mousemove', onMouseMove);
        body.bind('mouseup', onMouseUp);
    });

    iframe.hostToIFrame.subscribe('disablemouseevents', function() {
        var body = $('body');
        body.unbind('mousemove', onMouseMove);
        body.unbind('mouseup', onMouseUp);
    });

}(jQuery));
