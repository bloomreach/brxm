/*
 * Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

if (Hippo === undefined) {
    Hippo = {};
}

Hippo.Translation = {};

/*
 * render path from a list of T9Node ancestors
 */
Hippo.Translation.PathRenderer = Ext.extend(Ext.util.Observable, {

    constructor: function(config) {
        this.locales = config.locales;
        this.resources = config.resources;
        Hippo.Translation.PathRenderer.superclass.constructor.call(config);
    },

    renderPath: function(path) {
        var text, locale, i, len, countryClass, candidate;

        text = '<div style="float: left;">';
        locale = null;

        for (i = 1, len = path.length; i < len; i++) {
            if (i !== 1) {
                text += ' / ';
            }

            countryClass = 'hippo-translation-country';
            if ((locale === null || locale === undefined) && path[i].lang !== undefined) {
                candidate = this.locales[path[i].lang];
                if (candidate !== null && candidate !== undefined) {
                    locale = candidate;
                    countryClass = 'hippo-translation-country-' + locale.country;
                }
            }

            text += '<span class="x-tree-node-expanded">'
                    + '<img src="data:image/gif;base64,R0lGODlhAQABAID/AMDAwAAAACH5BAEAAAAALAAAAAABAAEAAAICRAEAOw==" '
                    + 'style="width: 16px; height: 16px; background-repeat: no-repeat;" '
                    + 'class="' + countryClass + '" />'
                    + '</span> ';
            text += path[i].name;
        }
        text += '</div>';
        if (locale !== null && locale !== undefined) {
            text += '<div style="float: left;" class="hippo-translation-language">'
                    + this.resources.language + ': ' + locale.name
                    + '</div>';
        }
        return text;
    }
});

Hippo.Translation.ImageService = function(imageServiceUrl) {
    this.service = imageServiceUrl;
};

Hippo.Translation.ImageService.prototype = {
    getImage: function(lang) {
        return this.service + "&lang=" + lang;
    }
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

