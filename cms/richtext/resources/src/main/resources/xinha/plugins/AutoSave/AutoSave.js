/*
 * Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

AutoSave._pluginInfo = {
    name         : "AutoSave",
    version      : "1.0",
    developer    : "a.bogaart@1hippo.com",
    developer_url: "http://www.onehippo.com",
    c_owner      : "OneHippo",
    license      : "al2",
    sponsor      : "OneHippo",
    sponsor_url  : "http://www.onehippo.com"
};

Xinha.Config.prototype.AutoSave = {
    timeoutLength : 2000,
    callbackUrl : ''
};

function AutoSave(editor) {
    this.editor = editor;
    this.lConfig = editor.config.AutoSave;

    this.timeoutID = null; // timeout ID, editor is dirty when non-null
    this.saving = false;   // whether a save is in progress

    // translation context
    this.context = {
        url : _editor_url + 'plugins/AutoSave/lang/',
        context: "AutoSave"
    };

    //Atach onkeyup and onchange event listeners to textarea for autosaving in htmlmode
    var txtArea = this.editor._textArea,
        cfg = this.editor.config,
        onChange = XinhaTools.proxy(this.checkChanges, this);

    if (YAHOO.util.Event) {
        YAHOO.util.Event.addListener(txtArea, 'keyup', onChange);
        YAHOO.util.Event.addListener(txtArea, 'cut', onChange);
        YAHOO.util.Event.addListener(txtArea, 'paste', onChange);
    } else {
        if (txtArea.addEventListener) {
            txtArea.addEventListener('keyup', onChange, false);
            txtArea.addEventListener('cut', onChange, false);
            txtArea.addEventListener('paste', onChange, false);
        } else if (txtArea.attachEvent) {
            txtArea.attachEvent('onkeyup', onChange);
            txtArea.attachEvent('cut', onChange);
            txtArea.attachEvent('paste', onChange);
        } else {
            txtArea['onkeyup'] = onChange;
            txtArea['cut'] = onChange;
            txtArea['paste'] = onChange;
        }
    }

    // optional button for explicit save
    cfg.registerIcon('save', [_editor_url + cfg.imgURL + 'ed_buttons_main.png', 9, 1]);
    cfg.registerButton('save-and-close', this._lc("Save and close"), cfg.iconList.save, true,
            XinhaTools.proxy(this.saveAndClose, this));
}

AutoSave.prototype = {
    _lc : function(string) {
        return Xinha._lc(string, this.context);
    },

    getId : function() {
        return this.editor._textArea.getAttribute("id");
    },

    getContents : function() {
        return this.editor.getInnerHTML();
    },

    saveAndClose : function() {
        var id = this.getId(),
            close = XinhaTools.proxy(function() {
                var plugin = XinhaTools.getPlugin(this.editor, 'StateChange');
                if (plugin !== null) {
                    plugin.setActivated(false, function() {
                        // Deactivate editor at context. Although this happens after the render() has been called,
                        // it is enough to simply remove the editor from the activeEditors list.
                        YAHOO.hippo.EditorManager.deactivateEditor(id);
                    });
                }
            }, this);

        if (this.timeoutID === null) {
            close();
        } else {
            this.save(false, close, close);
        }
    },

    // Save the contents of the xinha field.  Only one throttled request is executed concurrently.
    save : function(throttled, success, failure) {
        // nothing to do if editor is not dirty
        if (this.timeoutID === null) {
            return;
        }

        //else clear current timeout and continue save
        window.clearTimeout(this.timeoutID);

        if (throttled) {
            // reschedule when a save is already in progress
            if (this.saving) {
                this.checkChanges();
                return;
            }
            this.saving = true;
        }

        this.timeoutID = null;

        if (this.editor._editMode === 'wysiwyg') { //save Iframe html into textarea
            this.editor._textArea.value = this.editor.outwardHtml(this.editor.getHTML());
        }

        var xmlHttpReq = window.XMLHttpRequest ? new XMLHttpRequest() : new ActiveXObject("Microsoft.XMLHTTP"),
            url = this.editor.config.callbackUrl,
            serializedInput = Wicket.Form.serializeInput(Wicket.$(this.getId())),
            body = serializedInput[0].name + '=' + serializedInput[0].value,
            afterCallbackHandler = jQuery.proxy(function() {
                if (throttled) {
                    this.saving = false;
                }
                if (xmlHttpReq.status === 200) {
                    if (success) {
                        success();
                    }
                } else if (failure) {
                    failure();
                }
            }, this);

        xmlHttpReq.open('POST', url, throttled || false);
        xmlHttpReq.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
        xmlHttpReq.setRequestHeader('Wicket-Ajax', "true");
        xmlHttpReq.setRequestHeader('Wicket-Ajax-BaseURL', Wicket.Ajax.baseUrl);
        if (throttled) {
            xmlHttpReq.onreadystatechange = function() {
                if (xmlHttpReq.readyState === 4) {
                    afterCallbackHandler();
                }
            };
        }
        xmlHttpReq.send(body);

        if (!throttled) {
            afterCallbackHandler();
        }
    },

    onUpdateToolbar : function() {
        this.checkChanges();
    },

    onKeyPress : function(ev) {
        if (ev !== null && ev.ctrlKey && this.editor.getKey(ev) === 's') {
            this.save(true);
            Xinha._stopEvent(ev);
            return true;
        }
        this.checkChanges();
    },

    checkChanges : function() {
        if (this.timeoutID !== null) {
            window.clearTimeout(this.timeoutID);
        }

        var editorId = this.getId();
        this.timeoutID = window.setTimeout(function() {
            YAHOO.hippo.EditorManager.saveByTextareaId(editorId);
        }, this.lConfig.timeoutLength);
    },

    /**
     * Explicitly replace <p> </p> with general-purpose space (U+0020) with a <p> </p> including a non-breaking space (U+00A0)
     * to prevent the browser from not rendering these paragraphs
     *
     * See http://issues.onehippo.com/browse/HREPTWO-1713 for more info
     */
    inwardHtml : function(html) {
        this.imgRE = new RegExp('<p> <\/p>', 'gi');
        html = html.replace(this.imgRE, function(m) {
            return '<p>&nbsp;</p>';
        });
        return html;
    }
};
