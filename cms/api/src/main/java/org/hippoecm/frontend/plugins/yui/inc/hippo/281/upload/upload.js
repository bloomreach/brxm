/*
 * Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
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
 * Provides a singleton upload helper
 * </p>
 * @namespace YAHOO.hippo
 * @requires yahoo, dom, hippoajax, uploader, progressbar, datatable, datasource, button, ajaxindicator, hashmap
 * @module upload
 * @beta
 */

YAHOO.namespace('hippo');

if (!YAHOO.hippo.Upload) {
    (function() {
        "use strict";

        var Dom = YAHOO.util.Dom, Lang = YAHOO.lang;

        YAHOO.hippo.BytesToHumanReadable = function(filesize) {
            var number_format = function(number, decimals, dec_point, thousands_sep) {
                // http://kevin.vanzonneveld.net
                // +   original by: Jonas Raoni Soares Silva (http://www.jsfromhell.com)
                // +   improved by: Kevin van Zonneveld (http://kevin.vanzonneveld.net)
                // +     bugfix by: Michael White (http://crestidg.com)
                // +     bugfix by: Benjamin Lupton
                // +     bugfix by: Allan Jensen (http://www.winternet.no)
                // +    revised by: Jonas Raoni Soares Silva (http://www.jsfromhell.com)
                // *     example 1: number_format(1234.5678, 2, '.', '');
                // *     returns 1: 1234.57

                var n = number,
                    c = isNaN(decimals = Math.abs(decimals)) ? 2 : decimals,
                    d = (dec_point === null || dec_point === undefined) ? "," : dec_point,
                    t = (thousands_sep === null || thousands_sep === undefined) ? "." : thousands_sep,
                    s = n < 0 ? "-" : "",
                    i = String(parseInt(n = Math.abs(+n || 0).toFixed(c), 10)),
                    j = i.length > 3 ? i.length % 3 : 0;

                return s + (j ? i.substr(0, j) + t : "") + i.substr(j).replace(/(\d{3})(?=\d)/g, "$1" + t) + (c ? d + Math.abs(n - i).toFixed(c).slice(2) : "");
            };
            if (filesize >= 1073741824) {
                filesize = number_format(filesize / 1073741824, 2, '.', '') + ' Gb';
            } else {
                if (filesize >= 1048576) {
                    filesize = number_format(filesize / 1048576, 2, '.', '') + ' Mb';
                } else {
                    if (filesize >= 1024) {
                        filesize = number_format(filesize / 1024, 0) + ' Kb';
                    } else {
                        filesize = number_format(filesize, 0) + ' bytes';
                    }
                }
            }
            return filesize;
        };

        YAHOO.hippo.UploadImpl = function() {
            //YAHOO.widget.Uploader.SWFURL = "http://yui.yahooapis.com/2.8.1/build/uploader/assets/uploader.swf";
            this.latest = null;
            this.entries = new YAHOO.hippo.HashMap();
        };

        YAHOO.hippo.UploadImpl.prototype = {

            register : function(id, config) {
                if (!this.entries.containsKey(id)) {
                    this.entries.put(id, {id: id, config: config});
                }
            },

            render : function() {
                this.entries.forEach(this, function(k, v) {
                    var id = v.id,
                        config = v.config,
                        el = Dom.get(id);

                    if (Lang.isValue(el)) {
                        if (Lang.isUndefined(el.uploadWidget)) {
                            el.uploadWidget = new YAHOO.hippo.UploadWidget(id, config);
                        } else {
                            el.uploadWidget.update();
                        }
                        YAHOO.hippo.Upload.latest = el.uploadWidget;
                    } else {
                        YAHOO.log("Failed to render upload widget, element[" + id + "] not found", "error");
                    }
                });
                this.entries.clear();
            },

            upload : function() {
                if (Lang.isValue(this.latest)) {
                    this.latest.upload();
                }
            },

            stopIndicator : function() {
                if (Lang.isValue(this.latest)) {
                    this.latest.stopIndicator();
                }
            },

            restoreScrollPosition : function(posY) {
                if (Lang.isValue(this.latest)) {
                    this.latest.restoreScrollPosition(posY);
                }
            },

            removeItem : function(oRecord) {
                if (Lang.isValue(this.latest)) {
                    this.latest.removeItem(oRecord);
                }
            },

            hasFilesSelected: function() {
                if (Lang.isValue(this.latest)) {
                    return this.latest.hasFilesSelected();
                }
                return false;
            }
        };

        YAHOO.hippo.UploadWidget = function(id, config) {
            this.id = id;
            this.config = config;
            this.elements = {};
            this.scrollData = null;
            this.selectedFiles = [];
            this.progressBars = new YAHOO.hippo.HashMap();
            this.initializationAttempts = 0;
            this.scrollbarThreshold = 9;
            this.numberOfUploads = 0;
            YAHOO.widget.Uploader.SWFURL = config.flashUrl;

            if (Lang.isValue(this.config.ajaxIndicatorId)) {
                this.indicator = new YAHOO.hippo.AjaxIndicator(this.config.ajaxIndicatorId);
            }

            this.setupHtml();
            this.initializeUploader();

            YAHOO.hippo.HippoAjax.registerDestroyFunction(this.elements.selectFilesLink.get('element'), this.destroy, this);
        };

        YAHOO.hippo.UploadWidget.prototype = {

            setupHtml : function() {
                this.root = new YAHOO.util.Element(Dom.get(this.id));

                //Upload browse button
                this.elements.uiElements = new YAHOO.util.Element(document.createElement('div'));
                Dom.setStyle(this.elements.uiElements, 'display', 'inline');
                this.root.appendChild(this.elements.uiElements);

                if (this.config.alwaysShowLabel || this.config.maxNumberOfFiles > 1 || !this.config.uploadAfterSelect) {
                    //Upload information caption
                    this.elements.caption = new YAHOO.util.Element(document.createElement('div'));
                    Dom.addClass(this.elements.caption, 'uploadCaption');
                    this.elements.caption.get('element').innerHTML = this.config.translations['select.caption'];
                    this.root.appendChild(this.elements.caption);
                }

                this.elements.listCaption = new YAHOO.util.Element(document.createElement('div'));
                Dom.addClass(this.elements.listCaption, 'listCaption');
                this.elements.listCaption.get('element').innerHTML = this.config.translations['list.caption'];
                this.root.appendChild(this.elements.listCaption);
                Dom.setStyle(this.elements.listCaption, 'display', 'none');

                //datatable html container
                this.elements.datatableContainer = new YAHOO.util.Element(document.createElement('div'));
                Dom.addClass(this.elements.datatableContainer, 'dataTableContainer');
                this.root.appendChild(this.elements.datatableContainer);

                this.elements.uploaderContainer = new YAHOO.util.Element(document.createElement('div'));
                this.elements.uiElements.appendChild(this.elements.uploaderContainer);

                this.elements.uploaderOverlay = new YAHOO.util.Element(document.createElement('div'));
                Dom.addClass(this.elements.uploaderOverlay, 'browseOverlay');
                this.elements.uploaderContainer.appendChild(this.elements.uploaderOverlay);

                this.elements.selectFiles = new YAHOO.util.Element(document.createElement('div'));
                Dom.setStyle(this.elements.selectFiles, 'z-index', 1);
                this.elements.uploaderContainer.appendChild(this.elements.selectFiles);

                this.elements.selectFilesLink = new YAHOO.util.Element(document.createElement('div'));
                this.elements.selectFilesLink.get('element').innerHTML = this.config.translations['browse.caption'];

                Dom.addClass(this.elements.selectFilesLink, 'selectFilesLink');
                this.elements.selectFiles.appendChild(this.elements.selectFilesLink);

                Dom.setStyle(this.elements.uploaderOverlay, 'width',
                        this.config.buttonWidth === null || this.config.buttonWidth === ""  ? "155px" : this.config.buttonWidth);
                Dom.setStyle(this.elements.uploaderOverlay, 'height',
                        this.config.buttonHeight === null || this.config.buttonHeight === "" ? "26px" : this.config.buttonHeight);
            },

            initializeUploader : function() {
                this.uploader = new YAHOO.widget.Uploader(this.elements.uploaderOverlay);
                this.uploader.subscribe('contentReady', this.handleContentReady, this, true);
                this.uploader.subscribe('fileSelect', this.onFileSelect, this, true);
                this.uploader.subscribe('uploadStart', this.onUploadStart, this, true);
                this.uploader.subscribe('uploadProgress', this.onUploadProgress, this, true);
                this.uploader.subscribe('uploadCancel', this.onUploadCancel, this, true);
                this.uploader.subscribe('uploadComplete', this.onUploadComplete, this, true);
                this.uploader.subscribe('uploadCompleteData', this.onUploadResponse, this, true);
                this.uploader.subscribe('uploadError', this.onUploadError, this, true);
                this.uploader.subscribe('rollOver', this.handleRollOver, this, true);
                this.uploader.subscribe('rollOut', this.handleRollOut, this, true);
                this.uploader.subscribe('click', this.handleClick, this, true);
                this.uploader.subscribe('mouseDown', this.handleMouseDown, this, true);
                this.uploader.subscribe('mouseUp', this.handleMouseUp, this, true);
            },

            upload : function() {
                var sizes, el, offsetY, sc, pixFromBottom;

                if (this.selectedFiles.length > 0) {
                    if (Lang.isValue(this.indicator)) {
                        this.indicator.show();
                    }
                    this.uploader.uploadAll(this.config.uploadUrl);
                    this.numberOfUploads += this.selectedFiles.length;

                    if (Lang.isValue(this.layoutUnit) && this.layoutUnit.get('scroll') === true) {
                        //save scroll position
                        sizes = this.layoutUnit.getSizes();
                        el = this.layoutUnit.body;
                        offsetY = el.pageYOffset || el.scrollTop;
                        sc = offsetY + sizes.wrap.h;
                        pixFromBottom = el.scrollHeight - sc;
                        this.scrollData = '&scrollPosY=' + pixFromBottom;
                    }
                }
            },

            onFileSelect : function(event) {
                var files = [], max = this.config.maxNumberOfFiles, file, i, remove;
                if (Lang.hasOwnProperty(event, 'fileList') && Lang.isValue(event.fileList)) {
                    for (file in event.fileList) {
                        if (Lang.hasOwnProperty(event.fileList, file)) {
                            file = event.fileList[file];
                            file.progress = -1;
                            files.push(file);
                        }
                    }
                }

                if (files.length > 0) {
                    //There is no way to restrict the number of files that can be selected in the browse dialog so in
                    //order to comply to the maxNumberOfFiles property we have to remove any new items that are
                    //out of bounds
                    if ((files.length + this.selectedFiles.length) > max) {

                        //sort on id to ensure the splice call only removes the latest additions
                        files.sort(function(a, b) {
                            var iA, iB;
                            iA = parseInt(a.id.substr(4), 10);
                            iB = parseInt(b.id.substr(4), 10);
                            if (Lang.isNumber(iA) && Lang.isNumber(iB)) {
                                return (iA > iB) ? 1 : ((iB > iA) ? -1 : 0);
                            }
                            throw new Error('Could not compare items');
                        });
                        remove = files.splice(max - files.length, files.length - max);
                        for (i = 0; i < remove.length; i++) {
                            this.uploader.removeFile(remove[i].id);
                        }
                    }

                    if (files.length === max) {
                        this.disableBrowseButton();
                    }

                    this.selectedFiles = files;
                    this._renderDatatable();

                    if (this.config.uploadAfterSelect === true && files.length === max) {
                        this.upload();
                    }
                }
            },

            onUploadStart : function(event) {
                var record, id, nameWidth, pb;

                if (Lang.isValue(this.indicator)) {
                    this.indicator.show();
                }
                record = this._getRecordById(event.id);
                id = 'yui-progressbar-' + event.id;
                record.setData("progress", 0);
                this.datatable.updateCell(record, "name", record.getData("name"));
                this.datatable.updateCell(record, "id", record.getData("id"));

                nameWidth = this.datatable.getRecordSet().getLength() < 10 ?
                        305 : 305 - YAHOO.hippo.HippoAjax.getScrollbarWidth();
                pb = new YAHOO.widget.ProgressBar({
                    value: 1,
                    maxValue: 100,
                    width: nameWidth,
                    height: 12,
                    anim: true
                });
                pb.render(id);
                this.progressBars.put(event.id, pb);
            },

            onUploadProgress : function(event) {
                var id, record, prog, pb;

                id = event.id;
                record = this._getRecordById(id);
                prog = Math.round(100 * (event.bytesLoaded / event.bytesTotal));
                record.setData("progress", prog);
                if (this.progressBars.containsKey(id)) {
                    pb = this.progressBars.get(id);
                    pb.set('value', prog);
                }
            },

            onUploadCancel : function() {
                this.numberOfUploads--;
                this.onAfterUpload();
            },

            onUploadComplete  : function(event) {
                var id, record, pb;

                id = event.id;
                record = this._getRecordById(id);
                record.setData("progress", 100);
                if (this.progressBars.containsKey(id)) {
                    pb = this.progressBars.get(id);
                    pb.set('value', 100);
                }
                this.datatable.updateCell(record, "name", record.getData("name"));
                this.numberOfUploads--;
                this.onAfterUpload();
            },

            onUploadResponse : function() {
            },

            onUploadError : function(event) {
                this.numberOfUploads--;
                this.onAfterUpload();
            },

            onAfterUpload : function() {
                var url;

                if (this.numberOfUploads === 0) {
                    if (Lang.isValue(this.indicator)) {
                        this.indicator.hide();
                    }

                    this.enableBrowseButton();

                    if (this.config.clearAfterUpload === true) {
                        this.clearAfterUpload();
                    }

                    url = this.config.callbackUrl + "&finished=true";
                    if (Lang.isValue(this.scrollData)) {
                        url = url + this.scrollData;
                        this.scrollData = null;
                    }
                    this.config.callbackFunction.call(this, url);
                }
            },

            restoreScrollPosition : function(posY) {
                if (this.layoutUnit !== null && this.layoutUnit.get('scroll') === true) {
                    YAHOO.lang.later(200, this, function() {
                        var el = this.layoutUnit.body,
                            a = el.scrollTop,
                            b = (el.scrollHeight - el.offsetHeight) + posY,
                            attributes = {
                                scroll: { to: [a, b] }
                            };

                        new YAHOO.util.Scroll(el, attributes, 0).animate();
                    });
                }
            },

            stopIndicator : function() {
                if (Lang.isValue(this.indicator)) {
                    this.indicator.hide();
                }
            },

            clearAfterUpload : function() {
                YAHOO.lang.later(this.config.clearTimeout, this, '_removeDatatable');
            },

            handleContentReady  : function() {
                var allowedExtensions, i, len, ext, self;

                // CMS7-5086 the flash object javascript callbacks are not available at the moment
                // the swfReady event is fired from the flash object.
                if (this.uploader._swf.setAllowMultipleFiles !== undefined) {
                    // Allows multiple file selection in "Browse" dialog.
                    this.uploader.setAllowMultipleFiles(this.config.allowMultipleFiles);
                    this.uploader.setSimUploadLimit(this.config.simultaneousUploadLimit);

                    if (this.config.fileExtensions !== null && this.config.fileExtensions.length > 0) {
                        allowedExtensions = '';
                        for (i = 0, len = this.config.fileExtensions.length; i < len; ++i) {
                            ext = this.config.fileExtensions[i];
                            if (ext.indexOf("*.") !== 0) {
                                ext = "*." + ext;
                            }
                            allowedExtensions += ext + ';';
                        }
                        // Apply new set of file filters to the uploader.
                        this.uploader.setFileFilters([{description: "Files", extensions: allowedExtensions}]);
                    }

                    // Add the custom formatter to the shortcuts
                    YAHOO.widget.DataTable.Formatter.titleFormatter = this._titleFormatter;
                    YAHOO.widget.DataTable.Formatter.bytesFormatter = this._bytesFormatter;
                    YAHOO.widget.DataTable.Formatter.removeFormatter = this._removeFormatter;

                    this.layoutUnit = YAHOO.hippo.LayoutManager.findLayoutUnit(Dom.get(this.id));
                } else if (this.initializationAttempts++ < 20) {
                    window.setTimeout(function() {
                        this.uploader.destroy();
                        this.initializeUploader();
                    }.bind(this), 100);
                }
            },

            handleRollOver : function() {
                Dom.addClass(this.elements.selectFilesLink, 'rollover');
            },

            handleRollOut : function() {
                Dom.removeClass(this.elements.selectFilesLink, 'rollover');
            },

            handleClick : function() {

            },

            handleMouseDown : function() {

            },

            handleMouseUp : function() {

            },

            enableBrowseButton : function() {
                this.uploader.enable();
                Dom.removeClass(this.elements.selectFilesLink, 'disabled');
            },

            disableBrowseButton : function() {
                this.handleRollOut();
                this.uploader.disable();
                Dom.addClass(this.elements.selectFilesLink, 'disabled');
            },

            removeItem : function(oRecord) {
                this.uploader.removeFile(oRecord._oData.id);
                this.datatable.deleteRow(oRecord._sId);
                this.removeFromSelectedFiles(oRecord._oData.id);

                if (this.datatable.getRecordSet().getLength() === 0) {
                    this._removeDatatable();
                } else if (this.datatable.getRecordSet().getLength() === this.scrollbarThreshold) {
                    this._redrawDatatable();
                }

                this.enableBrowseButton();
            },

            removeFromSelectedFiles : function(id) {
                var i, len;
                for (i = 0, len = this.selectedFiles.length; i < len; i++) {
                    if (this.selectedFiles[i].id === id) {
                        this.selectedFiles.splice(i, 1);
                        break;
                    }
                }
            },

            hasFilesSelected : function() {
                if (!Lang.isUndefined(this.datatable) && this.datatable !== null) {
                    return this.datatable.getRecordSet().getLength() > 0;
                }
                return false;
            },

            _getRecordById : function(id) {
                var recordSet, i, len, r;
                recordSet = this.datatable.getRecordSet();
                for (i = 0, len = recordSet.getLength(); i < len; i++) {
                    r = recordSet.getRecord(i);
                    if (r._oData.id === id) {
                        return r;
                    }
                }
                return null;
            },

            _renderDatatable : function() {
                if (!Lang.isValue(this.datatable) ||
                        (this.datatable.getRecordSet().getLength() <= this.scrollbarThreshold &&
                        this.selectedFiles.length > this.scrollbarThreshold)) {
                    this._createDatatable();
                } else {
                    this.datatable.load({
                        datasource : this._createDataSource(this.datatable.getState().sortedBy)
                    });
                }
            },

            _createDataSource : function(sortedBy) {
                if (!Lang.isValue(sortedBy)) {
                    throw new Error('Argument sortedBy is required.');
                }

                this.selectedFiles.sort(function(a, b) {
                    if (sortedBy.key === 'name') {
                        a = a.name.toLowerCase();
                        b = b.name.toLowerCase();
                    } else if (sortedBy.key === 'size') {
                        a = a.size;
                        b = b.size;
                    }
                    return ((a < b) ? -1 : ((a > b) ? 1 : 0));
                });

                if (sortedBy.dir === YAHOO.widget.DataTable.CLASS_DESC) {
                    this.selectedFiles.reverse();
                }

                return new YAHOO.util.LocalDataSource(this.selectedFiles, {
                    responseSchema : {
                        fields: [
                            "id",
                            {key: "name", parser: "string"},
                            "created",
                            "modified",
                            "type",
                            {key: "size", parser: "number"},
                            "progress"
                        ]
                    }
                });
            },

            _createDatatable : function(sortedBy) {
                if (this.selectedFiles.length === 0) {
                    return;
                }

                Dom.setStyle(this.elements.listCaption, 'display', 'block');

                if (sortedBy === undefined) {
                    if (Lang.isValue(this.datatable)) {
                        sortedBy = this.datatable.getState().sortedBy;
                    } else {
                        sortedBy = {key: 'name', dir: YAHOO.widget.DataTable.CLASS_ASC};
                    }
                }

                if (this.selectedFiles.length > this.scrollbarThreshold) {
                    this.datatable = new YAHOO.widget.ScrollingDataTable(
                        this.elements.datatableContainer,
                        this._createColumnDefinitions(),
                        this._createDataSource(sortedBy),
                        {
                            selectionMode: "single",
                            width: '440px',
                            height: '199px',
                            sortedBy: sortedBy,
                            MSG_EMPTY: ''
                        }
                    );
                } else {
                    this.datatable = new YAHOO.widget.DataTable(
                        this.elements.datatableContainer,
                        this._createColumnDefinitions(),
                        this._createDataSource(sortedBy),
                        {
                            selectionMode: "single",
                            sortedBy: sortedBy,
                            MSG_EMPTY: ''
                        }
                    );
                }
            },

            _createColumnDefinitions : function() {
                var nameWidth = 305;
                if (this.selectedFiles.length > this.scrollbarThreshold) {
                    nameWidth -= YAHOO.hippo.HippoAjax.getScrollbarWidth();
                }

                return [
                    {key: "name", label: "File Name", sortable: true, width: nameWidth, formatter: "titleFormatter"},
                    {key: "size", label: "Size", sortable: true, width: 50, formatter: "bytesFormatter"},
                    {key: "id", label: "", sortable: false, width: 20, formatter: "removeFormatter"}
                ];
            },

            _redrawDatatable : function() {
                if (Lang.isValue(this.datatable)) {
                    var sortedBy = this.datatable.getState().sortedBy;
                    this._removeDatatable();
                    this._createDatatable(sortedBy);
                }
            },

            _removeDatatable : function() {
                if (Lang.isValue(this.datatable)) {
                    this.datatable.destroy();
                }
                this.datatable = null;
                Dom.setStyle(this.elements.listCaption, 'display', 'none');
            },

            _titleFormatter : function(elLiner, oRecord, oColumn, oData) {
                if (oRecord._oData.progress === 100) {
                    Dom.addClass(elLiner, 'finished');
                    elLiner.innerHTML = '<span style="font-weight: bold;">OK: </span><span title="' + oData + '">' + oData + '</span>';
                } else if (oRecord._oData.progress > -1) {
                    elLiner.innerHTML = '<div id="yui-progressbar-' + oRecord._oData.id + '"/>';// + oData + '</div';
                } else {
                    elLiner.innerHTML = '<span title="' + oData + '">' + oData + '</span>';
                }
            },

            _bytesFormatter : function(elLiner, oRecord, oColumn, oData) {
                elLiner.innerHTML = YAHOO.hippo.BytesToHumanReadable(oData);
            },

            _removeFormatter : function(elLiner, oRecord, oColumn, oData) {
                if (oRecord._oData.progress > -1) {
                    elLiner.innerHTML = '';
                } else {
                    var el = document.createElement('div');
                    Dom.addClass(el, 'remove_file');
                    elLiner.appendChild(el);
                    YAHOO.util.Event.addListener(el, "click", function() {
                        YAHOO.hippo.Upload.removeItem(oRecord);
                    }, this);
                }
            },

            destroy : function() {
                if (YAHOO.hippo.Upload.latest === this) {
                    YAHOO.hippo.Upload.latest = null;
                }

                if (Lang.isValue(this.uploader)) {
                    this.uploader.destroy();
                    this.uploader = null;
                }

                if (Lang.isValue(this.indicator)) {
                    this.indicator.hide();
                }

                this._removeDatatable();

                this.progressBars.forEach(this, function(k, v) {
                    v.destroy();
                });
                this.progressBars.clear();

                this.config = null;
                this.id = null;
            }
        };
    }());

    YAHOO.hippo.Upload = new YAHOO.hippo.UploadImpl();

    YAHOO.register("upload", YAHOO.hippo.Upload, {
        version: "2.8.1",
        build: "19"
    });
}
