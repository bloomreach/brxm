/*
 * Copyright 2009-2017 Hippo B.V. (http://www.onehippo.com)
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

/*global Event */

YAHOO.namespace('hippo');

if (!YAHOO.hippo.DateTime) {
    (function() {
        var Dom = YAHOO.util.Dom, Lang = YAHOO.lang;

        YAHOO.hippo.DateTimeImpl = function() {
            this.current = null;
        };

        YAHOO.hippo.DateTimeImpl.prototype = {

            render : function(id, config) {
                var el = Dom.get(id);
                if (el !== null && el !== undefined) {
                    if (Lang.isUndefined(el.datepicker)) {
                        el.datepicker = new YAHOO.hippo.DatePicker(id, config);
                    } else {
                        el.datepicker.update();
                    }
                } else {
                    YAHOO.log("Failed to render datepicker, element[" + id + "] not found", "error");
                }
            },

            setCurrent : function(datePicker) {
                if (this.current !== null && this.current !== undefined) {
                    this.current.hide();
                }
                this.current = datePicker;
            }
        };

        YAHOO.hippo.DatePicker = function(id, config) {
            this.id = id;
            this.config = config;
            this.config.dpJs = this.id + "DpJs";
            this.config.icon = this.id +"Icon";

            this.picker = null;

            this.containerId = Dom.generateId();
            this.config.dp = this.containerId;

            this.container = document.createElement("div");
            this.container.id = this.containerId;
            document.body.appendChild(this.container);

            YAHOO.util.Event.addListener(this.config.icon, "click", this.onIconClicked, this, true);
            YAHOO.hippo.HippoAjax.registerDestroyFunction(Dom.get(this.id), this.destroy, this);
        };

        YAHOO.hippo.DatePicker.prototype = {

            onIconClicked : function() {
                if (this.isVisible()) {
                    this.hide();
                } else {
                    this.show();
                }
            },

            /**
             * Display the YUI calendar widget. If the date is not null (should be a string) then it is parsed
             * using the provided date pattern, and set as the current date on the widget.
             */
            show : function() {
                var render, date, firstDate;

                YAHOO.hippo.DateTime.setCurrent(this);

                render = false;
                if (this.picker === null || this.picker === undefined) {
                    this.picker = new YAHOO.widget.Calendar(this.config.dpJs, this.config.dp, this.config);
                    this._localizeCalendar(this.picker, this.config.language);
                    this.picker.selectEvent.subscribe(this.selectHandler, this, true);
                    this.picker.hideEvent.subscribe(function() {this.picker.visible = false;}, this, true);
                    render = true;
                }

                date = Dom.get(this.id).value;
                if (date) {
                    date = this._parseDate(this.config.datePattern, date);
                    if (!isNaN(date)) {
                        this.picker.select(date);
                        firstDate = this.picker.getSelectedDates()[0];
                        this.picker.cfg.setProperty("pagedate", (firstDate.getMonth() + 1) + "/" + firstDate.getFullYear());
                        render = true;
                    }
                }
                if (render) {
                    this.picker.render();
                }

                //align picker to date input's bottom-left corner
                this.positionRelativeTo(this.picker.oDomContainer, this.id);
                this.picker.show();
                this.picker.visible = true;
            },

            hide : function() {
                if (this.picker !== null && this.picker !== undefined) {
                    this.picker.hide();
                }
            },

            destroy : function() {
                YAHOO.util.Event.removeListener(this.config.icon, "click", this.show);
                if (this.picker !== null && this.picker !== undefined) {
                    this.picker.destroy();
                    this.picker = null;
                }
                try {
                    document.body.removeChild(this.container);
                } catch(ignore) {}

                this.config = null;
                this.id = null;
            },

            selectHandler : function(type, args, cal) {
                Dom.get(this.id).value = this._substituteDate(this.config.datePattern, args[0][0]);

                if (this.isVisible()) {
                    if (this.config.hideOnSelect) {
                        this.hide();
                    }
                    if (this.config.fireChangeEvent) {
                        var field = Dom.get(this.id), changeEvent;
                        if (field.onchange !== null && field.onchange !== undefined) {
                            field.onchange();
                        } else {
                            changeEvent = new Event('change');
                            field.dispatchEvent(changeEvent);
                        }
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
                var targetPos, targetHeight, subjectHeight, subjectWidth, viewportHeight, viewportWidth, scrollPos;

                targetPos = Dom.getXY(target);
                targetHeight = Dom.get(target).offsetHeight;
                subjectHeight = Dom.get(subject).offsetHeight;
                subjectWidth = Dom.get(subject).offsetWidth;

                viewportHeight =  Dom.getViewportHeight();
                viewportWidth = Dom.getViewportWidth();

                // also take scroll position into account
                scrollPos = [Dom.getDocumentScrollLeft(), Dom.getDocumentScrollTop()];

                // correct datepicker's position so that it isn't rendered off screen on the right side or bottom
                if (targetPos[0] + subjectWidth > scrollPos[0] + viewportWidth) {
                    // correct horizontal position
                    Dom.setX(subject, Math.max(targetPos[0], viewportWidth) - subjectWidth);
                } else {
                    Dom.setX(subject, targetPos[0]);
                }
                if (targetPos[1] + targetHeight + 1 + subjectHeight > scrollPos[1] + viewportHeight) {
                    // correct vertical position
                    Dom.setY(subject, Math.max(targetPos[1], viewportHeight) - subjectHeight);
                } else {
                    Dom.setY(subject, targetPos[1] + targetHeight + 1);
                }
            },

            isVisible : function() {
                return this.picker !== null && this.picker !== undefined && this.picker.visible;
            },

            /**
             * Parses date from simple date pattern. Only parses dates with yy, MM and dd like patterns, though
             * it is safe to have time as long as it comes after the pattern (which should be the case
             * anyway 99.9% of the time).
             */
            _parseDate : function(pattern, value) {
                var numbers, day, month, year, arrayPos, i, len, c, date;

                numbers = value.match(/(\d+)/g);
                if (numbers === null || numbers === undefined) {
                    return NaN;
                }

                arrayPos = 0;
                for (i = 0, len = pattern.length; i < len; i++) {
                    c = pattern.charAt(i);
                    while ((pattern.charAt(i) === c) && (i < pattern.length)) {
                        i++;
                    }
                    if (c === 'y') {
                        year = numbers[arrayPos++];
                    } else if (c === 'M') {
                        month = numbers[arrayPos++];
                    } else if (c === 'd') {
                        day = numbers[arrayPos++];
                    }
                    if (arrayPos > 2) {
                        break;
                    }
                }
                // TODO this is a bit crude. Make nicer some time.
                if (year < 100) {
                    if (year < 70) {
                        year = parseInt(year, 10) + 2000;
                    } else {
                        year = parseInt(year, 10) + 1900;
                    }
                }
                date = new Date();
                date.setFullYear(year, (month - 1), day);
                return date;
            },

            /**
             * Return the result of interpolating the value (date) argument with the date pattern.
             * The dateValue has to be an array, where year is in the first, month in the second
             * and date (day of month) in the third slot.
             */
            _substituteDate : function(datePattern, date) {
                var day, month, year;
                day = date[2];
                month = date[1];
                year = date[0];
                // optionally do some padding to match the pattern
                if(datePattern.match(/\bdd\b/)) {
                    day = this._padDateFragment(day);
                }
                if(datePattern.match(/\bMM\b/)) {
                    month = this._padDateFragment(month);
                }
                if(datePattern.match(/\byy\b/)) {
                    year = this._padDateFragment(year % 100);
                }
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
            },

            _localizeCalendar : function(calendar, lang) {
                var locale = {
                    en: function(cfg) {
                        // defaults to English
                    },
                    de: function(cfg) {
                        cfg.setProperty("MONTHS_LONG",    ["Januar", "Februar", "M\u00E4rz", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember"]);
                        cfg.setProperty("MONTHS_SHORT",   ["Jan", "Feb", "M\u00E4r", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez"]);
                        cfg.setProperty("WEEKDAYS_LONG",  ["Sonntag", "Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag"]);
                        cfg.setProperty("WEEKDAYS_MEDIUM",["Son", "Mon", "Die", "Mit", "Don", "Fre", "Sam"]);
                        cfg.setProperty("WEEKDAYS_SHORT", ["So", "Mo", "Di", "Mi", "Do", "Fr", "Sa"]);
                        cfg.setProperty("WEEKDAYS_1CHAR", ["S", "M", "D", "M", "D", "F", "S"]);
                        cfg.setProperty("START_WEEKDAY",  ["1"]);
                    },
                    nl: function(cfg) {
                        cfg.setProperty("MONTHS_LONG",    ["januari", "februari", "maart", "april", "mei", "juni", "juli", "augustus", "september", "october", "november", "december"]);
                        cfg.setProperty("MONTHS_SHORT",   ["jan", "feb", "maa", "apr", "mei", "jun", "jul", "aug", "sep", "oct", "nov", "dec"]);
                        cfg.setProperty("WEEKDAYS_LONG",  ["zondag", "maandag", "dinsdag", "woensdag", "donderdag", "vrijdag", "zaterdag"]);
                        cfg.setProperty("WEEKDAYS_MEDIUM",["zon", "maa", "din", "woe", "don", "vri", "zat"]);
                        cfg.setProperty("WEEKDAYS_SHORT", ["zo", "ma", "di", "wo", "do", "vr", "za"]);
                        cfg.setProperty("WEEKDAYS_1CHAR", ["z", "m", "d", "w", "d", "v", "z"]);
                        cfg.setProperty("START_WEEKDAY",  ["1"]);
                    },
                    fr: function(cfg) {
                        cfg.setProperty("MONTHS_LONG",    ["janvier", "f\u00E9vrier", "mars", "avril", "mai", "juin", "juillet", "ao\u00FBt", "septembre", "octobre", "novembre", "d\u00E9cembre"]);
                        cfg.setProperty("MONTHS_SHORT",   ["jan", "f\u00E9v", "mar", "avr", "mai", "jui", "jul", "ao\u00FB", "sep", "oct", "nov", "d\u00E9c"]);
                        cfg.setProperty("WEEKDAYS_LONG",  ["dimanche", "lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi"]);
                        cfg.setProperty("WEEKDAYS_MEDIUM",["dim", "lun", "mar", "mer", "jeu", "ven", "sam"]);
                        cfg.setProperty("WEEKDAYS_SHORT", ["di", "lu", "ma", "me", "je", "ve", "sa"]);
                        cfg.setProperty("WEEKDAYS_1CHAR", ["d", "l", "m", "m", "j", "v", "s"]);
                        cfg.setProperty("START_WEEKDAY",  ["1"]);
                    },
                    es: function(cfg) {
                        cfg.setProperty("MONTHS_LONG",    ["Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"]);
                        cfg.setProperty("MONTHS_SHORT",   ["Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"]);
                        cfg.setProperty("WEEKDAYS_LONG",  ["Domingo", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"]);
                        cfg.setProperty("WEEKDAYS_MEDIUM",["Dom", "Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"]);
                        cfg.setProperty("WEEKDAYS_SHORT", ["Do", "Lu", "Ma", "Mi", "Ju", "Vi", "Sá"]);
                        cfg.setProperty("WEEKDAYS_1CHAR", ["D", "L", "M", "X", "M", "V", "D"]);
                        cfg.setProperty("START_WEEKDAY",  ["1"]);
                    },
                    zh: function(cfg) {
                        cfg.setProperty("MONTHS_LONG",    ["一月份", "二月份", "三月份", "四月份", "五月份", "六月份", "七月份", "八月份", "九月份", "十月份", "十一月份", "十二月份"]);
                        cfg.setProperty("MONTHS_SHORT",   ["一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月", "十一月", "十二月"]);
                        cfg.setProperty("WEEKDAYS_LONG",  ["星期天", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"]);
                        cfg.setProperty("WEEKDAYS_MEDIUM",["周日", "周一", "周二", "周三", "周四", "周五", "周六"]);
                        cfg.setProperty("WEEKDAYS_SHORT", ["周日", "周一", "周二", "周三", "周四", "周五", "周六"]);
                        cfg.setProperty("WEEKDAYS_1CHAR", ["日", "一", "二", "三", "四", "五", "六"]);
                        cfg.setProperty("START_WEEKDAY",  ["1"]);
                    }
                }[lang];

                if (locale) {
                    locale.apply(null, [calendar.cfg]);
                }
            }
        };

    }());

    YAHOO.hippo.DateTime = new YAHOO.hippo.DateTimeImpl();

    YAHOO.register("DateTime", YAHOO.hippo.DateTime, {
        version: "2.8.1", build: "19"
    });
}
