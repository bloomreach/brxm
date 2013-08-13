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
(function() {
    "use strict";

    function iterate(object, func) {
        var property;
        for (property in object) {
            if (object.hasOwnProperty(property)) {
                func(property, object[property]);
            }
        }
    }

    function isEmpty(value) {
        return value === undefined || value === null || value === "";
    }

    function setElementAttributes(element, attributeParameterMap, parameters) {
        iterate(attributeParameterMap, function(attribute, parameterName) {
            var parameterValue = parameters[parameterName];
            if (isEmpty(parameterValue)) {
                element.removeAttribute(attribute);
            } else {
                element.setAttribute(attribute, parameterValue);
            }
        });
    }

    function getElementParameters(element, attributeParameterMap) {
        var parameters = {};
        if (element !== null) {
            iterate(attributeParameterMap, function(attribute, parameterName) {
                if (element.hasAttribute(attribute)) {
                    parameters[parameterName] = element.getAttribute(attribute);
                }
            });
        }
        return parameters;
    }

    function initInternalLinkPicker(editor, callbackUrl) {

        var LINK_ATTRIBUTES_PARAMETER_MAP = {
            href: 'f_href',
            title: 'f_title',
            target: 'f_target'
        };

        function createLinkAttributePairs(parameters) {
            var pairs = {};
            iterate(LINK_ATTRIBUTES_PARAMETER_MAP, function(attribute, parameterName) {
                var parameterValue = parameters[parameterName];
                if (parameterValue !== undefined && parameterValue !== null && parameterValue !== "") {
                    pairs[attribute] = parameterValue;
                }
            });
            return pairs;
        }

        function isSelectionEmpty(selection) {
            if (selection.getType() === CKEDITOR.SELECTION_NONE) {
                return true;
            }
            var ranges = selection.getRanges();
            return ranges.length === 0 || ranges[0].collapsed;
        }

        function getSelectedLinkOrNull(selection) {
            var linkNode = selection.getStartElement().getAscendant('a', true);
            if (linkNode !== null && linkNode.is('a')) {
                return linkNode;
            }
            return null;
        }

        function createLinkFromSelection(selection, linkParameters) {
            var range, linkAttributes, linkStyle;

            if (!isSelectionEmpty(selection)) {
                range = selection.getRanges()[0];

                linkAttributes = createLinkAttributePairs(linkParameters);
                linkStyle = new CKEDITOR.style({
                    element: 'a',
                    attributes: linkAttributes,
                    type: CKEDITOR.STYLE_INLINE
                });
                linkStyle.applyToRange(range);
                range.select();
            }
        }

        function updateToolbarButtonState() {
            var state = CKEDITOR.TRISTATE_OFF,
                selection = editor.getSelection();

            if (isSelectionEmpty(selection) && getSelectedLinkOrNull(selection) === null) {
                state = CKEDITOR.TRISTATE_DISABLED;
            }
            editor.getCommand('pickInternalLink').setState(state);
        }

        editor.ui.addButton('PickInternalLink', {
            label: 'Internal link',
            command: 'pickInternalLink',
            toolbar: 'insert',
            allowedContent: 'a[!href,title,target]',
            requiredContent: 'a[!href]'
        });

        editor.addCommand('pickInternalLink', {

            startDisabled: true,

            exec: function(editor) {
                var selectedLink = getSelectedLinkOrNull(editor.getSelection()),
                    callbackParameters = getElementParameters(selectedLink, LINK_ATTRIBUTES_PARAMETER_MAP);

                Wicket.Ajax.post({
                    u: callbackUrl,
                    ep: callbackParameters
                });
            }
        });

        editor.addCommand('insertInternalLink', {
            exec: function(editor, parameters) {
                var selectedLink = getSelectedLinkOrNull(editor.getSelection());

                if (selectedLink !== null) {
                    setElementAttributes(selectedLink, LINK_ATTRIBUTES_PARAMETER_MAP, parameters);

                    // ensure compatibility with the 'link' plugin, which creates an additional attribute
                    // 'data-cke-saved-href' for each link that overrides the actual href value. We don't need
                    // this attribute, so remove it.
                    selectedLink.removeAttribute('data-cke-saved-href');
                } else {
                    createLinkFromSelection(editor.getSelection(), parameters);
                }
            }
        });

        // update the toolbar button state whenever the selection changes (copied from the 'clipboard' plugin)
        editor.on('contentDom', function() {
            var editable = editor.editable(),
                mouseupTimeout;

            editable.attachListener( CKEDITOR.env.ie ? editable : editor.document.getDocumentElement(), 'mouseup', function() {
                mouseupTimeout = setTimeout(updateToolbarButtonState, 0);
            });
            editor.on( 'destroy', function() {
                clearTimeout(mouseupTimeout);
            });
            editable.on('keyup', updateToolbarButtonState);
        });
    }

    function initImagePicker(editor, callbackUrl) {
        var IMAGE_ATTRIBUTE_PARAMETER_MAP = {
            src: 'f_url',
            type: 'f_type',
            facetselect: 'f_facetselect',
            alt: 'f_alt',
            align: 'f_align',
            width: 'f_width',
            height: 'f_height'
        };

        editor.ui.addButton('PickImage', {
            label: 'Pick an image',
            command: 'pickImage',
            toolbar: 'insert',
            allowedContent: 'img[!src,alt,align,width,height,facetselect,type]',
            requiredContent: 'img[!src]'
        });

        editor.addCommand('pickImage', {
            exec: function(editor) {
                var selectedImage = editor.getSelection().getStartElement(),
                    callbackParameters = {};

                if (selectedImage.getName() === 'img') {
                    callbackParameters = getElementParameters(selectedImage, IMAGE_ATTRIBUTE_PARAMETER_MAP);
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
                setElementAttributes(img, IMAGE_ATTRIBUTE_PARAMETER_MAP, parameters);
                editor.insertElement(img);
            }
        });
    }

    CKEDITOR.plugins.add('hippopicker', {

        icons: 'pickinternallink,pickimage',
        hidpi: true,

        init: function(editor) {
            var config = editor.config.hippopicker;

            initInternalLinkPicker(editor, config.internalLink.callbackUrl);
            initImagePicker(editor, config.image.callbackUrl);

            // Ensure compatibility with the 'Maximize' plugin. That plugin breaks the styling of Hippo's modal Wicket
            // dialogs because it removes all CSS classes (including 'hipo-root') from the document body when the
            // editor is maximized. Here we explicitly re-add the 'hippo-root' CSS class when the editor is maximized
            // so the image picker dialog still looks good.
            editor.on("afterCommandExec", function(event) {
                if (event.data.name === 'maximize') {
                    if (event.data.command.state === CKEDITOR.TRISTATE_ON) {
                        CKEDITOR.document.getBody().addClass('hippo-root');
                    }
                }
            });
        }

    });
}());
