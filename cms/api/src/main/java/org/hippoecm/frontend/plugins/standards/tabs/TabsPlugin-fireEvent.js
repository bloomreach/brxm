(function(window, document) {
    var fireEvent = function(element, eventName, active) {
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
            event.tabId = element.id ? element.id : element.name;
            event.active = active;
            if (document.createEvent) {
                element.dispatchEvent(event);
            } else if (element.fireEvent) {
                element.fireEvent('on' + event.eventType, event);
            }
        } catch (e) {
            if (console) {
                console.log('Error firing tab selection event on element: ' + element.id + ', ' + e);
            }
        }
    };
    if (window.Hippo && window.Hippo.activePerspective) {
        fireEvent(window.Hippo.activePerspective, 'readystatechange', false);
    }
    var decorator = document.getElementById('REPLACE_WITH_TAB_ID');
    fireEvent(decorator, 'readystatechange', true);
    window.Hippo = window.Hippo || {};
    window.Hippo.activePerspective = decorator;
})(window, document);
