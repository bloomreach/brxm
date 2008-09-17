/*
 * Copyright 2008 Hippo
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
 * @class a YAHOO.util.DDProxy extension
 * @requires dragdrop
 * @extends YAHOO.util.DDProxy
 * @constructor
 * @param {String} id the id of the linked element
 * @param {String} sGroup the group of related DragDrop objects
 */
YAHOO.namespace("hippo"); 

YAHOO.hippo.DDBaseModel = function(id, sGroup, config) { 
    YAHOO.hippo.DDBaseModel.superclass.constructor.apply(this, arguments); 
    this.initPlayer(id, sGroup, config); 
};

YAHOO.extend(YAHOO.hippo.DDBaseModel, YAHOO.util.DDProxy, {
    
    TYPE: "DDBaseModel",
    
     initPlayer: function(id, sGroup, config) {
        if (!id) { 
            return; 
        }
        
        YAHOO.util.DDM.mode = YAHOO.util.DDM.POINT;

        this.initStyle();

        // specify that this is not currently a drop target
        this.isTarget = false;
        this.originalStyles = [];
        this.type = YAHOO.hippo.DDBaseModel.TYPE;
        this.startPos = YAHOO.util.Dom.getXY( this.getEl() );
        this.label = config.label;
        
        YAHOO.log(id + " startpos: " + this.startPos, "info", "hippo");
    },
    
    initStyle: function() {
        var el = this.getDragEl();
        YAHOO.util.Dom.setStyle(el, "borderColor", "transparent");
    },
    
    setStartStyle: function() {
    },
    
    setEndStyle: function() {
    },
    
    startDrag: function(x, y) {
        if(!this.startPos) {
           this.startPos = YAHOO.util.Dom.getXY(this.getEl()); 
        }
        YAHOO.log(this.id + " startDrag", "info", "hippo");
        var Dom = YAHOO.util.Dom;
        
        this.setStartStyle();

        var targets = YAHOO.util.DDM.getRelated(this, true);
        YAHOO.log(targets.length + " targets", "info", "hippo");

        for (var i=0; i<targets.length; i++) {
            var targetEl = this.getTargetDomRef(targets[i]);

            if (!this.originalStyles[targetEl.id]) {
                this.originalStyles[targetEl.id] = targetEl.className;
            }
            YAHOO.log("Old style = " + this.originalStyles[targetEl.id], "info", "hippo");
            targetEl.className = targetEl.className + " drag-drop-target";
        }
    },

    getTargetDomRef: function(oDD) {
        if (oDD.resource) {
            return oDD.resource.getEl();
        } else {
            return oDD.getEl();
        }
    },

    endDrag: function(e) {
        this.setEndStyle();
        this.resetTargets();
    },

    resetTargets: function() {
        // reset the target styles
        
        var targets = YAHOO.util.DDM.getRelated(this, true);
        for (var i=0; i<targets.length; i++) {
            var targetEl = this.getTargetDomRef(targets[i]);
            var oldStyle = this.originalStyles[targetEl.id];
            if (oldStyle || oldStyle == '') {
                targetEl.className = oldStyle;
            } 
        }
    },
    
    getCallbackParameters: function(dropId) {
        var params = new Array();
        params.push( {key :'targetId', value :dropId} );
        return params;
    },
    
    cancelCallback: function() {
        return false;
    },
    
    onDragDropAction: function(dropId) {
    },
    
    onInvalidDrop: function(e) { 
    }, 
    
    onDragOver: function(e, id) {
    },
    
    onDrag: function(e, id) {
    }
});

YAHOO.hippo.DDModel = function(id, sGroup, config) { 
    YAHOO.hippo.DDModel.superclass.constructor.apply(this, arguments); 
       this.initPlayer(id, sGroup, config); 
};

YAHOO.extend(YAHOO.hippo.DDModel, YAHOO.hippo.DDBaseModel, {
    
    TYPE: "DDModel",
    
    initStyle: function() {
        YAHOO.hippo.DDModel.superclass.initStyle.call(this);
        var el = this.getDragEl();
        YAHOO.util.Dom.setStyle(el, "opacity", 0.76);
    },

    setStartStyle: function() {
        YAHOO.log(this.id + " setStartStyle", "info", "hippo");
        var Dom = YAHOO.util.Dom;

        var dragEl = this.getDragEl();
        dragEl.innerHTML = this.label;
        Dom.setStyle(dragEl, "color",  Dom.getStyle(clickEl, "color"));
        
        var clickEl = this.getEl();
        Dom.setStyle(clickEl, "opacity", 0.4);
    },

    setEndStyle: function() {
        YAHOO.util.Dom.setStyle(this.getEl(), "opacity", 1);
    },

    onDragDrop: function(e, id) {
        this.resetTargets();//maybe unneeded        
    }
  
});

YAHOO.hippo.DDRemove = function(id, sGroup, config) { 
    YAHOO.hippo.DDRemove.superclass.constructor.apply(this, arguments); 
       this.initPlayer(id, sGroup, config); 
};

YAHOO.extend(YAHOO.hippo.DDRemove, YAHOO.util.DDProxy, {
    
    TYPE: "DDRemove",
    
     initPlayer: function(id, sGroup, config) {
        if (!id) { 
            return; 
        }
        
        YAHOO.util.DDM.mode = YAHOO.util.DDM.POINT;

        var el = this.getDragEl()
        YAHOO.util.Dom.setStyle(el, "borderColor", "transparent");
        YAHOO.util.Dom.setStyle(el, "opacity", 0.76);

        // specify that this is not currently a drop target
        this.isTarget = false;
        this.type = YAHOO.hippo.DDRemove.TYPE;

        this.startPos = YAHOO.util.Dom.getXY( this.getEl() );
        YAHOO.log(id + " startpos: " + this.startPos, "info", "hippo");
        
        this.label = config.label;
        this.currentGroup = config.currentGroup;
    },

    startDrag: function(x, y) {
        if(!this.startPos) {
           this.startPos = YAHOO.util.Dom.getXY(this.getEl()); 
        }
        var Dom = YAHOO.util.Dom;

        var dragEl = this.getDragEl();
        var clickEl = this.getEl();
        
        dragEl.innerHTML = this.label;
        dragEl.innerHTML = '<div style="padding-left:15px;padding-top:5px;width:120px;"><div>Remove <ul>' + clickEl.innerHTML + '</ul></div></div>';
        
        Dom.setStyle(dragEl, "color",  Dom.getStyle(clickEl, "color"));
        Dom.setStyle(clickEl, "opacity", 0.4);
        
    },

    endDrag: function(e) {
        //fade effect?
        YAHOO.util.Dom.setStyle(this.getEl(), "opacity", 0.2);
        //YAHOO.util.Dom.setStyle(this.getEl(), "display", 'none');
    },

    onDragDrop: function(e, id) {
    },

    onInvalidDrop: function(e) { 
    }, 

    onDragOver: function(e, id) {
    },

    onDrag: function(e, id) {
        
    }
});

YAHOO.hippo.DDImage = function(id, sGroup, config) { 
    YAHOO.hippo.DDImage.superclass.constructor.apply(this, arguments); 
       this.initPlayer(id, sGroup, config); 
};

YAHOO.extend(YAHOO.hippo.DDImage, YAHOO.hippo.DDBaseModel, {
    
    TYPE: "DDImage",
    
     initPlayer: function(id, sGroup, config) {
        if (!id) { 
            return; 
        }
        YAHOO.hippo.DDImage.superclass.initPlayer.call(this, id, sGroup, config);
        
        this.nodePath = config.nodePath;
        this.currentGroup = config.currentGroup;
    },
    
    setStartStyle: function() {
        var Dom = YAHOO.util.Dom;
        var dragEl = this.getDragEl();
        var clickEl = this.getEl();
        
        dragEl.innerHTML = this.label;
        dragEl.innerHTML = '<div style="padding-left:15px;padding-top:5px;width:120px;"><div><img src="' + clickEl.src + '" /></div></div>';
        
        //dragEl.innerHTML = '<div class="drag-image-mask"><div class="drag-image-mask-content"><img src="' + clickEl.src + '" width="90"/></div><span>' + this.label + '</span></div>';
        
        Dom.setStyle(dragEl, "color",  Dom.getStyle(clickEl, "color"));
        Dom.setStyle(clickEl, "opacity", 0.4);
    },
    
    setEndStyle: function() {
        YAHOO.util.Dom.setStyle(this.getEl(), "opacity", 1);
    },
    
    onDragDropAction: function(dropId) {
        var textAreas = YAHOO.util.Dom.getElementsByClassName('xinha_textarea', 'textarea', YAHOO.util.Dom.get(dropId));
        if(textAreas == null || textAreas[0] == null)
            return null;
        
        var id = textAreas[0].id;
        var x= eval('xinha_editors.' + id);

        var img = new Image();
        img.src = 'drop-on-xinha' + this.nodePath;
        if ( Xinha.is_ie ) {
            var sel = x.getSelection();
            var range = x.createRange(sel);
            //TODO: check if this still works.
            x._doc.execCommand("insertimage", false, img.src);
            img = range.parentElement();
            // wonder if this works...
            if ( img.tagName.toLowerCase() != "img" ) {
                img = img.previousSibling;
            }
        } else {
            //gecko/webkit
            x.insertNodeAtSelection(img);
            if ( !img.tagName ) {
                // if the cursor is at the beginning of the document
                img = x.range.startContainer.firstChild;
            }
        }
        x._insertImage(img);
    },
    
    cancelCallback: function() {
        return true;
    }

});

YAHOO.hippo.DDInsertImage = function(id, sGroup, config) { 
    YAHOO.hippo.DDInsertImage.superclass.constructor.apply(this, arguments); 
       this.initPlayer(id, sGroup, config); 
};

YAHOO.extend(YAHOO.hippo.DDInsertImage, YAHOO.hippo.DDImage, {
    TYPE: "DDInsertImage",
    
    cancelCallback: function() {
        return false;
    },
    
    onDragDropAction: function(dropId) {
    }
    
});