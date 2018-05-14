/*
 *  Copyright 2010-2015 Hippo B.V. (http://www.onehippo.com)
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

Ext.ns('Hippo.ux.window');

Hippo.ux.window.FloatingWindow = Ext.extend(Ext.Window, {

    draggable: true,

    initComponent: function() {
        var w = 0, t, region, marginX;
        if (typeof window.innerWidth === 'number') {
            // Non-IE
            w = window.innerWidth;
        } else if (document.documentElement && document.documentElement.clientWidth) {
            // IE 6+ in 'standards compliant mode'
            w = document.documentElement.clientWidth;
        }
        t = Ext.getBody().getScroll().top;

        region = Ext.isEmpty(this.initialConfig.initRegion) ? 'center' : this.initialConfig.initRegion;
        if (region !== 'center') {
            marginX = Ext.isEmpty(this.initialConfig.x) ? 0 : this.initialConfig.x;
            if (region === 'right') {
                this.initialConfig.x = w - (this.initialConfig.width + marginX + Ext.getScrollBarSize());
                this.initialConfig.y += t;
            }
        } else {
            if (!Ext.isEmpty(this.initialConfig.y)) {
                this.initialConfig.y += t;
            }
        }

        Ext.apply(this, Ext.apply(this.initialConfig, {
            listeners: {
                show: function() {
                    this.saveRelativePosition();
                }
            }
        }));

        this.addEvents('startdrag', 'enddrag');

        Hippo.ux.window.FloatingWindow.superclass.initComponent.apply(this, arguments);
    },

    /**
     * Store the window position relative to the viewport.
     */
    saveRelativePosition: function() {
        this.relPos = this.getPosition();
        this.relPos[1] -= Ext.getBody().getScroll().top; //subtract pixels scrolled vertically
    },

    /**
     * Restore the window position to the relative stored position
     */
    restoreRelativePosition: function() {
        var active, cb;

        active = Ext.WindowMgr.getActive();
        if (active !== null) {
            active.setActive(false);
        }

        if (this.el.hasActiveFx()) {
            this.el.stopFx();
        }
        cb = function() {
            if (this.activateTimeout !== null && this.activateTimeout !== undefined) {
                window.clearTimeout(this.activateTimeout);
            }
            this.activateTimeout = window.setTimeout(function() {
                active.setActive(true);
                this.activateTimeout = null;
            }.createDelegate(this), 750);
        }.createDelegate(this);

        this.el.shift({
            x: this.relPos[0],
            y: this.relPos[1] + Ext.getBody().getScroll().top,
            easing: 'easeOut',
            duration: 0.35,
            callback: cb
        });
    },

    /**
     * Override and instantiate custom DD
     */
    initDraggable: function() {
        var del;
        this.dd = new Hippo.ux.window.FloatingWindow.DD(this);

        del = this.restoreRelativePosition.createDelegate(this);
        Ext.EventManager.on(window, 'scroll',
                del,
                window, {buffer: 50}
        );
        this.on('beforeclose', function() {
            Ext.EventManager.un(window, 'scroll', del);
        });
    }

});

Ext.reg('hFloatingWindow', Hippo.ux.window.FloatingWindow);

/**
 * Could not get Window.drag hooks to work, so for now Ext.Window.DD is extended
 * @param win
 */
Hippo.ux.window.FloatingWindow.DD = function(win) {
    this.win = win;
    Hippo.ux.window.FloatingWindow.DD.superclass.constructor.call(this, win);
};

Ext.extend(Hippo.ux.window.FloatingWindow.DD, Ext.Window.DD, {

    startDrag: function() {
        Hippo.ux.window.FloatingWindow.DD.superclass.startDrag.apply(this);
        this.win.fireEvent('startdrag');
    },

    endDrag: function() {
        Hippo.ux.window.FloatingWindow.DD.superclass.endDrag.apply(this);
        this.win.saveRelativePosition();
        this.win.fireEvent('enddrag');
    }
});
