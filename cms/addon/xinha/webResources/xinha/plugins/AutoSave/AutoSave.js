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
    this.autoSave = this;
    
    this.changed = false;
    this.timeoutId = null;

    this.textarea = this.editor._textArea;
    this.initial_html = null;
    
    var self = this;
    preCallHandler = function() {
      if (self.timeoutId != null) {
        window.clearTimeout(self.timeoutId);
        self.timeoutId = null;
        self.save();
      }
    }
    Wicket.Ajax.registerPreCallHandler(preCallHandler);
}

AutoSave.prototype._lc = function(string) {
    return Xinha._lc(string, 'AutoSave');
}

AutoSave.prototype.onGenerateOnce = function() {
    this.initial_html = this.editor.getInnerHTML();
}

AutoSave.prototype.onUpdateToolbar = function() {
  this.checkChanges(null);
}

AutoSave.prototype.onKeyPress = function(ev) {
  this.checkChanges(ev);
}

AutoSave.prototype.checkChanges = function(ev) {
    if(this.timeoutId != null) {
      window.clearTimeout(this.timeoutId);
      this.timeoutId = null;
    }  

    if ( ev != null && ev.ctrlKey && this.editor.getKey(ev) == 's') {
            this.save();
            Xinha._stopEvent(ev);
            return true;
    }
    else {
        if (this.changed) {
          this.setTimer();
        } else {
            if (this.getChanged()) { 
              this.changed = true;
              this.setTimer();
            }
        }
        return false;
    }
}

AutoSave.prototype.onExecCommand = function (cmd) {
    if (this.changed && cmd == 'undo') { 
        if (this.initial_html == this.editor.getInnerHTML()) this.setUnChanged();
        return false;
    }
}

AutoSave.prototype.getChanged = function() {
    if (this.initial_html == null) this.initial_html = this.editor.getInnerHTML();
    if (this.initial_html != this.editor.getInnerHTML() && this.changed == false) {
        return true;
    }
    else return false;
}

AutoSave.prototype.setTimer = function() {
    window.clearTimeout(this.timeoutId);
    var self = this;
    saveContent = function() { self.save() } ;
    this.timeoutId = window.setTimeout("saveContent()", this.lConfig.timeoutLength);
}

AutoSave.prototype.setUnChanged = function() {
    this.changed = false;
}
        
AutoSave.prototype.save = function() {
    var self = this;
    this.timoutId = null;
    var form = this.editor._textArea.form;
    form.onsubmit();
    var callbackUrl = this.editor.config.callbackUrl + "&save=true";
    var myId = this.editor._textArea.getAttribute("id");

    return wicketAjaxPost(callbackUrl, wicketSerialize(Wicket.$(myId)), function() {
        self.initial_html = self.editor.getInnerHTML();
        self.changed = false;
    }, null, function() { return Wicket.$(myId) != null; });
}
