/*
 * Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

ModalDialog = function(url, plugin, token, editor) {
    this.editor = editor;
    this.callbackUrl = url; 
    this.callbackUrl += url.indexOf('?') > -1 ? "&" : "?";
    this.callbackUrl += ('pluginName=' + encodeURIComponent(plugin));
    this.token = token;
};

ModalDialog.prototype = {
    _values : null,
    _lastRange: null,
        
    show : function(parameters) {
        this.saveState();
        var url = this.callbackUrl;
        for (var p in parameters) {
            url += ('&' + this.token + p + '=' + encodeURIComponent(parameters[p]));
        }
        wicketAjaxGet(url, null, null, null);
        openModalDialog = this;
    },

    close : function(values) {
        this.restoreState();
        this._values = values;
        this.onOk(values);
        openModalDialog = null;
    },

    cancel : function() {
        this.restoreState();
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
    },
    
    saveState: function() {
        // We need to preserve the selection
        // if this is called before some editor has been activated, it activates the editor
        if (Xinha._someEditorHasBeenActivated)
        {
          this._lastRange = this.editor.saveSelection();
        }
        this.editor.deactivateEditor();
        this.editor.suspendUpdateToolbar = true;
        this.editor.currentModal = this;
    },
    
    restoreState: function() {
        if(this.editor.editorIsActivated() && Xinha._currentlyActiveEditor) {
            return;
        }
        this.editor.suspendUpdateToolbar = false;
        this.editor.currentModal = null;
        this.editor.activateEditor();
        if (this._lastRange != null) {
            try {
                this.editor.restoreSelection(this._lastRange);
            } catch(e) {
                //A bug in IE brings us here. Ignore it.
            }
         }
        this.editor.updateToolbar();
        this.editor.focusEditor();
    }
}

