/**
 * trivial futures implementation.
 */
if (typeof Hippo == 'undefined') {
    Hippo = {};
}

Hippo.Future = function(func, options) {
    options = options || {};
    this.continuingFuture = options.continuingFuture || false;

    this.success = false;
    this.completed = false;
    this.value = undefined;

    this.successHandlers = [];
    this.failureHandlers = [];

    var self = this;
    func.call(this, function(value) {
        self.onSuccess.call(self, value)
    }, function() {
        self.onFailure.call(self)
    });
};

Hippo.Future.prototype = {

    when: function(cb) {
        if (this.continuingFuture) {
            this.successHandlers.push(cb);
            if (this.completed) {
                cb.call(this, this.value);
            }
        } else {
            if (!this.completed) {
                this.successHandlers.push(cb);
            } else if (this.success) {
                cb.call(this, this.value);
            }
        }
        return this;
    },

    otherwise: function(cb) {
        if (this.continuingFuture) {
            this.failureHandlers.push(cb);
            if (!this.success) {
                cb.call(this);
            }
        } else {
            if (!this.completed) {
                this.failureHandlers.push(cb);
            } else if (!this.success) {
                cb.call(this);
            }
        }
        return this;
    },

    get: function() {
        if (!this.completed) {
            throw "Future has not completed yet";
        }
        return this.value;
    },

    onSuccess: function(value) {
        if (!this.continuingFuture && this.completed) {
            return;
        }
        this.value = value;
        this.success = true;
        this.completed = true;
        for (var i = 0; i < this.successHandlers.length; i++) {
            this.successHandlers[i].call(this, value);
        }
        if (!this.continuingFuture) {
            this.cleanup();
        }
    },

    onFailure: function() {
        if (!this.continuingFuture && this.completed) {
            return;
        }
        this.success = false;
        this.completed = true;
        for (var i = 0; i < this.failureHandlers.length; i++) {
            this.failureHandlers[i].call(this);
        }
        if (!this.continuingFuture) {
            this.cleanup();
        }
    },

    cleanup: function() {
        delete this.successHandlers;
        delete this.failureHandlers;
    }

};

Hippo.Future.join = function() {
    var futures;
    if (arguments.length == 1) {
        futures = arguments[0];
    } else {
        futures = Array.prototype.slice.call(arguments);
    }

    var value = null;
    var join = new Hippo.Future(function(onSuccess, onFailure) {
        var completed = false;
        var togo = futures.length;
        var failureHandler = function() {
            togo--;
            if (completed) {
                return;
            }
            completed = true;
            onFailure.call(this);
        };
        for (var i = 0; i < futures.length; i++) {
            futures[i].when(function(result) {
                togo--;
                if (!completed && togo === 0) {
                    completed = true;
                    onSuccess.call(this, value);
                }
            }).otherwise(failureHandler);
        }
    });
    join.set = function(val) {
        value = val;
    };
    return join;
};