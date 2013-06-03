/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

function StateChange(editor, args) {
    this.editor = editor;
}

StateChange._pluginInfo = {
    name         : "StateChange",
    version      : "1.0",
    developer    : "a.bogaart@1hippo.com",
    developer_url: "http://www.onehippo.com/",
    c_owner      : "Hippo",
    license      : "al2",
    sponsor      : "OneHippo",
    sponsor_url  : "http://www.onehippo.com/"
};

StateChange.prototype = {
    setFullScreen : function(state, success, failure) {
        this._setState('fullScreen', state, success, failure);
    },

    setActivated: function(state, success, failure) {
        this._setState('activated', state, success, failure);
    },

    _getCallbackUrl : function() {
        var url = this.editor.config.StateChange.callbackUrl;
        return url + (url.indexOf('?') > -1 ? '&' : '?');
    },

    _setState : function(name, state, success, failure) {
        if (wicketAjaxGet) {
            wicketAjaxGet(this._getCallbackUrl() + name + '=' + state, success, failure);
        }
    }
};