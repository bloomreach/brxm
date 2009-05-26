AutoSave._pluginInfo = {
  name          : "AutoSave",
  version       : "1.0",
  developer     : "Arthur Bogaart",
  developer_url : "http://www.hippo.nl",
  c_owner       : "Arthur Bogaart",
  sponsor       : "",
  sponsor_url   : "",
  license       : "htmlArea"
}

Xinha.Config.prototype.AutoSave =
{
  'timeoutLength' : 2000,
  'callbackUrl' : '?save=true',
  'saveSuccessFlag' : 'XINHA_SAVED_FLAG'
}

function AutoSave(editor) {
    this.editor = editor;
    this.lConfig = editor.config.AutoSave;
    
//    this.autoSave = this;
//    this.changed = false;
//    this.timeoutId = null;
//
//    this.textarea = this.editor._textArea;
//    this.initial_html = null;
}

AutoSave.prototype._lc = function(string) {
    return Xinha._lc(string, 'AutoSave');
}

AutoSave.prototype.onGenerateOnce = function() {
    YAHOO.hippo.EditorManager.register(this, this.lConfig.timeoutLength);
}

//Interface
// - String getId
// - String getContents
// - void save


AutoSave.prototype.getId = function() {
    return this.editor._textArea.getAttribute("id");
}

AutoSave.prototype.getContents = function() {
    return this.editor.getInnerHTML();
}

AutoSave.prototype.saveOld = function() {
    var myId = this.getId();
    var self = this;
    var form = this.editor._textArea.form;
    form.onsubmit();
    var callbackUrl = this.editor.config.callbackUrl + "&save=true";

    return wicketAjaxPost(callbackUrl, wicketSerialize(Wicket.$(myId)), function() {
        self.initial_html = self.editor.getInnerHTML();
        self.changed = false;
    }, null, function() { return Wicket.$(myId) != null; }, 'hmmmmm|d');
}

AutoSave.prototype.save = function() {
    
    var xmlHttpReq = false;
    var self = this;
    // Mozilla/Safari
    if (window.XMLHttpRequest) {
        self.xmlHttpReq = new XMLHttpRequest();
    }
    // IE
    else if (window.ActiveXObject) {
        self.xmlHttpReq = new ActiveXObject("Microsoft.XMLHTTP");
    }

    var form = this.editor._textArea.form;
    form.onsubmit();
    var callbackUrl = this.editor.config.callbackUrl + "&save=true";

    self.xmlHttpReq.open('POST', callbackUrl, false);
    self.xmlHttpReq.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
    self.xmlHttpReq.onreadystatechange = function() {
        if (self.xmlHttpReq.readyState == 4) {
            //console.log('AJAX-UPDATE: ' + self.xmlHttpReq.responseText);
        }
    }
    self.xmlHttpReq.send(wicketSerialize(Wicket.$(this.getId())));
}


AutoSave.prototype.onUpdateToolbar = function() {
  this.checkChanges(null);
}

AutoSave.prototype.onKeyPress = function(ev) {
  this.checkChanges(ev);
}

AutoSave.prototype.checkChanges = function(ev) {
    if ( ev != null && ev.ctrlKey && this.editor.getKey(ev) == 's') {
        YAHOO.hippo.EditorManager.resetById(this.getId());
        this.save();
        Xinha._stopEvent(ev);
        return true;
    }
    
    YAHOO.hippo.EditorManager.check(this.getId());   
}

AutoSave.prototype.onExecCommand = function (cmd) {
    if (this.changed && cmd == 'undo') {
        if (this.initial_html == this.editor.getInnerHTML()) this.setUnChanged();
        return false;
    }
}

AutoSave.prototype.getChanged = function() {
    if (this.initial_html == null) this.initial_html = this.editor.getInnerHTML();
    if (!this.changed && this.initial_html != this.editor.getInnerHTML()) {
        return true;
    }
    else return false;
}

AutoSave.prototype.setTimer = function() {
    this.clearTimer();
    var self = this;
    YAHOO.hippo.EditorManager.set(this.editor.id, function() { self.save() }, this.lConfig.timeoutLength);
}

AutoSave.prototype.clearTimer = function() {
    YAHOO.hippo.EditorManager.reset(this.editor.id);
}

AutoSave.prototype.setUnChanged = function() {
    this.changed = false;
}
        
