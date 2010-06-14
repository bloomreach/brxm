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
            }
        };

        YAHOO.hippo.UploadWidget = function(id, config) {
            this.id = id;
            this.config = config;

            if(this.config.ajaxIndicatorId != null) {
                this.indicator = new YAHOO.hippo.AjaxIndicator(this.config.ajaxIndicatorId);
            }

            YAHOO.widget.Uploader.SWFURL = config.flashUrl;

            var uiLayer = YAHOO.util.Dom.getRegion('selectLink');
            var overlay = YAHOO.util.Dom.get('uploaderOverlay');
            YAHOO.util.Dom.setStyle(overlay, 'width', uiLayer.right-uiLayer.left + "px");
            YAHOO.util.Dom.setStyle(overlay, 'height', uiLayer.bottom-uiLayer.top + "px");

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

            },

            handleRollOver : function() {
                Dom.replaceClass(Dom.get('selectLink'), 'yau-select-link-rollout', 'yau-select-link-rollover');
            },

            handleRollOut : function() {
                Dom.replaceClass(Dom.get('selectLink'), 'yau-select-link-rollover', 'yau-select-link-rollout');
            },

            handleClick : function() {

            },

            handleMouseDown : function() {

            },

            handleMouseUp : function() {

            },

            _updateDatatable : function(event) {
                var rowNum = this.fileIdHash[event["id"]];
                var prog = Math.round(100*(event["bytesLoaded"]/event["bytesTotal"]));
                var progbar = '<div class="yau-progressbar-container"><div class="yau-progressbar" style="width:' + prog + 'px;"></div></div>';
                this.singleSelectDataTable.updateRow(rowNum, {name: this.dataArr[rowNum]["name"], size: this.dataArr[rowNum]["size"], progress: progbar});
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

                for (var j = 0; j < this.dataArr.length; j++) {
                    this.fileIdHash[this.dataArr[j].id] = j;
                }

                var nameWidth = this.dataArr.length > 10 ? 222: 235;

                var myColumnDefs = [
                    {key:"name", label: "File Name", sortable:false, width: nameWidth},
                    {key:"size", label: "Size", sortable:false, width: 40},
                    {key:"progress", label: "Upload progress", sortable:false, width: 100}
                ];

                var myDataSource = new YAHOO.util.DataSource(this.dataArr);
                myDataSource.responseType = YAHOO.util.DataSource.TYPE_JSARRAY;
                myDataSource.responseSchema = {
                  fields: ["id","name","created","modified","type", "size", "progress"]
                };

                if(this.dataArr.length > 10) {
                    this.singleSelectDataTable = new YAHOO.widget.ScrollingDataTable(
                        "dataTableContainer",
                        myColumnDefs, myDataSource, {
                        selectionMode:"single",
                        width: '440px',
                        height: '216px'
                    });
                } else {
                    this.singleSelectDataTable = new YAHOO.widget.DataTable(
                        "dataTableContainer",
                        myColumnDefs, myDataSource, {
                        selectionMode:"single"
                    });
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
