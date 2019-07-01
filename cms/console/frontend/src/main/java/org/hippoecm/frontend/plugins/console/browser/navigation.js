/*
 * Copyright 2012-2019 Hippo B.V. (http://www.onehippo.com)
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

window.Hippo = window.Hippo || {};

Hippo.Tree = Hippo.Tree || {};
Hippo.Tree.addShortcuts = function(callbackUrl) {

    var register = function(key) {
        // always unregister first to avoid multiple callbacks registered to one input event
        shortcut.remove(key);
        shortcut.add(key, function() {
            Wicket.Ajax.get({
                u : callbackUrl + '&key=' + key
            });
        }, {
            'disable_in_input': true
        });
    };
    register('Up');
    register('Down');
    register('Left');
    register('Right');
};
