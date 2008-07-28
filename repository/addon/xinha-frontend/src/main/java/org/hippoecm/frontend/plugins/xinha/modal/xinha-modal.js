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

ModalDialogImpl = function() {
};

ModalDialogImpl.prototype = {
    MODAL_DIALOG_PARAM: 'ModalDialogParam-',
    modalAction: null,
    
    openModal: function(componentUrl, plugin, action, init) {
        var str = componentUrl.indexOf('?') > -1 ? "&" : "?";
        str += 'pluginName=' + plugin;
        for(var i in init) {
            str += encodeURI(('&' + this.MODAL_DIALOG_PARAM + i + '=' + init[i]));
        }
        if(str.length > 1)
            componentUrl += str;
        var _this = this;
        var func = function() {
            _this.modalAction = action;
        }
        wicketAjaxGet(componentUrl, func, null, null);
    },
    
    closeModal: function(value) {
        if(this.modalAction != null) {
            this.modalAction(value);
        }
        this.modalAction = null;
    },
    
    cancelModal: function() {
        this.modalAction = null;        
    }
}

ModalDialog = new ModalDialogImpl();
