/*
 * Copyright (C) 2011 by Marijn Haverbeke <marijnh@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * Please note that some subdirectories of the CodeMirror distribution
 * include their own LICENSE files, and are released under different
 * licences.
 */
(function() {
  CodeMirror.simpleHint = function(editor, getHints) {
    // We want a single cursor position.
    if (editor.somethingSelected()) return;
    var result = getHints(editor);
    if (!result || !result.list.length) return;
    var completions = result.list;
    function insert(str) {
      editor.replaceRange(str, result.from, result.to);
    }
    // When there is only one completion, use it directly.
    if (completions.length == 1) {insert(completions[0]); return true;}

    // Build the select widget
    var complete = document.createElement("div");
    complete.className = "CodeMirror-completions";
    var sel = complete.appendChild(document.createElement("select"));
    // Opera doesn't move the selection when pressing up/down in a
    // multi-select, but it does properly support the size property on
    // single-selects, so no multi-select is necessary.
    if (!window.opera) sel.multiple = true;
    for (var i = 0; i < completions.length; ++i) {
      var opt = sel.appendChild(document.createElement("option"));
      opt.appendChild(document.createTextNode(completions[i]));
    }
    sel.firstChild.selected = true;
    sel.size = Math.min(10, completions.length);
    var pos = editor.cursorCoords();
    complete.style.left = pos.x + "px";
    complete.style.top = pos.yBot + "px";
    document.body.appendChild(complete);
    // Hack to hide the scrollbar.
    if (completions.length <= 10)
      complete.style.width = (sel.clientWidth - 1) + "px";

    var done = false;
    function close() {
      if (done) return;
      done = true;
      complete.parentNode.removeChild(complete);
    }
    function pick() {
      insert(completions[sel.selectedIndex]);
      close();
      setTimeout(function(){editor.focus();}, 50);
    }
    CodeMirror.connect(sel, "blur", close);
    CodeMirror.connect(sel, "keydown", function(event) {
      var code = event.keyCode;
      // Enter
      if (code == 13) {CodeMirror.e_stop(event); pick();}
      // Escape
      else if (code == 27) {CodeMirror.e_stop(event); close(); editor.focus();}
      else if (code != 38 && code != 40) {
        close(); editor.focus();
        setTimeout(function(){CodeMirror.simpleHint(editor, getHints);}, 50);
      }
    });
    CodeMirror.connect(sel, "dblclick", pick);

    sel.focus();
    // Opera sometimes ignores focusing a freshly created node
    if (window.opera) setTimeout(function(){if (!done) sel.focus();}, 100);
    return true;
  };
})();
