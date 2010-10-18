var HippoFrameDD = {};

( function() {

    var DDImpl = function() {
    };

    DDImpl.prototype = {

        init: function() {
            try {
                onhostmessage(function(msg) {
                    this.attachDrag(msg.id, msg.data);
                    return false;
                }, this, false, 'attachDrag');

                onhostmessage(function(msg) {
                    this.dropFromParent(msg);
                    return false;
                }, this, false, 'dropFromParent');

            } catch(e) {
                alert(e)
            }
        },

        dropFromParent: function(msg) {
            if(this.current != null) {
                //demo impl
                var jid = 'table#' + this.current.id;

                this.detachDragTable(this.current.id);

                for(var i=0; i<msg.data.length; ++i) {
                    $('<tr><td class="dragHandle"></td><td>' + msg.data[i].data.path + '</td></tr>').insertAfter(jid + " > tbody > tr:last");
                }

                this.attachDragTable(this.current.id);
            }
        },

        attachDrag: function(id, data) {
            var table = data.element.first(); //EXTjs-API call, should not do this..
            try {
                $(table.dom).tableDnD({
                    onDrop: function(ttable, row) {
                        var serialized = $(table.dom).tableDnDSerialize();
                        var order = serialized.split('&');
                        for (var i = 0; i < order.length; ++i) {
                            var s = order[i];
                            order[i] = s.substring(s.indexOf('=') + 1, s.length);
                        }
                        sendMessage({containerId: id, children: order}, 'rearrange');
                    },
                    dragHandle: "dragHandle"
                });
                $('#' + table.dom.id + ' tr').hover(function() {
                    $(this.cells[0]).addClass('showDragHandle');
                }, function() {
                    $(this.cells[0]).removeClass('showDragHandle');
                });
            } catch(e) {
                alert(e);
            }

            this.current = table;
        },

        attachDragTable: function(id, group) {
            try {
                $('#' + id).tableDnD({
                    onDrop: function(table, row) {
                        var serialized = $('#' + id).tableDnDSerialize();
                        var order = serialized.split('&');
                        for (var i = 0; i < order.length; ++i) {
                            var s = order[i];
                            var name = s.substring(s.indexOf('=') + 1, s.length);
                            order[i] = name;
                        }

                        sendMessage({containerId: id, children: order}, 'rearrange');
                    },
                    dragHandle: "dragHandle"
                });
                $('#' + id + ' tr').hover(function() {
                    $(this.cells[0]).addClass('showDragHandle');
                }, function() {
                    $(this.cells[0]).removeClass('showDragHandle');
                });
            } catch(e) {
                alert(e);
            }
        },

        detachDragTable: function(id) {
            try {
                $('#' + id).tableDnDDestroy();
            } catch(e) {
                alert(e);
            }
        },

        attachDrop: function(id, group) {
            var el = Ext.get(id);
            try {
                var dz1 = new Ext.dd.DropZone(id, {ddGroup: group});
            } catch(e) {
                alert(e);
            }
        },

        detachDrop: function(id) {

        }
    };

    HippoFrameDD = new DDImpl();

})();

/*
 Hippo.App.DraggableComponent = Ext.extend(Ext.dd.DD, {
 startDrag: function(x, y) {
 var dragEl = Ext.get(this.getDragEl());
 var el = Ext.get(this.getEl());

 dragEl.applyStyles({'z-index':2000});
 dragEl.update(el.dom.innerHTML);
 dragEl.addClass(el.dom.className + ' dd-proxy');
 },

 // Called the instance the element is dragged.
 b4StartDrag : function() {
 // Cache the drag element
 if (!this.el) {
 this.el = Ext.get(this.getEl());
 }

 //Cache the original XY Coordinates of the element, we'll use this later.
 this.originalXY = this.el.getXY();

 return true;
 },

 // Called when element is dropped not anything other than a dropzone with the same ddgroup
 onInvalidDrop : function() {
 // Set a flag to invoke the animated repair
 this.invalidDrop = true;
 },

 // Called when the drag operation completes
 endDrag : function() {
 // Invoke the animation if the invalidDrop flag is set to true
 if (this.invalidDrop === true) {
 // Remove the drop invitation
 this.el.removeClass('dropOK');

 // Create the animation configuration object
 var animCfgObj = {
 easing   : 'elasticOut',
 duration : 1,
 scope    : this,
 callback : function() {
 // Remove the position attribute
 this.el.dom.style.position = '';
 }
 };

 // Apply the repair animation
 this.el.moveTo(this.originalXY[0], this.originalXY[1], animCfgObj);
 delete this.invalidDrop;
 }

 },

 onDragOver: function(e, targetId) {
 //console.log('dragOver: ' + targetId);
 if ('dd1-ct' === targetId || 'dd2-ct' === targetId) {
 var target = Ext.get(targetId);
 this.lastTarget = target;
 target.addClass('dd-over');
 }
 },

 onDragOut: function(e, targetId) {
 //console.log('dragOut: ' + targetId);
 if ('dd1-ct' === targetId || 'dd2-ct' === targetId) {
 var target = Ext.get(targetId);
 this.lastTarget = null;
 target.removeClass('dd-over');
 }
 }
 });
 */