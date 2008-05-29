/*
 * Copyright 2007 Hippo
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
 *
 * @extends YAHOO.util.DDProxy
 * @constructor
 * @param {String} id the id of the linked element
 * @param {String} sGroup the group of related DragDrop objects
 */
YAHOO.namespace("hippo"); 
 
YAHOO.hippo.DDNode = function(id, sGroup, config) { 
    YAHOO.hippo.DDNode.superclass.constructor.apply(this, arguments); 
       this.initPlayer(id, sGroup, config); 
};

YAHOO.extend(YAHOO.hippo.DDNode, YAHOO.util.DDProxy, {
    
    TYPE: "DDNode",
    
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

        this.originalStyles = [];

        this.type = YAHOO.hippo.DDNode.TYPE;
        this.slot = null;

        this.startPos = YAHOO.util.Dom.getXY( this.getEl() );
        YAHOO.log(id + " startpos: " + this.startPos, "info", "hippo");
        
        this.label = config.label;
    },

    startDrag: function(x, y) {
        if(!this.startPos) {
           this.startPos = YAHOO.util.Dom.getXY(this.getEl()); 
        }
        YAHOO.log(this.id + " startDrag", "info", "hippo");
        var Dom = YAHOO.util.Dom;

        var dragEl = this.getDragEl();
        var clickEl = this.getEl();
        
        dragEl.innerHTML = this.label;
        //dragEl.className = clickEl.className;

        //Dom.setStyle(dragEl, "opacity",  0.9);
        
        Dom.setStyle(dragEl, "color",  Dom.getStyle(clickEl, "color"));
        //Dom.setStyle(dragEl, "backgroundColor", Dom.getStyle(clickEl, "backgroundColor"));

        Dom.setStyle(clickEl, "opacity", 0.4);

        var targets = YAHOO.util.DDM.getRelated(this, true);
        YAHOO.log(targets.length + " targets", "info", "hippo");
        for (var i=0; i<targets.length; i++) {
            
            var targetEl = this.getTargetDomRef(targets[i]);

            if (!this.originalStyles[targetEl.id]) {
                this.originalStyles[targetEl.id] = targetEl.className;
            }
            YAHOO.log("Old style = " + this.originalStyles[targetEl.id], "info", "hippo");
            targetEl.className = targetEl.className + " target";
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
        // reset the linked element styles
        YAHOO.util.Dom.setStyle(this.getEl(), "opacity", 1);

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

    onDragDrop: function(e, id) {
        // get the drag and drop object that was targeted
        var oDD;
        
        if ("string" == typeof id) {
            oDD = YAHOO.util.DDM.getDDById(id);
        } else {
            oDD = YAHOO.util.DDM.getBestMatch(id);
        }
        
        YAHOO.log('ondragdrop: oDD=' + oDD.id, "info", "hippo");

        var el = this.getEl();

        // check if the slot has a player in it already
        if (oDD.resource) {
            YAHOO.log("oDD.resource exists", "info", "hippo");
            // check if the dragged player was already in a slot
            if (this.slot) {
                YAHOO.log("this.slot exists", "info", "hippo");
                // check to see if the player that is already in the
                // slot can go to the slot the dragged player is in
                // YAHOO.util.DDM.isLegalTarget is a new method
                if ( YAHOO.util.DDM.isLegalTarget(oDD.resource, this.slot) ) {
                    YAHOO.log("swapping player positions", "info", "hippo");
                    YAHOO.util.DDM.moveToEl(oDD.resource.getEl(), el);
                    this.slot.resource = oDD.resource;
                    oDD.resource.slot = this.slot;
                } else {
                    YAHOO.log("moving player in slot back to start", "info", "hippo");
                    YAHOO.util.Dom.setXY(oDD.resource.getEl(), oDD.resource.startPos);
                    this.slot.resource = null;
                    oDD.resource.slot = null
                }
            } else {
                // the player in the slot will be moved to the dragged
                // players start position
                YAHOO.log("this.slot doesn't exist", "info", "hippo");
                oDD.resource.slot = null;
                YAHOO.util.DDM.moveToEl(oDD.resource.getEl(), el);
            }
        } else {
            // Move the player into the emply slot
            // I may be moving off a slot so I need to clear the player ref
            YAHOO.log("oDD.resource doesn;t exist", "info", "hippo");
            if (this.slot) {
                this.slot.resource = null;
            }
        }
        
        //YAHOO.util.Dom.setXY(this.getEl(), this.startPos);
         //svar myAnim = new YAHOO.util.Anim(oDD, { width: { from: 10, to: 100 } }, 1);
         //myAnim = new YAHOO.util.Anim(el, {opacity: { to: 0 } });
         //myAnim.animate();
         //setTimeout('showDrop()', 2000);
        //YAHOO.util.DDM.moveToEl(el, oDD.getEl());
//        var pathEl = document.createElement('span');
//        pathEl.innerHTML = this.label;
//        YAHOO.util.Dom.insertAfter(oDD,pathEl);
        this.resetTargets();

        this.slot = oDD;
        this.slot.resource = this;
    },

    onInvalidDrop: function(e) { 
        // return to the start position 
        // Dom.setXY(this.getEl(), startPos); 
        // Animating the move is more intesting
        
        var myId = this.getDragEl().id;
        var myEl = this.getDragEl();
        var myStartPos = this.startPos;
        
        //alert('myId=' + myId + ' - myStartPos=' + myStartPos);
        
        
/*        new YAHOO.util.Motion(  
            myEl, {  
                points: {  
                    to: myStartPos 
                } 
            },  
            1,  
            YAHOO.util.Easing.easeOut  
        ).animate();
*/    }, 

    swap: function(el1, el2) {
        var Dom = YAHOO.util.Dom;
        var pos1 = Dom.getXY(el1);
        var pos2 = Dom.getXY(el2);
        Dom.setXY(el1, pos2);
        Dom.setXY(el2, pos1);
    },

    onDragOver: function(e, id) {
    },

    onDrag: function(e, id) {
    }
});