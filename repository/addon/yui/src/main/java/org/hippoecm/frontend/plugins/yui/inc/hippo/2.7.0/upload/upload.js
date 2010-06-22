/**
 * @description
 * <p>
 * Provides a singleton upload helper
 * </p>
 * @namespace YAHOO.hippo
 * @requires yahoo, dom, hippoajax, uploader, datatable, button, ajaxindicator
 * @module upload
 * @beta
 */

YAHOO.namespace('hippo');

if (!YAHOO.hippo.Upload) {
    (function() {
        var Dom = YAHOO.util.Dom, Lang = YAHOO.lang;

        YAHOO.hippo.UploadImpl = function() {
            //YAHOO.widget.Uploader.SWFURL = "http://yui.yahooapis.com/2.8.1/build/uploader/assets/uploader.swf";
            this.latest = null;
            this.entries = new YAHOO.hippo.HashMap();
        };

        YAHOO.hippo.UploadImpl.prototype = {

            register : function(id, config) {
                if(!this.entries.containsKey(id)) {
                    this.entries.put(id, {id: id, config: config});
                }
            },

            render : function() {
                this.entries.forEach(this, function(k, v) {
                    var id = v.id;
                    var config = v.config;
                    var el = Dom.get(id);
                    if(el != null) {
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
                if(this.latest != null) {
                    this.latest.upload();
                }
            },

            removeItem : function(oRecord) {
                if(this.latest != null) {
                    this.latest.removeItem(oRecord);
                }
            }
        };

        YAHOO.hippo.UploadWidget = function(id, config) {
            this.id = id;
            this.config = config;

            if(this.config.ajaxIndicatorId != null) {
                this.indicator = new YAHOO.hippo.AjaxIndicator(this.config.ajaxIndicatorId);
            }

            YAHOO.widget.Uploader.SWFURL = config.flashUrl;

            var selectFilesLink = Dom.get('selectFilesLink');
            Dom.addClass(selectFilesLink, 'i18n-' + this.config.locale);

            var overlay = Dom.get('uploaderOverlay');
            Dom.setStyle(overlay, 'width', "244px");
            Dom.setStyle(overlay, 'height', "26px");

            this.uploader = new YAHOO.widget.Uploader( "uploaderOverlay" );
            this.fileList = null;
            this.numberOfUploads = 0;

            this.uploader.addListener('contentReady', this.handleContentReady, this, true);
            this.uploader.addListener('fileSelect', this.onFileSelect, this, true);
            this.uploader.addListener('uploadStart', this.onUploadStart, this, true);
            this.uploader.addListener('uploadProgress', this.onUploadProgress, this, true);
            this.uploader.addListener('uploadCancel', this.onUploadCancel, this, true);
            this.uploader.addListener('uploadComplete', this.onUploadComplete, this, true);
            this.uploader.addListener('uploadCompleteData', this.onUploadResponse, this, true);
            this.uploader.addListener('uploadError', this.onUploadError, this, true);
            this.uploader.addListener('rollOver', this.handleRollOver, this, true);
            this.uploader.addListener('rollOut', this.handleRollOut, this, true);
            this.uploader.addListener('click', this.handleClick, this, true);
            this.uploader.addListener('mouseDown', this.handleMouseDown, this, true);
            this.uploader.addListener('mouseUp', this.handleMouseUp, this, true);

            YAHOO.hippo.HippoAjax.registerDestroyFunction(Dom.get(this.id), this.destroy, this);
        };

        YAHOO.hippo.UploadWidget.prototype = {

            upload : function() {
                if(this.fileList != null) {
                    if(this.indicator != null) {
                        this.indicator.show();
                    }
                    this.uploader.uploadAll(this.config.uploadUrl);
                }
            },

            onFileSelect : function(event) {
                if('fileList' in event && event.fileList != null) {
	                  this.fileList = event.fileList;
    	              this._createDatatable(this.fileList);
                }
            },

            onUploadStart : function(event) {
                if(this.indicator != null) {
                    this.indicator.show();
                }
                this.numberOfUploads++;
            },

            onUploadProgress : function(event) {
                this._updateDatatable(event);
            },

            onUploadCancel : function() {
                this.numberOfUploads--;
                this.onAfterUpload();
            },

            onUploadComplete  : function(event) {
                this._updateDatatable(event);
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
                if(this.numberOfUploads == 0) {
                    if(this.indicator != null) {
                        this.indicator.hide();
                    }
                    
                    var url = this.config.callbackUrl + "&finished=true";
                    this.config.callbackFunction.call(this, url);
                }
            },

            handleContentReady  : function() {
                // Allows the uploader to send log messages to trace, as well as to YAHOO.log
                //this.uploader.setAllowLogging(true);

                // Allows multiple file selection in "Browse" dialog.
                this.uploader.setAllowMultipleFiles(this.config.allowMultipleFiles);
                this.uploader.setSimUploadLimit(this.config.simultaneousUploadLimit);

                if(this.config.fileExtensions != null && this.config.fileExtensions.length > 0) {
                    var allowedExtensions = '';
                    for(var i=0; i<this.config.fileExtensions.length; ++i) {
                        allowedExtensions += this.config.fileExtensions[i] + ';';
                    }
                    // Apply new set of file filters to the uploader.
                    this.uploader.setFileFilters(new Array({description:"Files", extensions:allowedExtensions}));
                }

  	            // Add the custom formatter to the shortcuts
      	        YAHOO.widget.DataTable.Formatter.titleFormatter = this._titleFormatter;
      	        YAHOO.widget.DataTable.Formatter.bytesFormatter = this._bytesFormatter;
                YAHOO.widget.DataTable.Formatter.removeFormatter = this._removeFormatter;
            },

            handleRollOver : function() {
                Dom.addClass(Dom.get('selectFilesLink'), 'rollover');
            },

            handleRollOut : function() {
                Dom.removeClass(Dom.get('selectFilesLink'), 'rollover');
            },

            handleClick : function() {

            },

            handleMouseDown : function() {

            },

            handleMouseUp : function() {

            },

            removeItem : function(oRecord) {
                this.uploader.removeFile(oRecord._oData.id);
                this.datatable.deleteRow(oRecord._sId);
            },

            _updateDatatable : function(event) {
                var recordSet = this.datatable.getRecordSet();
                for (var j = 0; j < recordSet.getLength(); j++) {
                    var r = recordSet.getRecord(j);
                    if(r._oData["id"] == event["id"]) {
                        var prog = Math.round(100*(event["bytesLoaded"]/event["bytesTotal"]));
                        var progbar = '<div class="yau-progressbar-container"><div class="yau-progressbar" style="width:' + prog + 'px;"></div></div>';
                        this.datatable.updateRow(j, {name: progbar, size: r._oData["size"]});
                    }
                }
            },

            _createDatatable : function(entries) {
                rowCounter = 0;
                this.fileIdHash = {};
                this.dataArr = [];
                for(var i in entries) {
                    var entry = entries[i];
                    entry["progress"] = '<div class="yau-progressbar-container"></div>';
                    this.dataArr.unshift(entry);
                }

//                for (var j = 0; j < this.dataArr.length; j++) {
//                    this.fileIdHash[this.dataArr[j].id] = j;
//                }

                var nameWidth = this.dataArr.length < 12 ? 305 : 305 - YAHOO.hippo.HippoAjax.getScrollbarWidth();

                var myColumnDefs = [
                    {key:"name", label: "File Name", sortable:true, width: nameWidth, formatter:"titleFormatter"},
                    {key:"size", label: "Size", sortable:true, width: 50, formatter: "bytesFormatter"},
                    {key:"id", label: "", sortable:false, width: 20, formatter: "removeFormatter"}
                    /*{key:"progress", label: "Upload progress", sortable:false, width: 100}*/
                ];

                var myDataSource = new YAHOO.util.DataSource(this.dataArr);
                myDataSource.responseType = YAHOO.util.DataSource.TYPE_JSARRAY;
                myDataSource.responseSchema = {
                  fields: ["id",{key:"name", parser:"string"},"created","modified","type", {key:"size", parser:"number"}, "progress"]
                };


                var sortedBy = {key:'name', dir: YAHOO.widget.DataTable.CLASS_ASC};
                if(this.datatable != null) {
                    var state = this.datatable.getState();
                    sortedBy.key = state.sortedBy.key;
                    sortedBy.dir = state.sortedBy.dir;
                }
                this.dataArr.sort(function(a, b) {
                    if(sortedBy.key == 'name') {
                        a = a.name.toLowerCase();
                        b = b.name.toLowerCase();
                    } else if(sortedBy.key == 'size') {
                        a = a.size;
                        b = b.size;
                    }
                    return ((a < b) ? -1 : ((a > b) ? 1 : 0));
                });
                if(sortedBy.dir == YAHOO.widget.DataTable.CLASS_DESC) {
                    this.dataArr.reverse();
                }
                if(this.dataArr.length > 12) {
                    this.datatable = new YAHOO.widget.ScrollingDataTable(
                        "dataTableContainer",
                        myColumnDefs, myDataSource, {
                        selectionMode:"single",
                        width: '440px',
                        height: '236px',
                        sortedBy: sortedBy
                    });
                } else {
                    this.datatable = new YAHOO.widget.DataTable(
                        "dataTableContainer",
                        myColumnDefs, myDataSource, {
                        selectionMode:"single",
                        sortedBy: sortedBy
                    });
                }
                this.datatable.subscribe("columnSortEvent", this._refreshFileIdHash, null, this);
                //this.singleSelectDataTable.hideColumn('progress');
            },

            _refreshFileIdHash : function() {
                var recordSet = this.datatable.getRecordSet();
                for (var j = 0; j < recordSet.getLength(); j++) {
                    var r = recordSet.getRecord(j);
                    this.fileIdHash[r._oData.id] = j;
                }
            },

            _titleFormatter : function(elLiner, oRecord, oColumn, oData) {
                elLiner.innerHTML = '<span title="' + oData + '">' + oData + '</span>';
            },

            _bytesFormatter : function(elLiner, oRecord, oColumn, oData) {
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

                    var n = number, c = isNaN(decimals = Math.abs(decimals)) ? 2 : decimals;
                    var d = dec_point == undefined ? "," : dec_point;
                    var t = thousands_sep == undefined ? "." : thousands_sep, s = n < 0 ? "-" : "";
                    var i = parseInt(n = Math.abs(+n || 0).toFixed(c)) + "", j = (j = i.length) > 3 ? j % 3 : 0;

                    return s + (j ? i.substr(0, j) + t : "") + i.substr(j).replace(/(\d{3})(?=\d)/g, "$1" + t) + (c ? d + Math.abs(n - i).toFixed(c).slice(2) : "");
                };
                var filesize = oData;
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
                elLiner.innerHTML = filesize;
            },

            _removeFormatter : function(elLiner, oRecord, oColumn, oData) {
                var el = document.createElement('div');
                Dom.addClass(el, 'remove_file');
                elLiner.appendChild(el);
                YAHOO.util.Event.addListener(el, "click", function() {
                    YAHOO.hippo.Upload.removeItem(oRecord);
                }, this);
            },

            destroy : function() {
                if(YAHOO.hippo.Upload.latest === this) {
                    YAHOO.hippo.Upload.latest = null;
                }

                if(this.uploader != null) {
                    this.uploader.destroy();
                    this.uploader = null;
                }

                if(this.indicator != null) {
                    this.indicator.hide();
                }

                this.config = null;
                this.id = null;
            }
        };
    })();

   YAHOO.hippo.Upload = new YAHOO.hippo.UploadImpl();

   YAHOO.register("upload", YAHOO.hippo.Upload, {
       version: "2.8.1", build: "19"
   });
}
