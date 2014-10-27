/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

(function (window) {
    'use strict';

    var config = {
        modulesFileSrc: ''
    };

    // public methods
    var hippoLoader = {};

    hippoLoader.scripts = function () {
        return document.getElementsByTagName('script');
    };

    hippoLoader.filenameFromPath = function (path) {
        return path.substring(path.lastIndexOf('/') + 1);
    };

    hippoLoader.loadModulesFile = function () {
        var scriptTags = hippoLoader.scripts();
        for (var i = scriptTags.length - 1; i > 0; i--) {
            var filename = hippoLoader.filenameFromPath(scriptTags[i].src);
            var dataMain = scriptTags[i].getAttribute('data-modules');

            if (filename == 'loader.js' || filename == 'loader.min.js') {
                if (dataMain) {
                    config.modulesFileSrc = dataMain;
                } else {
                    console.warn('No modules file specified for the plugin loader script. Using the default \'modules.json\' Example: <script src="loader.js" data-modules="modules.json"></script>');
                }
            }
        }
    };

    hippoLoader.loadModules = function (items, prefix) {

        $.each(items, function (index, component) {
            var folder, files;

            if (typeof component !== 'string') {
                folder = component.component;
                if (component.file) {
                    if ($.isArray(component.file)) {
                        files = component.file;
                    } else {
                        files = [component.file];
                    }
                } else {
                    files = [folder + '.js'];
                }
                if (!hippoLoader.isBrowser(component.browser)) {
                    return;
                }
            } else {
                folder = component;
                files = [component + '.js'];
            }
            $.each(files, function (index, file) {
                hippoLoader.getScript((prefix ? prefix + '/' : '') + (folder ? folder + '/' : '') + file);
            });
        });
    };

    hippoLoader.isBrowser = function (browser) {
        if (!browser) {
            return true;
        }
        switch (browser) {
            case 'IE':
                if (!IE.isTheBrowser) {
                    return false;
                }
                break;
            case 'IE <= 8':
                if (!IE.isTheBrowser) {
                    return false;
                }
                switch (IE.actualVersion) {
                    case '9':
                    case '10':
                        return false;
                }
                break;
        }
        return true;
    };

    hippoLoader.loadScripts = function (items) {
        $.each(items, function (index, url) {
            hippoLoader.getScript(url);
        });
    };

    hippoLoader.getScript = function (url) {
        $('head').append('<script src=\"' + url + '\" />');
    };

    hippoLoader.load = function (config) {
        config = config || {};
        $.ajax({
            url: config.modulesFileSrc,
            dataType: 'json'
        }).done(function (data) {
            var includes = data.includes;
            var app = data.application;
            $.each(includes, function (name, include) {
                if ($.isArray(include)) {
                    hippoLoader.loadScripts(include);
                } else {
                    hippoLoader.loadModules(include.items, include.prefix);
                }
            });

            $('head').append('<script>angular.bootstrap(document.getElementById(\'container\'), [\'' + app + '\']);</' + 'script>');
        }).fail(function (jqXHR, textStatus, errorThrown) {
            errorThrown.message = '[Loader error s(' + textStatus + ')]: ' + errorThrown;
        });
    };

    // run with config info
    hippoLoader.loadModulesFile();
    hippoLoader.load(config);

    // expose hippoLoader to the global object
    window.hippoLoader = hippoLoader;
})(window);