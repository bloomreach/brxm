/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var openModalDialog = null;

ModalDialog = function(url, plugin, token) {
    this.callbackUrl = url; 
    this.callbackUrl += url.indexOf('?') > -1 ? "&" : "?";
    this.callbackUrl += ('pluginName=' + encodeURIComponent(plugin));
    this.token = token;
};

ModalDialog.prototype = {
    _values : null,
        
    show : function(parameters) {
        var url = this.callbackUrl;
        for (var p in parameters) {
            url += ('&' + this.token + p + '=' + encodeURIComponent(parameters[p]));
        }
        wicketAjaxGet(url, null, null, null);
        openModalDialog = this;
    },

    close : function(values) {
        this._values = values;
        this.onOk(values);
        openModalDialog = null;
    },

    cancel : function() {
        this.onCancel();
        openModalDialog = null;        
        this._values = null;
    },
    
    hide : function() {
        return this._values;
    },
    
    onOk : function(values){
    },
    onCancel: function(){
    }
}

