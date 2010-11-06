/*
 * Copyright 2010 Hippo
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

if (typeof(Hippo) == 'undefined') {
  Hippo = {};
}

Hippo.Translation = {};

/*
 * render path from a list of T9Node ancestors
 */
Hippo.Translation.renderPath = function(path) {
  var text = '';
  for (var i = 1; i < path.length; i++) {
    if (i != 1) {
      text += ' / ';
    }
    text += path[i].name;
  }
  return text;
};

Hippo.Translation.ImageService = function(imageServiceUrl) {
   this.service = imageServiceUrl;
};

Hippo.Translation.ImageService.prototype = {
  getImage: function(lang) {
    return this.service + "&lang=" + lang;
  },
};

Hippo.Translation.SiblingLocator = Ext.extend(Ext.util.Observable, {

  constructor: function(config) {
    this.dataUrl = config.dataUrl;
    Hippo.Translation.SiblingLocator.superclass.constructor.call(this, config);
  },
  
  getSiblings: function(t9Id, callback) {
    Ext.Ajax.request({
      url: this.dataUrl,
      params: {
        t9id: t9Id
      },
      success: function(response) {
        var children = Ext.util.JSON.decode(response.responseText);
        callback.call(this, children);
      }
    });
  }

});

