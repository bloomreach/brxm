  
InsertImage._pluginInfo = {
  name          : "InsertImage",
  origin        : "Xinha Core override",
  version       : "$LastChangedRevision: 1055 $".replace(/^[^:]*:\s*(.*)\s*\$$/, '$1'),
  developer     : "Arthur Bogaart (OneHippo)",
  developer_url :"http://www.onehippo.com/",
  c_owner :"OneHippo",
  license :"al2",
  sponsor :"OneHippo",
  sponsor_url :"http://www.onehippo.com/"
};

function InsertImage(editor) {
    this.editor = editor;
    var cfg = editor.config;
    var self = this;

    editor.config.btnList.insertimage[3] = function() { self.show(); }

    this.imgRE = new RegExp('<img[^>]+>', 'gi');
    this.srcRE = new RegExp('src="[^"]+"', 'i');
    this.facetselectRE = new RegExp('facetselect="([^"]+)"', 'i');
    this.noReplacePrefixes = ['http:', 'https:', '/'];
}

Xinha.Config.prototype.InsertImage = {
        callbackUrl: null        
};

InsertImage.prototype._lc = function(string) {
    return Xinha._lc(string, 'Xinha');
};

InsertImage.prototype.onGenerateOnce = function()
{
};

InsertImage.prototype.prepareDialog = function()
{
    var config = this.editor.config;
    var callbackUrl = config.InsertImage.callbackUrl;

    this.dialog = new ModalDialog(callbackUrl, InsertImage._pluginInfo.name, config.xinhaParamToken, this.editor);
    // Connect the OK button
    var self = this;
    this.dialog.onOk = function(values) {
        
        //Workaround for strange value.value approach of f_align in Xinha/modules/InsertImage/pluginMethods.js ..
        if(!YAHOO.lang.isUndefined(values.f_align)) {
            values.f_align = {value: values.f_align};
        }
        
        var img = self.image;
        if(values.f_url == '' && img != null) { //Image should be removed
            var p = img.parentNode;
            while(img.hasChildNodes())
            {
                p.insertBefore(img.removeChild(img.childNodes[0]), img);
            }
            p.removeChild(img);
            self.editor.updateToolbar();
        } else {
            self.apply();
        }
        self.editor.plugins.AutoSave.instance.save();
    };
    this.dialogReady = true;
};


InsertImage.prototype.insertImage = function(values, openModal) {
    if (!this.dialog)
    {
      this.prepareDialog();
    }
    this.dialog.close(values);
    if(openModal) {
        this.dialog.show(values);
    }
}

/**
 * Prefix relative image sources with binaries prefix
 */
InsertImage.prototype.inwardHtml = function(html) {
    var _this = this;
    var prefix = this.getPrefix();
    var isPortletContext = this.isPortletContext();
    html = html.replace(this.imgRE, function(m) {
        if (_this.facetselectRE.test(m)) {
            var facetselect = RegExp.$1;
            m = m.replace(_this.srcRE, function(n) {
                if (!isPortletContext)
                    return 'src="' + prefix + facetselect + '"';
                else
                    return 'src="' + prefix + "?_path=" + facetselect + '"';
            });
        } else {
            m = m.replace(_this.srcRE, function(n) {
                var url = n.substring(5, n.length - 1);
                if (_this.shouldPrefix(url)) {
                    return 'src="' + prefix + url + '"';
                }
                return n;
            });
        }
        return m;
    });
    return html;
}

/**
 * Strip of binaries prefix
 */
InsertImage.prototype.outwardHtml = function(html) {
    var _this = this;
    var prefix = this.getPrefix();
    html = html.replace(this.imgRE, function(m) {
        if (_this.facetselectRE.test(m)) {
            var facetselect = RegExp.$1;
            m = m.replace(_this.srcRE, function(n) {
                return 'src="' + facetselect + '"';
            });
            return m;
        } else {
            m = m.replace(_this.srcRE, function(n) {
                var idx = n.indexOf(prefix);
                if (idx > -1) {
                    return 'src="' + n.substr(prefix.length + idx);
                }
                return n;
            });
            return m;
        }
    });
    return html;
}

InsertImage.prototype.getPrefix = function() {
    if (this.prefix == null) {
        this.prefix = this.editor.config.prefix;
        if (this.prefix.charAt(this.prefix.length - 1) != '/') {
            this.prefix += '/';
        }
    }
    return this.prefix;
}

InsertImage.prototype.shouldPrefix = function(url) {
    for (var i=0; i<this.noReplacePrefixes.length; i++) {
        if(url.indexOf(this.noReplacePrefixes[i]) == 0) 
            return false;
    }
    return true;
}

InsertImage.prototype.isPortletContext = function() {
    return (this.editor.config.isPortletContext);
}

//Workaround for method not found error
InsertImage.prototype.stripBaseURL = function(url) {
    return url;
}

InsertImage.prototype.show = function(image)
{
        if (!this.dialog) this.prepareDialog();
  
  var editor = this.editor;
        if ( typeof image == "undefined" )
  {
    image = editor.getParentElement();
    if ( image && image.tagName.toLowerCase() != 'img' )
    {
      image = null;
    }
  }

  if ( image )
  {
    function getSpecifiedAttribute(element,attribute)
    {
      var a = element.attributes;
      for (var i=0;i<a.length;i++)
      {
        if (a[i].nodeName == attribute && a[i].specified)
        {
          return a[i].value;
        }
      }
      return '';
    }
    outparam =
    {
      f_url    : this.stripBaseURL(image.getAttribute('src',2)), // the second parameter makes IE return the value as it is set, as opposed to an "interpolated" (as MSDN calls it) value
      f_facetselect : getSpecifiedAttribute(image,'facetselect'),
      f_alt    : image.alt,
      f_border : image.border,
      f_align  : image.align,
      f_vert   : getSpecifiedAttribute(image,'vspace'),
      f_horiz  : getSpecifiedAttribute(image,'hspace'),
      f_width  : image.width,
      f_height : image.height
    };
  }
  else{
  {
    outparam =
    {
      f_url    : '',
      f_facetselect  : '',
      f_alt    : '',
      f_border : '',
      f_align  : '',
      f_vert   : '',
      f_horiz  : '',
      f_width  : '',
      f_height : ''
    };
  }
  }
  this.image = image;
  // now calling the show method of the Xinha.Dialog object to set the values and show the actual dialog
  this.dialog.show(outparam);
};

// and finally ... take some action
InsertImage.prototype.apply = function()
{
  var param = this.dialog.hide();
  if (!param.f_url)
  {
    return;
  }
  var editor = this.editor;
  var img = this.image;
  if ( !img )
  {
    if ( Xinha.is_ie )
    {
      var sel = editor.getSelection();
      var range = editor.createRange(sel);
      editor._doc.execCommand("insertimage", false, param.f_url);
      img = range.parentElement();
      // wonder if this works...
      if ( img.tagName.toLowerCase() != "img" )
      {
        img = img.previousSibling;
      }
    }
    else
    {
      img = document.createElement('img');
      img.src = param.f_url;
      editor.insertNodeAtSelection(img);
      if ( !img.tagName )
      {
        // if the cursor is at the beginning of the document
        img = range.startContainer.firstChild;
      }
    }
    this.image = img;
  }
  else
  {
    img.src = param.f_url;
  }
  
  for ( var field in param )
  {
    var value = param[field];
    switch (field)
    {
      case "f_facetselect":
      if (value)
      img.setAttribute("facetselect",value);
      else
      img.removeAttribute("facetselect");
      break;
      case "f_alt":
      if (value)
      img.alt = value;
      else
      img.removeAttribute("alt");
      break;
      case "f_border":
      if (value)
      img.border = parseInt(value || "0");
      else
      img.removeAttribute("border");
      break;
      case "f_align":
      if (value.value)
      img.align = value.value;
      else
      img.removeAttribute("align");
      break;
      case "f_vert":
      if (value != "")
      img.vspace = parseInt(value || "0");
      else
      img.removeAttribute("vspace");
      break;
      case "f_horiz":
      if (value != "")
      img.hspace = parseInt(value || "0");
      else
      img.removeAttribute("hspace");
      break;
      case "f_width":
      if (value)
      img.width = parseInt(value || "0");
      else
      img.removeAttribute("width");
      break;
      case "f_height":
      if (value)
      img.height = parseInt(value || "0");
      else
      img.removeAttribute("height");
      break;
    }
  }
};
