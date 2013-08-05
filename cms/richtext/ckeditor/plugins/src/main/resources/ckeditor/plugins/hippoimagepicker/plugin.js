/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

CKEDITOR.plugins.add( 'hippoimagepicker', {

    icons: 'pickimage',
    hidpi: true,

    init: function(editor) {
        var callbackUrl = editor.config.hippoimagepicker.callbackUrl,
            imgAttributeParameterMap = {
                src: 'f_url',
                type: 'f_type',
                facetselect: 'f_facetselect',
                alt: 'f_alt',
                align: 'f_align',
                width: 'f_width',
                height: 'f_height'
            }

        function iterate(object, func) {
            for (property in object) {
                if (object.hasOwnProperty(property)) {
                    func(property, object[property]);
                }
            }
        }

        function setImgAttributes(imgElement, parameters) {
            iterate(imgAttributeParameterMap, function(imgAttribute, parameterName) {
                var parameterValue = parameters[parameterName];
                if (parameterValue !== undefined && parameterValue !== null && parameterValue !== "") {
                    imgElement.setAttribute(imgAttribute, parameterValue);
                }
            });
        }

        function getImgParameters(imgElement) {
            var parameters = {};
            iterate(imgAttributeParameterMap, function(imgAttribute, parameterName) {
                if (imgElement.hasAttribute(imgAttribute)) {
                    parameters[parameterName] = imgElement.getAttribute(imgAttribute);
                }
            });
            return parameters;
        }

        editor.addCommand('pickImage', {
            exec: function(editor) {
                var selectedElement = editor.getSelection().getStartElement(),
                    callbackParameters = {};

                if (selectedElement.getName() === 'img') {
                    callbackParameters = getImgParameters(selectedElement);
                }

                Wicket.Ajax.post({
                    u: callbackUrl,
                    ep: callbackParameters
                });
            }
        });

        editor.addCommand('insertImage', {
            exec: function(editor, parameters) {
                var img = editor.document.createElement('img');
                setImgAttributes(img, parameters);
                editor.insertElement(img);
            }
        });

        editor.ui.addButton('PickImage', {
            label: 'Pick an image',
            command: 'pickImage',
            toolbar: 'insert',
            allowedContent: 'img[!src,alt,align,width,height,facetselect,type]',
            requiredContent: 'img[!src]'
        });

        // Ensure compatibility with the 'Maximize' plugin. That plugin breaks the styling of Hippo's modal Wicket
        // dialogs because it removes all CSS classes (including 'hipo-root') from the document body when the
        // editor is maximized. Here we explicitly re-add the 'hippo-root' CSS class when the editor is maximized
        // so the image picker dialog still looks good.
        editor.on("afterCommandExec", function(event) {
            if (event.data.name === 'maximize') {
                if (event.data.command.state == CKEDITOR.TRISTATE_ON) {
                    CKEDITOR.document.getBody().addClass('hippo-root');
                }
            }
        });
    }

});
