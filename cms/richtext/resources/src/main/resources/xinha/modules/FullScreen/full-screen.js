/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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

function FullScreen(editor, args) {
  this.editor = editor;
  this.originalSizes = null;
  editor._superclean_on = false;
  var cfg = editor.config;

  cfg.registerIcon('fullscreen', [_editor_url + cfg.imgURL + 'ed_buttons_main.png',8,0]);
  cfg.registerIcon('fullscreenrestore', [_editor_url + cfg.imgURL + 'ed_buttons_main.png',9,0]);

  cfg.registerButton
      ( 'fullscreen',
          this._lc("Maximize/Minimize Editor"),
          cfg.iconList.fullscreen,
          true,
          function(e, objname, obj) {
            e._fullscreen();
          }
      );

  // See if we can find 'popupeditor' and replace it with fullscreen
  cfg.addToolbarElement("fullscreen", "popupeditor", 0);
}

FullScreen._pluginInfo =
{
  name     : "FullScreen",
  version  : "1.0",
  developer: "AB",
  developer_url: "http://www.onehippo.com/",
  c_owner      : "OneHippo",
  license      : "al2",
  sponsor      : "",
  sponsor_url  : ""
};

FullScreen.prototype._lc = function(string) {
  return Xinha._lc(string, {url : _editor_url + 'modules/FullScreen/lang/',context:"FullScreen"});
};

/** fullScreen makes an editor take up the full window space (and resizes when the browser is resized)
 *  the principle is the same as the "popupwindow" functionality in the original htmlArea, except
 *  this one doesn't popup a window (it just uses to positioning hackery) so it's much more reliable
 *  and much faster to switch between
 */

Xinha.prototype._fullscreen = function(resize) {
  var e = this;
  var cfg = e.config;
  function sizeItUp() {
    if (!e._isFullScreen || e._sizing) {
      return false;
    }

    e._sizing = true;
    // Width & Height of window
    var dim = Xinha.viewportSize();
    if (e.originalSizes == null) {
      e.originalSizes = {
        x:   parseInt(e._htmlArea.style.width),
        y:   parseInt(e._htmlArea.style.height),
        dim: dim
      };
    }

    var h = dim.y - e.config.fullScreenMargins[0] - e.config.fullScreenMargins[2];
    var w = dim.x - e.config.fullScreenMargins[1] - e.config.fullScreenMargins[3];

    e.sizeEditor(w + 'px', h + 'px', e.config.sizeIncludesBars, e.config.sizeIncludesPanels);
    e._sizing = false;
    if ( e._toolbarObjects.fullscreen ) e._toolbarObjects.fullscreen.swapImage(cfg.iconList.fullscreenrestore);

    //Because of HREPTWO-3990 we have to use a custom setHTML implementation that doesn't work in fullscreen.
    //As a workaround we disable the custom undo behavior that Xinha provides in fullscreen mode, see CMS7-5847 
    if(Xinha.ie_version == 7 || Xinha.ie_version == 8) {
      e._customUndo = false;
    }

  }

  function sizeItDown() {
    if (e._isFullScreen || e._sizing) return false;
    e._sizing = true;

    if (e.originalSizes != null) {
      var os = e.originalSizes;
      var nDim = Xinha.viewportSize();
      var nW = os.x + (nDim.x - os.dim.x);
      var nH = os.y + (nDim.y - os.dim.y);
      e.sizeEditor( nW + 'px', nH + 'px', e.config.sizeIncludesBars, e.config.sizeIncludesPanels);
      e.originalSizes = null;
    } else {
      e.initSize();
    }

    e._sizing = false;
    if ( e._toolbarObjects.fullscreen ) e._toolbarObjects.fullscreen.swapImage(cfg.iconList.fullscreen);

    //See CMS7-5847
    if(Xinha.ie_version == 7 || Xinha.ie_version == 8) {
      e._customUndo = true;
    }
  }

  /** It's not possible to reliably get scroll events, particularly when we are hiding the scrollbars
   *   so we just reset the scroll ever so often while in fullscreen mode
   */
  function resetScroll() {
    if (e._isFullScreen) {
      window.scroll(0, 0);
      window.setTimeout(resetScroll, 150);
    }
  }

  if (resize === true) {
    sizeItUp();
    return;
  }


  if (typeof this._isFullScreen == 'undefined') {
    this._isFullScreen = false;
  }

  var selection = this.saveSelection();

  // Gecko has a bug where if you change position/display on a
  // designMode iframe that designMode dies.
  if (Xinha.is_gecko) {
    this.deactivateEditor();
  }

  if (this._isFullScreen) {

    // Unmaximize
    this._htmlArea.style.position = '';
    if (!Xinha.is_ie) this._htmlArea.style.border = '';

    try {
      if (Xinha.is_ie && document.compatMode == 'CSS1Compat') {
        var bod = document.getElementsByTagName('html');
      }
      else {
        var bod = document.getElementsByTagName('body');
      }
      bod[0].style.overflow = '';
    }
    catch(e) {
      // Nutthin
    }
    this._isFullScreen = false;
    sizeItDown();

    // Restore all ancestor positions
    var ancestor = this._htmlArea;
    while ((ancestor = ancestor.parentNode) && ancestor.style) {
      ancestor.style.position = ancestor._xinha_fullScreenOldPosition;
      ancestor._xinha_fullScreenOldPosition = null;

      if (ancestor.className == 'yui-layout-doc') {
        for (var i = 0; i < ancestor.childNodes.length; i++) {
          var c = ancestor.childNodes[i];
          c.style.position = c._xinha_fullScreenOldPosition;
          c._xinha_fullScreenOldPosition = null;
        }
      }
    }

    this._iframe.className = this._iframe.classNameOld;
    this._iframe.classNameOld = null;

    if (Xinha.ie_version < 7) {
      var selects = document.getElementsByTagName("select");
      for (var i = 0; i < selects.length; ++i) {
        selects[i].style.visibility = 'visible';
      }
    }
    window.scroll(this._unScroll.x, this._unScroll.y);

    if (Xinha.ie_version < 9) {
      document.body.removeChild(this._htmlArea);
      this._parentNode.appendChild(this._htmlArea);
    }

  }
  else {//Maximize

    // Get the current Scroll Positions
    this._unScroll =
    {
      x:(window.pageXOffset) ? (window.pageXOffset) : (document.documentElement) ? document.documentElement.scrollLeft : document.body.scrollLeft,
      y:(window.pageYOffset) ? (window.pageYOffset) : (document.documentElement) ? document.documentElement.scrollTop : document.body.scrollTop
    };

    if (Xinha.ie_version < 9) {
      this._parentNode = this._htmlArea.parentNode;
      this._htmlArea.parentNode.removeChild(this._htmlArea);
      document.body.appendChild(this._htmlArea);
    }
    // Make all ancestors position = static
    var ancestor = this._htmlArea;
    while ((ancestor = ancestor.parentNode) && ancestor.style) {
      if (ancestor.className == 'yui-layout-doc') {
        for (var i = 0; i < ancestor.childNodes.length; i++) {
          var c = ancestor.childNodes[i];
          c._xinha_fullScreenOldPosition = ancestor.style.position;
          c.style.position = 'static';
        }
      }
      ancestor._xinha_fullScreenOldPosition = ancestor.style.position;
      ancestor.style.position = 'static';
    }

    this._iframe.classNameOld = this._iframe.className;
    this._iframe.className = 'xinha_fullscreen';

    // very ugly bug in IE < 7 shows select boxes through elements that are positioned over them
    if (Xinha.ie_version < 7) {
      var selects = document.getElementsByTagName("select");
      var s, currentEditor;
      for (var i = 0; i < selects.length; ++i) {
        s = selects[i];
        currentEditor = false;
        while (s = s.parentNode) {
          if (s == this._htmlArea) {
            currentEditor = true;
            break;
          }
        }
        if (!currentEditor && selects[i].style.visibility != 'hidden') {
          selects[i].style.visibility = 'hidden';
        }
      }
    }


    // Maximize
    window.scroll(0, 0);
    this._htmlArea.style.position = 'absolute';
    this._htmlArea.style.zIndex = 999;
    this._htmlArea.style.left = e.config.fullScreenMargins[3] + 'px';
    this._htmlArea.style.top = e.config.fullScreenMargins[0] + 'px';
    if (!Xinha.is_ie && !Xinha.is_webkit) this._htmlArea.style.border = 'none';
    this._isFullScreen = true;
    resetScroll();

    try {
      if (Xinha.is_ie && document.compatMode == 'CSS1Compat') {
        var bod = document.getElementsByTagName('html');
      }
      else {
        var bod = document.getElementsByTagName('body');
      }
      bod[0].style.overflow = 'hidden';
    }
    catch(e) {
      // Nutthin
    }

    sizeItUp();
  }

  if (Xinha.is_gecko) {
    this.activateEditor();
    try {
      var node = YAHOO.util.Selector.query('td.toolbarElement', this._toolbar, true);
      YAHOO.util.Dom.getFirstChild(node).focus();
    } catch(e) {
      //error not important for user
    }
  }
  this.restoreSelection(selection);
  this.focusEditor();
};
