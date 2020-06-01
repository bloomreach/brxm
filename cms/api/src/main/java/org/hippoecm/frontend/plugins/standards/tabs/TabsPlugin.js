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
(function(window, document) {
    "use strict";

    if (window.Hippo === undefined) {
        window.Hippo = {};
    }

    window.Hippo.fireEvent = function(element, eventName, active) {
        try {
            var event;
            if (document.createEvent) {
                event = document.createEvent('HTMLEvents');
                event.initEvent(eventName, true, true);
            } else {
                event = document.createEventObject();
                event.eventType = eventName;
            }
            event.eventName = eventName;
            event.tabId = element.id || element.name;
            event.active = active;
            if (document.createEvent) {
                element.dispatchEvent(event);
            } else if (element.fireEvent) {
                element.fireEvent('on' + event.eventType, event);
            }
        } catch (e) {
            if (console) {
                console.log("Error firing event '" + eventName + "' on element '" + element.id + "', " + e);
            }
        }
    };

    window.Hippo.fireTabSelectionEvent = function(tabId) {
        if (window.Hippo.activePerspective) {
            window.Hippo.fireEvent(window.Hippo.activePerspective, 'readystatechange', false);
        }
        var decorator = document.getElementById(tabId);
        window.Hippo.fireEvent(decorator, 'readystatechange', true);
        window.Hippo.activePerspective = decorator;
    };
}(window, document));
