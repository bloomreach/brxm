/*
 * Copyright (c) <2011> <Ryan Petrello>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

Ext.namespace('Ext.ux');

Ext.ux.ColorField = Ext.extend(Ext.form.TriggerField,  {

    triggerConfig: {
        src: Ext.BLANK_IMAGE_URL,
        tag: "img",
        cls: "x-form-trigger x-form-color-trigger"
    },
    invalidText : "Colors must be in a the hex format #FFFFFF.",
    regex: /^\#[0-9A-F]{6}$/i,
    allowBlank: false,
    fallback: true,

    initComponent : function() {
        Ext.ux.ColorField.superclass.initComponent.call(this);
        this.addEvents('select');
        this.on('change', function(c, v) {
            this.onSelect(c, v);
        }, this);
    },

    // private
    onDestroy : function() {
		Ext.destroy(this.menu);
        Ext.ux.ColorField.superclass.onDestroy.call(this);
    },
    
    // private
    afterRender: function() {
        Ext.ux.ColorField.superclass.afterRender.call(this);
        this.el.setStyle('background', this.value);
        this.detectFontColor();        
    },

    /**
     * @method onTriggerClick
     * @hide
     */
    // private
    onTriggerClick : function() {
        if (this.disabled) {
            return;
        }
        if (this.menu === undefined) {
            this.menu = new Ext.ux.ColorMenu({
                hideOnClick: false,
                fallback: this.fallback
            });
        }
        this.onFocus();
        this.menu.picker.setValue(this.getValue() || '#FFFFFF');
        this.menu.show(this.el, "tl-bl?");
        this.menuEvents('on');
    },
    
    //private
    menuEvents: function(method) {
        this.menu[method]('select', this.onSelect, this);
        this.menu[method]('hide', this.onMenuHide, this);
        this.menu[method]('show', this.onFocus, this);
    },
    
    onSelect: function(m, d) {
        this.setValue(d);
        this.fireEvent('select', this, d);
        this.el.setStyle('background', d);
        this.detectFontColor();
    },
    
    // private
    // Detects whether the font color should be white or black, according to the
    // current color of the background
    detectFontColor : function () {
        var h2d, value, avg;
        if (!this.menu || !this.menu.picker.rawValue) {
            if (!this.value) {
                value = 'FFFFFF';
            } else {
                h2d = function(d){ return parseInt(d, 16); };
                value = [
                    h2d(this.value.slice(1, 3)),
                    h2d(this.value.slice(3, 5)),
                    h2d(this.value.slice(5))
                ];
            }
        } else {
            value = this.menu.picker.rawValue;
        }
        avg = (value[0] + value[1] + value[2]) / 3;
        this.el.setStyle('color', (avg > 128) ? '#000' : '#FFF');
    },    
    
    onMenuHide: function(){
        this.focus(false, 60);
        this.menuEvents('un');
    }
    
});    

Ext.ux.ColorMenu = Ext.extend(Ext.menu.Menu, {

   enableScrolling: false,

   initComponent: function() {
       
       Ext.apply(this, {
           plain: true,
           showSeparator: false,
           items: this.picker = new Ext.ux.ColorPicker(Ext.apply({
               internalRender: this.strict || !Ext.isIE,
               wheelImage: this.wheelImage,
               gradientImage: this.gradientImage,
               fallback: this.fallback
           }, this.initialConfig))
       });
       this.picker.purgeListeners();
       Ext.ux.ColorMenu.superclass.initComponent.call(this);
       this.relayEvents(this.picker, ["select"]);
       this.on('select', this.menuHide, this);
       if (this.handler) {
           this.on('select', this.handler, this.scope || this);
       }
   },

   menuHide: function() {
       if (this.hideOnClick){
           this.hide(true);
       }
   },
   
   doLayout: function(shallow, force) {
       Ext.ux.ColorMenu.superclass.doLayout.call(this, shallow, force);
       this.getEl().setZIndex(30000);
   }
   
});

/* Preload the picker images so they're available at render time */
Ext.ux.ColorMenu.prototype.wheelImage = (function() {
    var wheelImage = new Image();
    wheelImage.onload = Ext.emptyFn;
    wheelImage.src = 'wicket/resource/org.onehippo.cms7.channelmanager.channeleditor.ChannelEditorHeaderItem/plugins/colorfield/images/wheel.png';
    return wheelImage;
}());

Ext.ux.ColorMenu.prototype.gradientImage = (function() {
    var gradientImage = new Image();
    gradientImage.onload = Ext.emptyFn;
    gradientImage.src = 'wicket/resource/org.onehippo.cms7.channelmanager.channeleditor.ChannelEditorHeaderItem/plugins/colorfield/images/gradient.png';
    return gradientImage;
}());

Ext.ux.ColorPicker = function(config) {
    Ext.ux.ColorPicker.superclass.constructor.call(this, config);
    this.addEvents(
        /**
	     * @event select
	     * Fires when a color is selected
	     * @param {ColorPalette} this
	     * @param {String} color The 6-digit color hex code (without the # symbol)
	     */
        'select'
    );
    
    if (!this.value) {
        this.value = this.defaultValue;
    }

    if (this.handler) {
        this.on("select", this.handler, this.scope, true);
    }
    
};
Ext.extend(Ext.ux.ColorPicker, Ext.ColorPalette, {
    canvasSupported: true,
    itemCls: 'x-color-picker',
    defaultValue: "#0000FF",
    width: 200,
    // private
    onRender : function(container, position){
        var el;
        if (!this.value) {
            this.value = this.defaultValue;
        }
        el = document.createElement("div");
        el.className = this.itemCls;
        container.dom.insertBefore(el, position);
        Ext.get(el).setWidth(this.width);
        this.canvasdiv = Ext.get(el).createChild({
            tag: 'div'
        });
        this.wheel = this.canvasdiv.dom.appendChild(document.createElement("canvas"));
        this.wheel.setAttribute('width', '200');
        this.wheel.setAttribute('height', '200');
        this.wheel.setAttribute('class', 'x-color-picker-wheel');
        
        if (this.fallback || !this.wheel.getContext || !this.wheel.getContext('2d').getImageData) {
            this.canvasSupported = false;
            this.itemCls = 'x-color-palette';
            while (container.dom.firstChild) {
                container.dom.removeChild(container.dom.firstChild);
            }
            Ext.ux.ColorPicker.superclass.onRender.call(this, container, position);
            return;
        }
        
        /* Draw the wheel image onto the container */
        this.wheel.getContext('2d').drawImage(this.wheelImage, 0, 0);
        this.drawGradient();
        
        Ext.get(this.wheel).on('click', this.select, this);
        
        this.el = Ext.get(el);
    },
    
    // private
    afterRender : function() {
        var t, self;
        Ext.ColorPalette.superclass.afterRender.call(this);
        if (!this.canvasSupported) {
            return;
        }
        /* Fire selection events on drag */
        t = new Ext.dd.DragDrop(this.wheel);
        self = this;
        t.onDrag = function(e, t) {
            self.select(e, this.DDM.currentTarget);
        };
    },
    
    select : function(e, t) {
        var context, coords, data;
        if (!this.canvasSupported) {
            this.value = e;
            Ext.ux.ColorPicker.superclass.select.call(this, e);
            this.fireEvent('select', this, '#'+this.value); 
            return;
        }
        context = this.wheel.getContext('2d');
        coords = [
            e.xy[0] - Ext.get(t).getLeft(),
            e.xy[1] - Ext.get(t).getTop()
        ];
        
        try {
            data = context.getImageData(coords[0], coords[1], 1, 1);
        } catch(exception) { return; } // The user selected an area outside the <canvas>
        
        // Disallow selecting transparent regions
        if (data.data[3] === 0) {
            context = this.gradient.getContext('2d');
            data = context.getImageData(coords[0], coords[1], 1, 1);
            if (data.data[3] === 0) {
                return;
            }
            
            this.rawValue = data.data;
            this.value = this.hexValue(data.data[0], data.data[1], data.data[2]);
            this.fireEvent('select', this, this.value);
        } else {
            this.rawValue = data.data;
            this.value = this.hexValue(data.data[0], data.data[1], data.data[2]);
            this.drawGradient();
            this.fireEvent('select', this, this.value);
        }
    },
    
    // private
    drawGradient : function() {
        var context, center;
        if (!this.gradient) {
            this.gradient = this.canvasdiv.dom.appendChild(document.createElement("canvas"));
            this.gradient.setAttribute('width', '200');
            this.gradient.setAttribute('height', '200');
            this.gradient.setAttribute('class', 'x-color-picker-gradient');
            if (G_vmlCanvasManager !== undefined) {
                this.gradient = G_vmlCanvasManager.initElement(this.gradient);
            }
            Ext.get(this.gradient).on('click', this.select, this);
        }
        context = this.gradient.getContext('2d');
        center = [97.5, 98];
        
        // Clear the canvas first
        context.clearRect(0, 0, this.gradient.width, this.gradient.height);
        
        context.beginPath();
        context.fillStyle = this.value;
        context.strokeStyle = this.value;
        context.arc(center[0], center[0], 65, 0, 2*Math.PI, false);
        context.closePath();
        context.fill();

        /* Draw the wheel image onto the container */
        this.gradient.getContext('2d').drawImage(this.gradientImage, 33, 32);
    },
    
    // private
    hexValue : function(r, g, b) {
        var chars = '0123456789ABCDEF';
        return '#'+(
            chars[parseInt(r/16, 10)] + chars[parseInt(r%16, 10)] +
            chars[parseInt(g/16, 10)] + chars[parseInt(g%16, 10)] +
            chars[parseInt(b/16, 10)] + chars[parseInt(b%16, 10)]
        );
    },
    
    getValue: function() {
        return this.value;
    },
    
    setValue: function(v) {
        this.value = v;
    }
});
Ext.reg('colorfield', Ext.ux.ColorField);
