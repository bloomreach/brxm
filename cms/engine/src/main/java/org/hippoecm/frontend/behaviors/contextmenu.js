/*
 * Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

(function() {

	if (typeof Hippo == 'undefined') {
		Hippo = {};
	}
	
	Hippo.Set = function() {
		this.entries = [];
	}
	
	Hippo.Set.prototype = {
		add : function (entry) {
			this.entries.push(entry);
		},
	
		remove : function (entry) {
		    if(this.entries.length == 0) {
		        return null;
		    }
		    
			var index = this._getIndex(entry);
			if (index >= 0) {
			    var part1 = this.entries.slice(0, index);
			    var part2 = this.entries.slice(index + 1);
	
			    this.entries = part1.concat(part2);
			    return index;
			}
			return null;
		},

		contains : function (entry) {
			return this._getIndex(entry) >= 0;
		},
		
		_getIndex : function (entry) {
	        for (var i = 0; i < this.entries.length; i++) {
	            if (this.entries[ i ] == entry) {
	                return i;
	            }
	        }
	        return -1;
	    }
	}

	var menus = new Hippo.Set();

	Hippo.ContextMenu = new Object();
	
	Hippo.ContextMenu.init = function() {
	    if(document.getElementById('context-menu-container') == null) {
	        var x = document.createElement('div');
	        x.id = "context-menu-container";
	        document.body.appendChild(x);
	    }
	}

	Hippo.ContextMenu.show = function(id) {
	    menus.add(id);
	}

	Hippo.ContextMenu.hide = function(id) {
        menus.remove(id);
        var YUID = YAHOO.util.Dom;
        var el = YUID.get('context-menu-container');
	    el.innerHTML = '';
	    YUID.setXY(el, [-100, -100]);
	}

	Hippo.ContextMenu.isShown = function(id) {
		return menus.contains(id);
	}
	
	Hippo.ContextMenu.renderInTree = function(id) {
	    var xy = this.getContextPosition(id);
	    this.renderAtPosition(id, xy[0] + 12, xy[1] + 5);
	}
	
	Hippo.ContextMenu.renderAtPosition = function(id, posX, posY) {
        var YUID = YAHOO.util.Dom;
        var container = YUID.get('context-menu-container');

        //reset container
        container.innerHTML = '';
        
        var menuHeight = 120; //middle ground fallback
        var uls = YUID.getElementsByClassName('hippo-toolbar-menu-item', 'ul', YUID.get(id));
        if(uls.length > 0) {
            var r = YUID.getRegion(uls[0]);
            menuHeight = r.height + 5;
        }
        
        var viewHeight = YUID.getViewportHeight();
        if(posY + menuHeight > viewHeight) {
            posY -= (menuHeight - 10);
        }
        container.appendChild(YUID.get(id));
        YUID.setXY(container, [posX,posY]);
        YUID.setStyle(id, 'visibility', 'visible');
    }
    
    Hippo.ContextMenu.currentContentLink = null;
    Hippo.ContextMenu.isShowing = false;
    
    Hippo.ContextMenu.showContextLink = function(id) {
        var YUID = YAHOO.util.Dom, YUIL = YAHOO.lang;
        if(this.isShowing) {
            return;
        }
        
        var _ar = YUID.getElementsByClassName('hippo-tree-dropdown-icon-container', 'span', id);
        if(YUIL.isArray(_ar) && _ar.length > 0) {
            var el = _ar[0];
            YUID.addClass(el, 'container-selected');

            if(!YUID._canPosition(el)) {
                return;
            }
            var pos = this.getContextPosition(id);
            YUID.setXY(el, pos);
            this.currentContentLink = el;
        }
        this.isShowing = true;
    }
    
    Hippo.ContextMenu.getContextPosition = function(id) {
        var YUID = YAHOO.util.Dom;
        var el = YUID.get(id);
        
        var unit  = this.getLayoutUnit(el); 
        if(unit != null) {
            var layoutRegion = YUID.getRegion(unit.get('element'));
            var myY = YUID.getRegion(el).top +2;
            var myX = layoutRegion.right - 20;
            if(YAHOO.env.ua.ie > 0 && YAHOO.env.ua.ie < 8) {
                //IE needs more whitespace @ the right of this widget
                //because else it will interfere with the resize handler
                myX -= 10; 
                
            }
            
            var layout = YUID.getAncestorByClassName(el, 'hippo-accordion-unit-center');
            var layoutDim = YUID.getRegion(layout);
            var treeDim = YUID.getRegion(YUID.getAncestorByClassName(el, 'hippo-tree'));
            if (treeDim.height > layoutDim.height) {
                myX -= 15;
            }
            return [myX, myY];
        }
    }
    
    Hippo.ContextMenu.hideContextLink = function(id) {
        var YUID = YAHOO.util.Dom;
        var el = this.currentContentLink;
        if(el == null) {
            var _ar = YUID.getElementsByClassName('hippo-tree-dropdown-icon-container', 'span', id);
            if(typeof(_ar.length) == 'undefined' && _ar.length > 0) {
              el = _ar[0];
            }
        }
        if(el != null) {
            YUID.removeClass(el, 'container-selected');
        }
        this.isShowing = false;
    }
    
    Hippo.ContextMenu.getLayoutUnit = function(el) {
        return YAHOO.hippo.LayoutManager.findLayoutUnit(el);
    }

})();
