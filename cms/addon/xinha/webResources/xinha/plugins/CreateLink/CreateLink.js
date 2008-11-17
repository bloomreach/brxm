function CreateLink(editor, args) {
    this.editor = editor;
    var cfg = editor.config;
    var self = this;

    editor.config.btnList.createlink[3] = function() {
        self.show(self._getSelectedAnchor()); 
    }
}

CreateLink._pluginInfo = {
    name :"CreateLink",
    version :"1.0",
    developer :"Arthur Bogaart",
    developer_url :"http://www.onehippo.com/",
    c_owner :"OneHippo",
    license :"al2",
    sponsor :"OneHippo",
    sponsor_url :"http://www.onehippo.com/"
};

Xinha.Config.prototype.CreateLink= {
    callbackUrl: null        
};

CreateLink.prototype._lc = function(string) {
    return Xinha._lc(string, 'Xinha');
};

CreateLink.prototype.onGenerateOnce = function()
{
  CreateLink.loadAssets();
};

CreateLink.loadAssets = function()
{
    var self = CreateLink;
    if (self.loading) return;
    self.loading = true;
    Xinha._getback(_editor_url + 'modules/CreateLink/pluginMethods.js', function(getback) { eval(getback); self.methodsReady = true; });
}


CreateLink.prototype.onUpdateToolbar = function()
{ 
    if (!(ModalDialog && CreateLink.methodsReady))
    {
        this.editor._toolbarObjects.createlink.state("enabled", false);
    }
    else this.onUpdateToolbar = null;
};


CreateLink.prototype.prepareDialog = function()
{
    var config = this.editor.config;
    var callbackUrl = config.CreateLink.callbackUrl;

    this.dialog = new ModalDialog(callbackUrl, CreateLink._pluginInfo.name, config.xinhaParamToken, this.editor);
    // Connect the OK button
    var self = this;
    this.dialog.onOk = function(values) {
        self.apply();
        self.editor.plugins.AutoSave.instance.saveSynchronous();
    };
    this.dialogReady = true;
};

CreateLink.prototype._getSelectedAnchor = function() {
    var sel = this.editor.getSelection();
    var rng = this.editor.createRange(sel);
    var a = this.editor.activeElement(sel);
    if (a != null && a.tagName.toLowerCase() == 'a') {
        return a;
    } else {
        a = this.editor._getFirstAncestor(sel, 'a');
        if (a != null) {
            return a;
        }
    }
    return null;
};

/**
 * Add link from outside Xinha (like DragDrop)
 */

CreateLink.prototype.createLink = function(values, openModal) {
    if (!this.dialog)
    {
      this.prepareDialog();
    }
    this.dialog.close(values);
    if(openModal) {
        this.dialog.show(values);
    }
}