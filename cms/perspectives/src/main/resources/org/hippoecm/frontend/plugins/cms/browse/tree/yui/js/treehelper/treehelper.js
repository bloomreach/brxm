/*
 * Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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

/**
 * @class a simple helper class to calculate tree width
 * @requires yahoo, dom, event, layoutmanager, hippodom
 * @constructor
 * @param {String} id the id of the linked element
 * @param {String} config
 */

YAHOO.namespace('hippo');

if (!YAHOO.hippo.TreeHelper) {
    (function($) {
        'use strict';

        var Dom = YAHOO.util.Dom,
            Lang = YAHOO.lang;

        function byClass(name, tag, root) {
            var found = Dom.getElementsByClassName(name, tag, root);
            if (!Lang.isUndefined(found.length) && found.length > 0) {
                return found[0];
            }
            return null;
        }

        function exists(o) {
            return !Lang.isUndefined(o) && !Lang.isNull(o);
        }

        YAHOO.hippo.TreeHelperImpl = function() {
        };

        YAHOO.hippo.TreeHelperImpl.prototype = {
            cfg : null,
            timeoutID: null,
            timeoutLength: 200,

            init : function(id, cfg) {
                var tree, sectionCenter;

                tree = Dom.get(id);
                if (exists(tree)) {
                    tree.treeHelper = {
                        cfg: cfg,
                        registered: false,
                        layoutUnit: null,
                        wicketTree: null,
                        highlight: null,
                        overId: null
                    };

                    if (cfg.workflowEnabled) {
                        // Notify the context menu the tree is scrolling so it can reposition the context menu
                        sectionCenter = Dom.getAncestorByClassName(tree, 'section-center');
                        if (exists(sectionCenter)) {
                            YAHOO.util.Event.on(sectionCenter, 'scroll', function () {
                                Hippo.ContextMenu.redraw();
                            });
                        }
                    }
                }
            },

            register : function(id, tree, helper) {
                var self;

                if (helper.cfg.bindToLayoutUnit) {
                    self = this;
                    YAHOO.hippo.LayoutManager.registerResizeListener(tree, self, function() {
                        self.render(id);
                    });
                }
                helper.wicketTree = byClass('wicket-tree', 'div', tree);

                YAHOO.hippo.HippoAjax.registerDestroyFunction(tree, this.cleanup, this, id);
            },

            render : function(id) {
                var tree, helper;

                tree = Dom.get(id);
                if (!(exists(tree) && exists(tree.treeHelper))) {
                    return;
                }
                helper = tree.treeHelper;

                if (!helper.registered) {
                    this.register(id, tree, helper);
                    helper.registered = true;
                }

                // Browser other than Edge can use:
                //    width: intrinsic;           /* Safari/WebKit uses a non-standard name */
                //    width: -moz-max-content;    /* Firefox/Gecko */
                //    width: -webkit-max-content; /* Chrome */
                // which causes the child elements to define the width of the element, Edge still needs help from javascript
                if (navigator.userAgent.indexOf('Edge') >= 0){
                    this.setTreeWidth(id, tree, helper);
                }

                if (helper.cfg.workflowEnabled) {
                    this.updateMouseListeners(id, tree, helper);
                }
                this.updateHighlight(id, tree, helper);
                this.updateContextIcon(id, tree, helper);
            },

            updateHighlight : function(id, tree, helper) {
                var selected, selectedY, highlight;

                highlight = helper.highlight = helper.highlight === null ? this.createHighlight(tree) : helper.highlight;

                if (highlight === null) {
                    return;
                }

                selected = Dom.getFirstChildBy(helper.wicketTree, function(node) {
                    return Dom.hasClass(node, 'row-selected');
                });

                if (selected === null) {
                    if (!Dom.hasClass(highlight, 'hippo-global-hideme')) {
                        Dom.addClass(highlight, 'hippo-global-hideme');
                    }
                } else {
                    if (Dom.hasClass(highlight, 'hippo-global-hideme')) {
                        Dom.removeClass(highlight, 'hippo-global-hideme');
                    }

                    selectedY = Dom.getY(selected);
                    if (selectedY !== Dom.getY(highlight)) {
                        // Move highlight widget to position of selected
                        Dom.setY(highlight, selectedY);
                    }
                }
            },

            createHighlight: function(tree) {
                var hippoTree = byClass('hippo-tree', 'div', tree),
                    el = null;

                if (hippoTree !== null) {
                    el = document.createElement('div');
                    hippoTree.appendChild(el);
                    Dom.addClass(el, 'hippo-tree-highlight-widget');
                }
                return el;
            },

            updateContextIcon: function(id, tree, helper) {
                if (helper.overId === null) {
                    return;
                }

                var el = $('#' + helper.overId);
                if (!el.hasClass('register-mouse')) {
                    // dropdown icon was visible when it was clicked, re-draw it
                    el.mouseleave().mouseenter();
                }
            },

            updateMouseListeners : function(id, tree, helper) {
                function mouseEnter() {
                    Hippo.ContextMenu.showContextLink(this.id);
                    helper.overId = this.id;
                }

                function mouseLeave() {
                    Hippo.ContextMenu.hideContextLink(this.id);
                    helper.overId = null;
                }

                $('.register-mouse', tree).each(function(index, el) {
                    var parent = el.parentNode;
                    $(el).removeClass('register-mouse');
                    $(parent).mouseenter(mouseEnter).mouseleave(mouseLeave);
                });
            },

            setTreeWidth : function(id, el, helper) {
                var width, computedWidth, items, i, iLen, item, itemChildNodes, j, jLen, childNode;

                width = 0;
                if (helper.cfg.treeAutowidth) {
                    computedWidth = 0;
                    items = Dom.getElementsByClassName('a_', 'div', id);

                    //Calculate width&height of items and save largest computedWidth value in var 'width'
                    for (i = 0, iLen = items.length; i < iLen; i++) {
                        item = items[i];
                        itemChildNodes = Dom.getChildren(item);
                        for (j = 0, jLen = itemChildNodes.length; j < jLen; j++) {
                            childNode = itemChildNodes[j];
                            computedWidth += Dom.getRegion(childNode).width;
                        }
                        if (computedWidth > width) {
                            width = computedWidth;
                        }
                        computedWidth = 0;
                    }

                    //Add magic width
                    if (helper.cfg.workflowEnabled) {
                        width += 25;
                    } else {
                        width += 16;
                    }
                }

                if (width > 0) {
                    this._setWidth(id, el, helper, width);
                }
            },

            _setWidth : function(id, el, helper, width) {
                var layoutMax, target;

                // Set width to child element with classname configured in property helper.cfg.setWidthToClassname
                target = byClass(helper.cfg.setWidthToClassname, 'div', id);
                if (target !== null) {
                    if (exists(helper.layoutUnit)) {
                        // Ensure width is at least the same as the first ancestorial layout unit
                        layoutMax = this.getLayoutMax(id, el, helper);
                        if (exists(layoutMax) && layoutMax > width) {
                            width = layoutMax;
                        }
                    }

                    Dom.setStyle(target, 'width', width + 'px');
                }
            },

            update: function(id) {
                this.render(id);
            },

            getLayoutMax: function(id, el, helper) {
                var target;
                if (helper.cfg.bindToLayoutUnit && exists(helper.layoutUnit)) {
                    return helper.layoutUnit.getSizes().body.w;
                } else if (exists(helper.cfg.useWidthFromClassname)) {
                    target = Dom.getAncestorByClassName(el, helper.cfg.useWidthFromClassname);
                    return parseInt(Dom.getStyle(target, 'width'), 10);
                }
                return null;
            },

            cleanup: function(id) {
                var tree = Dom.get(id);
                YAHOO.hippo.LayoutManager.unregisterResizeListener(tree, this);
            }
        };

    }(jQuery));

    YAHOO.hippo.TreeHelper = new YAHOO.hippo.TreeHelperImpl();
    YAHOO.register('TreeHelper', YAHOO.hippo.TreeHelper, {
        version: '2.9.0', build: '1'
    });
}
