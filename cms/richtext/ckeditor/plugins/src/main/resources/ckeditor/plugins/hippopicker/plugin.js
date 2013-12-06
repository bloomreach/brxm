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

    function disableRegisteredCKEditorDialog(ckeditorEvent) {
        ckeditorEvent.data.dialog = null;
    }

    function initInternalLinkPicker(editor, callbackUrl) {

        var LINK_ATTRIBUTES_PARAMETER_MAP = {
                'data-uuid': 'f_uuid',
                title: 'f_title',
                target: 'f_target'
            },
            LANG = editor.lang.hippopicker;

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

        function isInternalLink(element) {
            return !element.isReadOnly() && element.is('a') && element.hasAttribute('data-uuid');
        }

        function isSelectionEmpty(selection) {
            if (selection === null || selection.getType() === CKEDITOR.SELECTION_NONE) {
                return true;
            }
            var ranges = selection.getRanges();
            return ranges.length === 0 || ranges[0].collapsed;
        }

        function getSelectedLinkOrNull(selection) {
            var startElement, linkNode;

            if (selection === null) {
                return null;
            }

            startElement = selection.getStartElement();

            if (startElement !== null) {
                linkNode = startElement.getAscendant('a', true);
                if (linkNode !== null && linkNode.is('a')) {
                    return linkNode;
                }
            }
            return null;
        }

        function openInternalLinkPickerDialog(selectedLink) {
            var callbackParameters = getElementParameters(selectedLink, LINK_ATTRIBUTES_PARAMETER_MAP);

            Wicket.Ajax.post({
                u: callbackUrl,
                ep: callbackParameters
            });
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
            label: LANG.internalLinkTooltip,
            command: 'pickInternalLink',
            toolbar: 'links,5',
            allowedContent: 'a[!data-uuid,!href,title,target]',
            requiredContent: 'a[!data-uuid,!href]'
        });

        editor.addCommand('pickInternalLink', {

            startDisabled: true,

            exec: function(editor) {
                var selectedLink = getSelectedLinkOrNull(editor.getSelection());
                openInternalLinkPickerDialog(selectedLink);
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
                    selectedLink = getSelectedLinkOrNull(editor.getSelection());
                }

                if (selectedLink !== null) {
                    if (selectedLink.hasAttribute('data-uuid')) {
                        // Set the href attribute to 'http://' so the CKEditor link picker will still recognize the link
                        // as a link (and, for example, enable the 'remove link' button). The CMS will recognize the empty
                        // 'http://' href and still interpret the link as an internal link.
                        selectedLink.setAttribute('href', 'http://');
                    } else {
                        // the link has been removed in the picker dialog
                        selectedLink.remove(true);
                    }
                }
            }
        });

        editor.on('doubleclick', function(event) {
            var clickedLink = getSelectedLinkOrNull(editor.getSelection()) || event.data.element;

            if (isInternalLink(clickedLink)) {
                disableRegisteredCKEditorDialog(event);
                openInternalLinkPickerDialog(clickedLink);
            }
        }, null, null, 20);  // use a higher priority than 10 to overwrite the external link dialog

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
                'data-facetselect': 'f_facetselect',
                'data-type': 'f_type',
                'data-uuid': 'f_uuid',
                src: 'f_url',
                alt: 'f_alt',
                align: 'f_align',
                width: 'f_width',
                height: 'f_height'
            },
            LANG = editor.lang.hippopicker;

        function getSelectedImage() {
            return editor.getSelection().getStartElement();
        }

        function isInternalImage(element) {
            return !element.isReadOnly()
                    && element.is('img')
                    && element.hasAttribute('data-facetselect')
                    && element.hasAttribute('data-type')
                    && element.hasAttribute('data-uuid');
        }

        function openImagePickerDialog(element) {
            var callbackParameters = {};

            if (element.getName() === 'img') {
                callbackParameters = getElementParameters(element, IMAGE_ATTRIBUTE_PARAMETER_MAP);
            }

            Wicket.Ajax.post({
                u: callbackUrl,
                ep: callbackParameters
            });
        }

        editor.ui.addButton('PickImage', {
            label: LANG.imageTooltip,
            command: 'pickImage',
            toolbar: 'insert,5',
            allowedContent: 'img[!data-facetselect,!data-type,!data-uuid,!src,alt,align,width,height]',
            requiredContent: 'img[!data-facetselect,!data-type,!data-uuid,!src]'
        });

        editor.addCommand('pickImage', {
            exec: function(editor) {
                openImagePickerDialog(getSelectedImage());
            }
        });

        editor.addCommand('insertImage', {
            exec: function(editor, parameters) {
                var img = editor.document.createElement('img');
                setElementAttributes(img, IMAGE_ATTRIBUTE_PARAMETER_MAP, parameters);
                if (img.hasAttributes()) {
                    editor.insertElement(img);
                } else {
                    getSelectedImage().remove();
                }
            }
        });

        editor.on('doubleclick', function(event) {
            var clickedElement = event.data.element;

            if (isInternalImage(clickedElement)) {
                disableRegisteredCKEditorDialog(event);
                openImagePickerDialog(clickedElement);
            }
        }, null, null, 20); // use a higher priority than 10 to overwrite the external image dialog

    }

    CKEDITOR.plugins.add('hippopicker', {

        icons: 'pickinternallink,pickimage',
        hidpi: true,
        lang: CKEDITOR.tools.objectKeys(CKEDITOR.lang.languages),

        init: function(editor) {
            var config = editor.config.hippopicker;

            initInternalLinkPicker(editor, config.internalLink.callbackUrl);
            initImagePicker(editor, config.image.callbackUrl);

            // Ensure compatibility with the 'Maximize' plugin. That plugin breaks the styling of Hippo's modal Wicket
            // dialogs because it removes all CSS classes (including 'hippo-root') from the document body when the
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
