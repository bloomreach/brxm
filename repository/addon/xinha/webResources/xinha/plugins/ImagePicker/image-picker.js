//Copy from Xinha core InsertImage plugin, needed for changed dialog behavior

ImagePicker._pluginInfo = {
  name          : "ImagePicker",
  version       : "1.0",
  developer     : "Arthur Bogaart",
  sponsor       : "",
  sponsor_url   : "",
  license       : "" //TODO: which license?
};

function ImagePicker(editor) {
    this.config = editor.config;
    this.editor = editor;
    this.imgRE = new RegExp('<img[^>]+>', 'gi');
    this.srcRE = new RegExp('src="[^"]+"', 'i');
    this.noReplacePrefixes = ['http:', 'https:', '/'];
    cfg = editor.config;
    cfg.registerButton({
        id       : "imagepicker",
        tooltip  : this._lc("Image picker"),
        image    : [_editor_url + cfg.imgURL + 'ed_buttons_main.gif',6,3], //editor.imgURL("ed_abbreviation.gif", "Abbreviation"),
        textMode : false,
        action   : function(e) {
                     e.execCommand("insertimage");
                   }
      })
      cfg.addToolbarElement("imagepicker", "insertimage", 0);

} 

ImagePicker.prototype._lc = function(string) {
    return Xinha._lc(string, 'ImagePicker');
};

/**
 * Prefix relative image sources with binaries prefix
 */
ImagePicker.prototype.inwardHtml = function(html) {
    var _this = this;
    var prefix = this.getPrefix();    
    html = html.replace(this.imgRE, function(m) {
        m = m.replace(_this.srcRE, function(n) {
            var url = n.substring(5, n.length - 1);
            if(_this.shouldPrefix(url)) {
                return 'src="' + prefix + url + '"';
            }
            return n;
        });
        return m;
    });
    return html;
}

/**
 * Strip of binaries prefix
 */
ImagePicker.prototype.outwardHtml = function(html) {
    var _this = this;
    var prefix = this.getPrefix();
    html = html.replace(this.imgRE, function(m) {
        m = m.replace(_this.srcRE, function(n) {
            var idx = n.indexOf(prefix);
            if (idx > -1) {
                return 'src="' + n.substr(prefix.length + idx);
            }
            return n;
        });
        return m;
    });
    return html;
}

ImagePicker.prototype.getPrefix = function() {
    if (this.prefix == null) {
        this.prefix = this.editor.config.jcrNodePath;
        if (this.prefix.charAt(this.prefix.length - 1) != '/') {
            this.prefix += '/';
        }
    }
    return this.prefix;
}

ImagePicker.prototype.shouldPrefix = function(url) {
    if(this.checkPrefixes == null) {
        this.checkPrefixes = this.noReplacePrefixes;
        this.checkPrefixes.push(this.getPrefix());
    }    
    for(var i=0; i<this.checkPrefixes.length; i++) {
        if(url.indexOf(this.checkPrefixes[i]) == 0) 
            return false;
    }
    return true;
}

/**
 * Override Xinha._insertImage
 */
Xinha.prototype._insertImage = function(image)
{
  var editor = this;  // for nested functions
  var outparam;
  if ( typeof image == "undefined" )
  {
    image = this.getParentElement();
    if ( image && image.tagName.toLowerCase() != 'img' )
    {
      image = null;
    }
  }
  
  var base;
  if ( typeof editor.config.baseHref != 'undefined' && editor.config.baseHref !== null ) {
    base = editor.config.baseHref;
  }
  else {
    var bdir = window.location.toString().split("/");
    bdir.pop();
    base = bdir.join("/");
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
  /* if you want to understand why the above function is required, uncomment the two lines below and launch InsertImage in both Mozilla & IE with an image selected that hath neither value set and compare the results
  alert(image.vspace +' '+ image.getAttribute('vspace') + ' ' + image.getAttribute('vspace',2) + ' ' + getSpecifiedAttribute(image,'vspace') );
    alert(image.hspace +' '+ image.getAttribute('hspace') + ' ' + image.getAttribute('hspace',2) + ' ' + getSpecifiedAttribute(image,'hspace') );
  */
  outparam =
    {
      f_base   : base,
      f_url    : this.stripBaseURL(image.getAttribute('src',2)), // the second parameter makes IE return the value as it is set, as opposed to an "interpolated" (as MSDN calls it) value
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
    outparam =
    {
      f_base   : base,
      f_url    : ""      
    };
  }
  ModalDialog.openModal(
    editor.config.modalWindowScript,
    ImagePicker._pluginInfo.name,
    function(param)
    {
      // user must have pressed Cancel
      if ( !param )
      {
        return false;
      }
      
      var img = image;
      if ( !img )
      {
        if ( Xinha.is_ie )
        {
          var sel = editor.getSelection();
          var range = editor.createRange(sel);
          //TODO: check if this still works.
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
            if (value)
              img.align = value;
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
    },
    outparam);
};


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