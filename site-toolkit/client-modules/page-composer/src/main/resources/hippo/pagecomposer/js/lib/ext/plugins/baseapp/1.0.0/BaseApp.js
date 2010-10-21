/*!
 * Ext JS Library 3.2.1
 * Copyright(c) 2006-2010 Ext JS, Inc.
 * licensing@extjs.com
 * http://www.extjs.com/license
 */
/**
 * Ext.App
 * @extends Ext.util.Observable
 * @author Chris Scott
 * @abogaart added
 */
Ext.App = function(config) {

    // set up StateProvider
    this.initStateProvider();

    // array of views
    this.views = [];

    //debug flag
    this.debug = false;

    Ext.apply(this, config);
    if (!this.api.actions) {
        this.api.actions = {};
    }
    this.addEvents({
        'ready' : true,
        'beforeunload' : true,
        'beforeQuit' : true
    });

    // init when onReady fires.
    Ext.onReady(this.initApp, this);

    Ext.App.superclass.constructor.apply(this, arguments);
}
Ext.extend(Ext.App, Ext.util.Observable, {

    /***
     * response status codes.
     */
    STATUS_EXCEPTION :          'exception',
    STATUS_VALIDATION_ERROR :   "validation",
    STATUS_ERROR:               "error",
    STATUS_NOTICE:              "notice",
    STATUS_OK:                  "ok",
    STATUS_HELP:                "help",

    /**
     * @cfg {Object} api
     * remoting api
     */
    api: {
        url: null,
        type: null,
        actions: {}
    },

    // private, ref to message-box Element.
    msgCt : null,

    // @protected, onReady, executes when Ext.onReady fires.
    initApp : function() {
        Ext.QuickTips.init();

        if (this.loadMessage) {
            this.onReady(function() {
                this.addNotice(this.loadMessage);
            }, this);
        }

        // create the msgBox container.  used for App.addAlert
        this.msgCt = Ext.DomHelper.insertFirst(document.body, {id:'msg-div'}, true);
        this.msgCt.setStyle('position', 'absolute');
        this.msgCt.setStyle('z-index', 9999);
        this.msgCt.setWidth(300);

        this.initMessageHandling();

        this.init();

        Ext.EventManager.on(window, 'beforeunload', this.onUnload, this);
        this.fireEvent('ready', this);
        this.isReady = true;
    },

    initMessageHandling : function() {
        if (this.debug) {
            Ext.data.DataProxy.addListener('exception', function(proxy, type, action, options, res, e) {
                if (!res.success && res.message) {
                    this.addAlert(false, "Server-side error occurred while executing action=" + action);
                    console.error('Server side error: ' + res.message);
                }
                else {
                    if (e && typeof console.error == 'function') {
                        console.error(e);
                    } else {
                        console.group("Exception");
                        console.dir(arguments);
                        console.groupEnd();
                    }
                    this.addAlert(false, "Something bad happened while executing " + action);
                }
            }, this);

            Ext.data.DataProxy.addListener('write', function(proxy, action, result, res, rs) {
                this.addAlert(true, 'Action: ' + action + '<br/>Message: ' + res.message);
            }, this);
        }
    },

    //Template method for subclass initialisation
    init : Ext.emptyFn,

    onReady : function(fn, scope) {
        if (!this.isReady) {
            this.on('ready', fn, scope);
        } else {
            fn.call(scope, this);
        }
    },

    quit: function() {
        this.addNotice('Quitting..');
        this.fireEvent('beforeQuit', this);
    },

    //Alias for this.on.beforeQuit
    beforeQuit : function(fn, scope) {
        this.on('beforeQuit', fn, scope);
    },

    initStateProvider : function() {
        /*
         * set days to be however long you think cookies should last
         */
        var days = '';        // expires when browser closes
        if (days) {
            var date = new Date();
            date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
            var exptime = "; expires=" + date.toGMTString();
        } else {
            var exptime = null;
        }

        // register provider with state manager.
        Ext.state.Manager.setProvider(new Ext.state.CookieProvider({
            path: '/',
            expires: exptime,
            domain: null,
            secure: false
        }));
    },

    /**
     * registerView
     * register an application view component.
     * @param {Object} view
     */
    registerView : function(view) {
        this.views.push(view);
    },

    /**
     * getViews
     * return list of registered views
     */
    getViews : function() {
        return this.views;
    },

    /**
     * registerActions
     * registers new actions for API
     * @param {Object} actions
     */
    registerActions : function(actions) {
        Ext.apply(this.api.actions, actions);
    },

    /**
     * getAPI
     * return Ext Remoting api
     */
    getAPI : function() {
        return this.api;
    },

    /***
     * addAlert
     * show the message box.  Aliased to addMessage
     * @param {String} msg
     * @param {Bool} status
     */
    addAlert : function(status, msg) {
        this.addMessage(status, msg);
    },

    addNotice : function(msg) {
        this.addMessage(this.STATUS_NOTICE, msg);
    },

    /***
     * adds a message to queue.
     * @param {String} msg
     * @param {Bool} status
     */
    addMessage : function(status, msg) {
        var delay = 1.5;    // <-- default delay of msg box is 1 second.
        if (status == false) {
            delay = 5;    // <-- when status is error, msg box delay is 3 seconds.
        }

        this.msgCt.alignTo(document, 't-t');
        Ext.DomHelper.append(this.msgCt, {html:this.buildMessageBox(status, String.format.apply(String, Array.prototype.slice.call(arguments, 1)))}, true).slideIn('t').pause(delay).ghost("t", {remove:true});
    },

    /***
     * buildMessageBox
     */
    buildMessageBox : function(title, msg) {
        switch (title) {
            case true:
                title = this.STATUS_OK;
                break;
            case false:
                title = this.STATUS_ERROR;
                break;
        }
        return [
            '<div class="app-msg">',
            '<div class="x-box-tl"><div class="x-box-tr"><div class="x-box-tc"></div></div></div>',
            '<div class="x-box-ml"><div class="x-box-mr"><div class="x-box-mc"><h3 class="x-icon-text icon-status-' + title + '">', title, '</h3>', msg, '</div></div></div>',
            '<div class="x-box-bl"><div class="x-box-br"><div class="x-box-bc"></div></div></div>',
            '</div>'
        ].join('');
    },

    /**
     * decodeStatusIcon
     * @param {Object} status
     */
    decodeStatusIcon : function(status) {
        iconCls = '';
        switch (status) {
            case true:
            case this.STATUS_OK:
                iconCls = this.ICON_OK;
                break;
            case this.STATUS_NOTICE:
                iconCls = this.ICON_NOTICE;
                break;
            case false:
            case this.STATUS_ERROR:
                iconCls = this.ICON_ERROR;
                break;
            case this.STATUS_HELP:
                iconCls = this.ICON_HELP;
                break;
        }
        return iconCls;
    },

    /***
     * setViewState, alias for Ext.state.Manager.set
     * @param {Object} key
     * @param {Object} value
     */
    setViewState : function(key, value) {
        Ext.state.Manager.set(key, value);
    },

    /***
     * getViewState, aliaz for Ext.state.Manager.get
     * @param {Object} cmd
     */
    getViewState : function(key) {
        return Ext.state.Manager.get(key);
    },

    handleResponse : function(res) {
        if (res.type == this.STATUS_EXCEPTION) {
            return this.handleException(res);
        }
        if (res.message.length > 0) {
            this.addAlert(res.status, res.message);
        }
    },

    handleException : function(res) {
        Ext.MessageBox.alert(res.type.toUpperCase(), res.message);
    },

    onUnload : function(e) {
        if (this.fireEvent('beforeunload', this) === false) {
            e.stopEvent();
        }
    }

});