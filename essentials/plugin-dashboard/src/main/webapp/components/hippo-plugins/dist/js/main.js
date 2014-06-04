/*!
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
(function () {
    "use strict";

    angular.module('hippo.plugins', [])

    /**
     * Filter 'hippo.plugins.asset' that transforms a plugin-relative path to a packaged path.
     * The plugin can be specified on the scope, or in the template.  The template takes precedence.
     * <p>
     * Note that scope (prototypical) inheritance takes care of providing the plugin property in nested templates.
     */
            .filter('hippo.plugins.asset', ['hippo.plugins.url', function (buildUrl) {
                return function (path, plugin) {
                    return buildUrl((plugin ? plugin : this.plugin), path);
                };
            }])

        /*
         *
         */
            .provider('hippo.plugins.url', function () {
                this.prefix = 'components';
                this.exempt = [];

                this.setPrefix = function (prefix) {
                    this.prefix = prefix;
                };

                this.useRoot = function (pluginName) {
                    this.exempt.push(pluginName);
                };

                this.$get = function () {
                    var prefix = this.prefix,
                            exempt = this.exempt;
                    return function buildUrl(pluginName, path) {
                        if (!pluginName) {
                            return path;
                        } else if (exempt.indexOf(pluginName) < 0) {
                            return prefix + '/' + pluginName + '/dist/' + path;
                        } else {
                            return 'src/' + path;
                        }
                    };
                };
            });
})();
