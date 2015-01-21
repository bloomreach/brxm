/*
 *  Copyright 2010-2014 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
(function($) {
    "use strict";

    var hostToIFrame, iframeToHost, EditContentVisitor, EditMenuVisitor, CommentsProcessor, surfAndEdit;

    hostToIFrame = window.parent.Ext.getCmp('pageEditorIFrame').hostToIFrame;
    iframeToHost = window.parent.Ext.getCmp('pageEditorIFrame').iframeToHost;

    $(window).unload(function() {
        hostToIFrame = null;
        iframeToHost = null;
    });

    function replaceCommentWithEditButton(commentElement, btnClass, btnText, publishedEvent, publishedUuid, lockInfoTitle, lockInfoMessage) {
        var publish, link = document.createElement('a');
        if (commentElement.nextSibling) {
            commentElement.parentNode.insertBefore(link, commentElement.nextSibling);
        } else {
            commentElement.parentNode.appendChild(link);
        }


        link.setAttribute(HST.ATTR.ID, publishedUuid);
        link.setAttribute('href', '');
        link.setAttribute('class', btnClass);

        if (publishedEvent === 'edit-menu' && lockInfoTitle) {
            $(link).append('<span class="' + btnClass + '-locked" title="' + lockInfoTitle + '">' +
                                '<span class="left"></span>' +
                                '<span class="center">' + lockInfoMessage + '</span>' +
                                '<span class="right"></span>' +
                            '</span>');

            publish = function() {};
        } else {
            $(link).append('<span class="' + btnClass + '-left"><span class="' + btnClass + '-right"><span class="' + btnClass + '-center">' + btnText + '</span></span></span>');
            publish = function() {
                iframeToHost.publish(publishedEvent, publishedUuid);
            };
        }
        /**
         * use plain old javascript event listener to prevent other jQuery instances hijacking the event.
         */
        if (link.addEventListener) {
            link.addEventListener('click', function(event) {
                publish();
                event.stopPropagation();
                event.preventDefault();
                return false;
            }, false);
        } else if (link.attachEvent) {
            link.attachEvent('onclick', function(event) {
                publish();
                event.cancelBubble = true;
                return false;
            });
        }

        commentElement.parentNode.removeChild(commentElement);
    }

    EditContentVisitor = function(resources) {

        var documentUuids = [];

        function isEditContentData(dataString) {
            return dataString !== null
                && dataString.indexOf(HST.ATTR.TYPE) !== -1
                && dataString.indexOf(HST.ATTR.ID) !== -1
                && dataString.indexOf(HST.ATTR.URL) !== -1;
        }

        function readEditContentData(commentData) {
            if (isEditContentData(commentData)) {
                try {
                    var commentJson = JSON.parse(commentData);
                    if (commentJson !== null
                        && commentJson[HST.ATTR.TYPE] === HST.CMSLINK
                        && commentJson[HST.ATTR.ID] !== undefined
                        && commentJson[HST.ATTR.URL] !== undefined) {
                        return commentJson;
                    }
                } catch (e) {
                    console.warn("Skipping unparsable 'edit content' meta data from comment '" + commentData + "'", e);
                }
            }
            return null;
        }

        return {

            visit: function(commentElement, commentData) {
                var hstMetaData = readEditContentData(commentData),
                    documentUuid;
                if (hstMetaData !== null) {
                    documentUuid = hstMetaData[HST.ATTR.ID];
                    documentUuids.push(documentUuid);
                    replaceCommentWithEditButton(commentElement, HST.CLASS.EDITLINK, resources['edit-document'], 'edit-document', documentUuid);
                }
            },

            finalize: function() {
                iframeToHost.publish('documents', documentUuids);
            }

        };

    };

    EditMenuVisitor = function(resources) {

        function isEditMenuData(dataString) {
            return dataString !== null
                && dataString.indexOf(HST.ATTR.TYPE) !== -1
                && dataString.indexOf(HST.ATTR.ID) !== -1;
        }

        function readEditMenuData(commentData) {
            if (isEditMenuData(commentData)) {
                try {
                    var commentJson = JSON.parse(commentData);
                    if (commentJson !== null
                        && commentJson[HST.ATTR.TYPE] === HST.MENU
                        && commentJson[HST.ATTR.ID] !== undefined) {
                        return commentJson;
                    }
                } catch (e) {
                    console.warn("Skipping unparsable 'edit menu' meta data from comment '" + commentData + "'", e);
                }
            }
            return null;
        }

        return {

            visit: function(commentElement, commentData) {
                var hstMetaData = readEditMenuData(commentData),
                        menuUuid, lockedBy, lockedByCurrentUser, lockedOn, lockedOnDate, lockedOnDateString, lockInfoTitle;
                if (hstMetaData !== null) {
                    menuUuid = hstMetaData[HST.ATTR.ID];
                    lockedBy = hstMetaData[HST.ATTR.HST_LOCKED_BY];
                    lockedByCurrentUser = hstMetaData[HST.ATTR.HST_LOCKED_BY_CURRENT_USER];
                    lockedOn = hstMetaData[HST.ATTR.HST_LOCKED_ON];
                    
                    if (lockedBy && lockedByCurrentUser === 'false') {
                        lockInfoTitle = resources['menu-locked-by'].format(lockedBy);
                        if (lockedOn) {
                            lockedOnDate = new Date(parseInt(lockedOn, 10));
                            if (lockedOnDate) {
                                lockedOnDateString = lockedOnDate.toLocaleString();
                                lockInfoTitle = resources['menu-locked-by-on'].format(lockedBy, lockedOnDateString);
                            }
                        }
                    }
                    
                    replaceCommentWithEditButton(commentElement, HST.CLASS.EDITMENU, resources['edit-menu'], 'edit-menu', menuUuid, lockInfoTitle, resources['menu-locked']);
                }
            }

        };

    };

    CommentsProcessor = function(visitors) {

        function getCommentData(element) {
            if (element.length < 0) {
                // Skip conditional comments in IE: reading their 'data' property throws an
                // Error "Not enough storage space is available to complete this operation."
                // Conditional comments can be recognized by a negative 'length' property.
                return null;
            }
            if (!element.data || element.data.length === 0) {
                // no data available
                return null;
            }
            return element.data;
        }

        function processComment(commentElement) {
            var commentData, i, length;

            commentData = getCommentData(commentElement);

            if (commentData !== null) {
                for (i = 0, length = visitors.length; i < length; i++) {
                    visitors[i].visit(commentElement, commentData);
                }
            }
        }

        function processCommentsWithXPath() {
            var query = document.evaluate("//comment()", document, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null),
                i, length;
            for (i = 0, length = query.snapshotLength; i < length; i++) {
                processComment(query.snapshotItem(i));
            }
        }

        function isComment(element) {
            return element.nodeType === 8;
        }

        function processCommentsWithDomWalking(node) {
            if (!node || node.nodeType === undefined) {
                return;
            }
            if (isComment(node)) {
                processComment(node);
                return;
            }
            var i, length;
            for (i = 0, length = node.childNodes.length; i < length; i++) {
                processCommentsWithDomWalking(node.childNodes[i]);
            }
        }

        function processComments() {
            if (!!document.evaluate) {
                processCommentsWithXPath();
            } else {
                processCommentsWithDomWalking(document.body);
            }
        }

        function finalizeProcessors() {
            var i, length;
            for (i = 0, length = visitors.length; i < length; i++) {
                if (typeof visitors[i].finalize === 'function') {
                    visitors[i].finalize();
                }
            }
        }

        return {

            run: function() {
                try {
                    processComments();
                    finalizeProcessors();
                } catch(e) {
                    iframeToHost.exception('Error processing HTML comments', e);
                }
            }

        };

    };

    surfAndEdit = {

        init : function (data) {
            new CommentsProcessor([
                new EditContentVisitor(data.resources),
                new EditMenuVisitor(data.resources)
            ]).run();
        },

        showEditContentButtons: function() {
            $('.' + HST.CLASS.EDITLINK).show();
        },

        hideEditContentButtons: function() {
            $('.' + HST.CLASS.EDITLINK).hide();
        },

        showEditMenuButtons: function() {
            $('.' + HST.CLASS.EDITMENU).show();
        },

        hideEditMenuButtons: function() {
            $('.' + HST.CLASS.EDITMENU).hide();
        }

    };

    hostToIFrame.subscribe('init', surfAndEdit.init, surfAndEdit);
    hostToIFrame.subscribe('hide-edit-content-buttons', surfAndEdit.hideEditContentButtons, surfAndEdit);
    hostToIFrame.subscribe('show-edit-content-buttons', surfAndEdit.showEditContentButtons, surfAndEdit);
    hostToIFrame.subscribe('hide-edit-menu-buttons', surfAndEdit.hideEditMenuButtons, surfAndEdit);
    hostToIFrame.subscribe('show-edit-menu-buttons', surfAndEdit.showEditMenuButtons, surfAndEdit);

}(jQuery));
