(function(window, document) {
    if (window.history) {
        var path = "REPLACE_WITH_PATH",
            urlParameters = document.location.toString().split('?'),
            urlBase = urlParameters.shift(),
            hasPath = path.length > 0,
            url;

        if (urlParameters.length === 0) {
            if (hasPath) {
                url = urlBase + '?path=' + path;
            } else {
                url = urlBase;
            }
        } else {
            var queryString = urlParameters.join('?'); // join remaining parts, so parameters containing ? are handled correctly
            var parameters = queryString.split(/[&;]/g);
            var isChanged = false;
            for (var i = parameters.length - 1; i >= 0; i--) {
                if (parameters[i].indexOf('path=') === 0) {
                    if (hasPath) {
                        parameters[i] = 'path=' + path;
                    } else {
                        parameters.splice(i, 1);
                    }
                    isChanged = true;
                }
            }
            url = urlBase;
            if (parameters.length > 0) {
                url += '?' + parameters.join('&');
            }
            if (!isChanged && hasPath) {
                url += '&path=' + path;
            }
        }
        window.history.pushState(null, null, url);
    }
})(window, document);
