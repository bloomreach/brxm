/*
 * Copyright 2009 Hippo

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
 * @description
 * <p>
 * Provides a singleton manager for datetime pickers
 * </p>
 * @namespace YAHOO.hippo
 * @requires calendar
 * @module datetime
 * @beta
 */

YAHOO.namespace('hippo');

if (!YAHOO.hippo.DateTime) {
    (function() {
        var Dom = YAHOO.util.Dom, Lang = YAHOO.lang;

        YAHOO.hippo.DateTimeImpl = function() {
        };

        YAHOO.hippo.DateTimeImpl.prototype = {

            render : function(id, config) {
                var el = Dom.get(id);
                if(el != null) {
                    if (Lang.isUndefined(el.datepicker)) {
                        el.datepicker = new YAHOO.hippo.DatePicker(id, config);
                    } else {
                        el.datepicker.update();
                    }
                } else {
                    YAHOO.log("Failed to render datepicker, element[" + id + "] not found", "error");
                }
            }
        };

        YAHOO.hippo.DatePicker = function(id, config) {
            this.init(id, config);
        };

        YAHOO.hippo.DatePicker.prototype = {
            picker : null,
            id: null,
            config: null,
            containerId : null,
                
            init : function(id, cfg) {
                this.id = id;
                this.config = cfg;
                
                if(this.containerId == null) {
                    this.containerId = Dom.generateId();
                    container = document.createElement("div");
                    container.id = this.containerId;
                    document.body.appendChild(container);
                }

                this.config.dpJs = this.id + "DpJs";
                //this.config.dp = this.id + "Dp";
                this.config.dp = this.containerId;                
                this.config.icon = this.id +"Icon";
                
                YAHOO.util.Event.addListener(cfg.icon, "click", this.showCalendar, this, true);
                YAHOO.hippo.HippoAjax.registerDestroyFunction(Dom.get(this.id), this.destroy, this);
            },
            
            /**
             * Display the YUI calendar widget. If the date is not null (should be a string) then it is parsed
             * using the provided date pattern, and set as the current date on the widget.
             */
            showCalendar : function() {
                if(this.picker == null) {
                    this.picker = new YAHOO.widget.Calendar(this.config.dpJs, this.config.dp, this.config);
                    this.picker.selectEvent.subscribe(this.selectHandler, this, true);  
                    this.picker.render();
                }
                
                var date = Dom.get(this.id).value;
                if (date) {
                    date = this._parseDate(this.config.datePattern, date);
                    if (!isNaN(date)) {
                        this.picker.select(date);
                        firstDate = this.picker.getSelectedDates()[0];
                        this.picker.cfg.setProperty("pagedate", (firstDate.getMonth() + 1) + "/" + firstDate.getFullYear());
                        this.picker.render();
                    }
                }
                this.picker.show();
                
                if (this.config.alignWithIcon) {
                    this.positionRelativeTo(this.picker.oDomContainer, this.config.icon);
                }
                
                this.picker.visible = true;
            },
            
            destroy : function() {
                YAHOO.util.Event.removeListener(this.config.icon, "click", this.showCalendar);
                
                if(this.picker != null) {
                    this.picker.destroy();
                    this.picker = null;
                }

                this.config = null;
                this.id = null;
            },
            
            selectHandler : function(type, args, cal) {
                Dom.get(this.id).value = this._substituteDate(this.config.datePattern, args[0][0]);
                
                if (this.isPickerVisible()) {
                    if (this.config.hideOnSelect) {
                        this.picker.hide();
                        //Dom.setStyle(this.config.dp, 'display', 'none');
                        this.picker.visible = false;
                    }
                    if (this.config.fireChangeEvent) {
                        var field = Dom.get(this.id);
                        if (field.onchange != null && typeof(field.onchange) != 'undefined') field.onchange();
                    }
                }
            },
            
            /** 
             * Position subject relative to target top-left by default.
             * If there is too little space on the right side/bottom,
             * the datepicker's position is corrected so that the right side/bottom
             * is aligned with the display area's right side/bottom.
             * @param subject the dom element to has to be positioned
             * @param target id of the dom element to position relative to
             */
            positionRelativeTo : function(subject, target) {
                
                targetPos = Dom.getXY(target);
                targetHeight = Dom.get(target).offsetHeight;
                subjectHeight = Dom.get(subject).offsetHeight;
                subjectWidth = Dom.get(subject).offsetWidth;     
                
                viewPortHeight = this.getViewportHeight();   
                viewPortWidth = Dom.getViewportWidth();
                
                // also take scroll position into account
                scrollPos = [Dom.getDocumentScrollLeft(), Dom.getDocumentScrollTop()];
                
                // correct datepicker's position so that it isn't rendered off screen on the right side or bottom
                if (targetPos[0] + subjectWidth > scrollPos[0] + viewPortWidth) {
                    // correct horizontal position
                    Dom.setX(subject, Math.max(targetPos[0], viewPortWidth) - subjectWidth);
                } else {
                    Dom.setX(subject, targetPos[0]);
                }
                if (targetPos[1] + targetHeight + 1 + subjectHeight > scrollPos[1] + viewPortHeight) {
                    // correct vertical position
                    Dom.setY(subject, Math.max(targetPos[1], viewPortHeight) - subjectHeight);
                } else {
                    Dom.setY(subject, targetPos[1] + targetHeight + 1);
                }
            },
            
            getViewportHeight : function() {      
                if (window.innerHeight) // all browsers except IE
                    viewPortHeight = window.innerHeight;
                else if (document.documentElement && document.documentElement.clientHeight) // IE 6 strict mode
                    viewPortHeight = document.documentElement.height;       
                else if (document.body) // other IEs
                    viewPortHeight = document.body.clientHeight;
                return viewPortHeight;
            },
            
            isPickerVisible : function() {
                return this.picker.visible;
            },
            
            /**
             * Parses date from simple date pattern. Only parses dates with yy, MM and dd like patterns, though 
             * it is safe to have time as long as it comes after the pattern (which should be the case 
             * anyway 99.9% of the time).
             */
            _parseDate : function(pattern, value) {
                numbers = value.match(/(\d+)/g);
                if (numbers == null) return Number.NaN;
                var day, month, year;
                arrayPos = 0;
                for (i = 0; i < pattern.length; i++) {
                    c = pattern.charAt(i);
                    while ((pattern.charAt(i) == c) && (i < pattern.length)) {
                        i++;
                    }
                    if (c == 'y') {
                        year = numbers[arrayPos++];
                    } else if (c == 'M') {
                        month = numbers[arrayPos++];
                    } else if (c == 'd') {
                        day = numbers[arrayPos++];
                    }
                    if (arrayPos > 2) break;
                }
                // TODO this is a bit crude. Make nicer some time.
                if (year < 100) {
                    if (year < 70) {
                        year = year * 1 + 2000;
                    } else {
                        year = year * 1 + 1900;
                    }
                }
                var date = new Date();
                date.setFullYear(year, (month - 1), day);
                return date;
            },
            
            /**
             * Return the result of interpolating the value (date) argument with the date pattern.
             * The dateValue has to be an array, where year is in the first, month in the second
             * and date (day of month) in the third slot.
             */
            _substituteDate : function(datePattern, date) {
                day = date[2];
                month = date[1];
                year = date[0];
                // optionally do some padding to match the pattern
                if(datePattern.match(/\bdd\b/)) day =   this._padDateFragment(day);
                if(datePattern.match(/\bMM\b/)) month = this._padDateFragment(month);
                if(datePattern.match(/\byy\b/)) year =  this._padDateFragment(year % 100);
                // replace pattern with real values
                return datePattern.replace(/d+/, day).replace(/M+/, month).replace(/y+/, year);
            },
            
            /** 
             * Returns a string containing the value, with a leading zero if the value is < 10.
             */
            _padDateFragment : function(value) {
                return (value < 10 ? "0" : "") + value;
            },
            
            update : function() {
            }
        };

    })();

    YAHOO.hippo.DateTime = new YAHOO.hippo.DateTimeImpl();
    
    YAHOO.register("DateTime", YAHOO.hippo.DateTime, {
        version: "2.7.0", build: "1799"            
    });
}