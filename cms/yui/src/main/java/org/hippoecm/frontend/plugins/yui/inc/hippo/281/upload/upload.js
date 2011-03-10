/**
 * @description
 * <p>
 * Provides a singleton upload helper
 * </p>
 * @namespace YAHOO.hippo
 * @requires yahoo, dom, hippoajax, uploader, progressbar, datatable, button, ajaxindicator, hashmap
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

            restoreScrollPosition : function(posY) {
                if(this.latest != null) {
                    this.latest.restoreScrollPosition(posY);
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
            this.scrollData = null;
            this.progressBars = new YAHOO.hippo.HashMap();
            this.elements = {uiElements: null};

            if(this.config.ajaxIndicatorId != null) {
                this.indicator = new YAHOO.hippo.AjaxIndicator(this.config.ajaxIndicatorId);
            }

            YAHOO.widget.Uploader.SWFURL = config.flashUrl;

            var root = new YAHOO.util.Element(Dom.get(id));
            this.elements.uiElements = new YAHOO.util.Element(document.createElement('div'));
            Dom.setStyle(this.elements.uiElements, 'display', 'inline');
            root.appendChild(this.elements.uiElements);

            this.elements.datatableContainer = new YAHOO.util.Element(document.createElement('div'));
            Dom.addClass(this.elements.datatableContainer, 'dataTableContainer');
            root.appendChild(this.elements.datatableContainer);

            this.elements.uploaderContainer = new YAHOO.util.Element(document.createElement('div'));
            this.elements.uiElements.appendChild(this.elements.uploaderContainer);

            this.elements.uploaderOverlay = new YAHOO.util.Element(document.createElement('div'));
            Dom.setStyle(this.elements.uploaderOverlay, 'position', 'absolute');
            Dom.setStyle(this.elements.uploaderOverlay, 'z-index', 2);
            this.elements.uploaderContainer.appendChild(this.elements.uploaderOverlay);

            this.elements.selectFiles = new YAHOO.util.Element(document.createElement('div'));
            Dom.setStyle(this.elements.selectFiles, 'z-index', 1);
            this.elements.uploaderContainer.appendChild(this.elements.selectFiles);

            var link = document.createElement('div');
            link.innerHTML = this.config.translations['select.files.link'];
            this.elements.selectFilesLink = new YAHOO.util.Element(link);
            Dom.addClass(this.elements.selectFilesLink, 'selectFilesLink');
            this.elements.selectFiles.appendChild(this.elements.selectFilesLink);

            Dom.setStyle(this.elements.uploaderOverlay, 'width', config.buttonWidth);
            Dom.setStyle(this.elements.uploaderOverlay, 'height', "26px");

            this.uploader = new YAHOO.widget.Uploader(this.elements.uploaderOverlay);
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

            YAHOO.hippo.HippoAjax.registerDestroyFunction(root, this.destroy, this);
        };

        YAHOO.hippo.UploadWidget.prototype = {

            upload : function() {
                if(this.fileList != null) {
                    if(this.config.hideBrowseDuringUpload) {
                        //Dom.setStyle(this.elements.uploaderContainer, 'display', 'none');
                    }
                    if(this.indicator != null) {
                        this.indicator.show();
                    }
                    this.uploader.uploadAll(this.config.uploadUrl);
                    this.numberOfUploads += this.dataArr.length;

                    if(this.layoutUnit != null && this.layoutUnit.get('scroll') === true) {
                        //save scroll position
                        var sizes = this.layoutUnit.getSizes();
                        var el = this.layoutUnit.body;
                        var offsetY = el.pageYOffset || el.scrollTop;
                        var sc = offsetY + sizes.wrap.h;
                        var pixFromBottom = el.scrollHeight - sc;
                        this.scrollData = '&scrollPosY=' + pixFromBottom;
                    }

                }
            },

            onFileSelect : function(event) {
                if('fileList' in event && event.fileList != null) {
	                  this.fileList = event.fileList;
    	              this._createDatatable(this.fileList);
                }
                if(this.config.uploadAfterSelect === true) {
                    this.upload();
                }
            },

            onUploadStart : function(event) {
                if(this.indicator != null) {
                    this.indicator.show();
                }
                var record = this._getRecordById(event["id"]);
                var id = 'yui-progressbar-' + event["id"];
                record.setData("progress", 0);
                this.datatable.updateCell(record, "name", record.getData("name"));
                this.datatable.updateCell(record, "id", record.getData("id"));

                var nameWidth = this.datatable.getRecordSet().getLength() < 10 ? 305 : 305 - YAHOO.hippo.HippoAjax.getScrollbarWidth();
                var pb = new YAHOO.widget.ProgressBar({value:1, maxValue: 100, width: nameWidth, height: 12, anim: true});
                pb.render(id);
                this.progressBars.put(event["id"], pb);
            },

            onUploadProgress : function(event) {
                var id = event["id"];
                var record = this._getRecordById(id);
                var prog = Math.round(100*(event["bytesLoaded"]/event["bytesTotal"]));
                record.setData("progress", prog);
                if(this.progressBars.containsKey(id)) {
                    var pb = this.progressBars.get(id);
                    pb.set('value', prog);
                }
            },

            onUploadCancel : function() {
                this.numberOfUploads--;
                this.onAfterUpload();
            },

            onUploadComplete  : function(event) {
                var id = event["id"];
                var record = this._getRecordById(id);
                record.setData("progress", 100);
                if(this.progressBars.containsKey(id)) {
                    var pb = this.progressBars.get(id);
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
                if(this.numberOfUploads == 0) {
                    if(this.indicator != null) {
                        this.indicator.hide();
                    }

                    if(this.config.clearAfterUpload === true) {
                        this.clearAfterUpload();
                    }
                    if(this.config.hideBrowseDuringUpload === true) {
                        //Dom.setStyle(this.elements.uploaderContainer, 'display', 'block');
                    }

                    var url = this.config.callbackUrl + "&finished=true";
                    if(this.scrollData != null) {
                        url = url + this.scrollData;
                        this.scrollData = null;
                    }
                    this.config.callbackFunction.call(this, url);
                }
            },

            restoreScrollPosition : function(posY) {
                var el = Dom.get(this.id);
                /*if(Lang.isFunction(el.scrollIntoView)) {

                    YAHOO.lang.later(200, this, function() {
                        el.scrollIntoView();
                    });
                } else {*/
                    if(this.layoutUnit != null && this.layoutUnit.get('scroll') === true) {
                        YAHOO.lang.later(200, this, function() {
                            var el = this.layoutUnit.body;
                            var sh = (el.scrollHeight - el.offsetHeight) + posY;
                             var attributes = {
	                              scroll: { to: [el.scrollTop, sh] }
                             };
                              var anim = new YAHOO.util.Scroll(el, attributes, 0);
                            anim.animate();
                        });
                    }
                /*}*/
            },

            clearAfterUpload : function() {
                YAHOO.lang.later(this.config.clearTimeout, this, '_removeDatatable');
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

                this.layoutUnit = YAHOO.hippo.LayoutManager.findLayoutUnit(Dom.get(this.id));
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

            removeItem : function(oRecord) {
                this.uploader.removeFile(oRecord._oData.id);
                this.datatable.deleteRow(oRecord._sId);
            },

            _getRecordById : function(id) {
                var recordSet = this.datatable.getRecordSet();
                for (var j = 0; j < recordSet.getLength(); j++) {
                    var r = recordSet.getRecord(j);
                    if(r._oData["id"] == id) {
                        return r;
                    }
                }
                return null;
            },

            _createDatatable : function(entries) {
                this.dataArr = [];
                for(var i in entries) {
                    var entry = entries[i];
                    entry["progress"] = -1;
                    this.dataArr.unshift(entry);
                }

                var nameWidth = 305;
                if(this.dataArr.length > 10) {
                    nameWidth -= YAHOO.hippo.HippoAjax.getScrollbarWidth();
                }

                var myColumnDefs = [
                    {key:"name", label: "File Name", sortable:true, width: nameWidth, formatter:"titleFormatter"},
                    {key:"size", label: "Size", sortable:true, width: 50, formatter: "bytesFormatter"},
                    {key:"id", label: "", sortable:false, width: 20, formatter: "removeFormatter"}
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
                if(this.dataArr.length > 10) {
                    this.datatable = new YAHOO.widget.ScrollingDataTable(
                        this.elements.datatableContainer,
                        myColumnDefs, myDataSource, {
                        selectionMode:"single",
                        width: '440px',
                        height: '236px',
                        sortedBy: sortedBy
                    });
                } else {
                    this.datatable = new YAHOO.widget.DataTable(
                        this.elements.datatableContainer,
                        myColumnDefs, myDataSource, {
                        selectionMode:"single",
                        sortedBy: sortedBy
                    });
                }
            },

            _removeDatatable : function() {
                this.datatable.destroy();
                this.datatable = null;
            },

            _titleFormatter : function(elLiner, oRecord, oColumn, oData) {
                if(oRecord._oData.progress == 100) {
                    Dom.addClass(elLiner, 'finished');
                    elLiner.innerHTML = '<span style="font-weight: bold;">OK: </span><span title="' + oData + '">' + oData + '</span>';
                }else if(oRecord._oData.progress > -1) {
                    elLiner.innerHTML = '<div id="yui-progressbar-' + oRecord._oData.id + '"/>';// + oData + '</div';
                } else {
                    elLiner.innerHTML = '<span title="' + oData + '">' + oData + '</span>';
                }
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
                if(oRecord._oData.progress > -1) {
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
                if(this.datatable != null) {
                    this.datatable.destroy();
                }

                this.progressBars.forEach(this, function(k, v) {
                    v.destroy()
                });
                this.progressBars.clear();

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
