function AutoSave(editor) {
    this.editor = editor;
    this.changed = false;
    this.timeoutId = null;
    this.timeoutLength = 2000;
    var self = this;
    var cfg = editor.config;
    this.textarea = this.editor._textArea;
    this.initial_html = null;
}

AutoSave.prototype._lc = function(string) {
    return Xinha._lc(string, 'SaveSubmit');
}

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
    this.timeoutId = window.setTimeout("saveContent()", this.timeoutLength);
}

AutoSave.prototype.setUnChanged = function() {
    this.changed = false;
}

AutoSave.prototype.save =  function() {
    var editor = this.editor;
    var self = this;
    var form = editor._textArea.form;
    form.onsubmit();
    var callbackUrl = editor._textArea.getAttribute('callbackUrl');
    var myId = editor._textArea.getAttribute("id");
    
    var postSuccesFunc = function() {
      self.initial_html = self.editor.getInnerHTML();
      self.changed = false;
    };
    var acall = wicketAjaxPost(callbackUrl, wicketSerialize(Wicket.$(myId)), postSuccesFunc, null);
}

