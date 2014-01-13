app.controller('contentBlocksCtrl', function ($scope, $sce, $log, $rootScope, $http, MyHttpInterceptor) {

    // TODO fetch columns
    $scope.columns = {"items": [
        {"key": "Left column", "value": "left"},
        {"key": "Right column", "value": "right"}
    ]};
    $scope.deliberatelyTrustDangerousSnippet = function () {
        return $sce.trustAsHtml('<a target="_blank" href="http://content-blocks.forge.onehippo.org">Detailed documentation</a>');
    };
    $scope.introMessage = "Content Blocks plugin provides the content/document editor an ability to add multiple pre-configured compound type blocks to a document. You can configure the available content blocks on per document type basis.";
    $scope.pluginClass = "org.onehippo.cms7.essentials.dashboard.contentblocks.ContentBlocksPlugin";
    $scope.pluginInstalled = true;
    $scope.payload = {"cbpayload": {"items": {"items": []}}};
    $scope.selection = [];
    $scope.providerInput = "";
    $scope.selectedItem = [];
    $scope.providers = [];
    $scope.baseCmsNamespaceUrl = "http://localhost:8080/cms?path=";
    $scope.baseConsoleNamespaceUrl = "http://localhost:8080/cms/console?path=";
    $scope.map = {};

    $scope.selectChange = function () {
        angular.forEach($scope.documentTypes, function (docType, key) {
            docType.providers.items = [];
            angular.forEach(docType.providers.ritems, function (providerItem, key) {
                $log.info($scope.map[providerItem.key]);
                $log.info(docType.providers.items.push($scope.map[providerItem.key]));
            });
        });
    };
    $scope.onDelete = function (docName) {
        var index = $scope.providers.indexOf(docName);
        $scope.providers.splice(index, 1);
    };
    $scope.onAdd = function (docName) {
        $scope.providerInput = "";
        // TODO: put providers
        $http({
            method: 'PUT',
            url: $rootScope.REST.compoundsCreate + docName,
            data: docName
        }).success(function (data) {
            if(!$scope.providers){
                $scope.providers = [];
            }
                    $scope.providers.push(data);

                });
    };
    $scope.addProviderToDocType = function (prov, docName) {
        var index = $scope.documentTypes.indexOf(docName);
        //check if is empty
        if ($scope.documentTypes[index].providers == "") {
            $scope.documentTypes[index].providers = {"items": []};
        }
        $scope.documentTypes[index].providers.items.push(prov);

    };
    $scope.removeProviderFromDocType = function (prov, docName) {
        var index = $scope.documentTypes.indexOf(docName)
        var providers = $scope.documentTypes[index].providers.items
        var providerIndex = providers.indexOf(prov);
        $scope.documentTypes[index].providers.items.splice(providerIndex, 1);
    };
    $scope.installPlugin = function () {
        $http({
            method: 'GET',
            url: $rootScope.REST.pluginInstall + $scope.pluginClass
        }).success(function (data) {

                    $scope.installMessage = data.value;
                });
    };

    $scope.saveBlocksConfiguration = function () {
        $scope.payload = {"cbpayload": {"items": {"items": []}}};
        $scope.payload.cbpayload.items.items = $scope.documentTypes;

        $http({
            method: 'POST',
            url: $rootScope.REST.contentblocksCreate,
            data: $scope.payload
        }).success(function (data) {
                    // TODO on prviders
                    //$scope.documentTypes.providers = [];

                });
    };

    $scope.toggleCheckBox = function (docName) {
        var index = $scope.selection.indexOf(docName);
        // check if  selected
        if (index > -1) {
            $scope.selection.splice(index, 1);
        }
        else {
            $scope.selection.push(docName);
        }
    };
    $scope.init = function () {
        // check if plugin is installed
        $http({
            method: 'GET',
            url: $rootScope.REST.pluginInstallState + $scope.pluginClass
        }).success(function (data) {
                    //{"installed":false,"pluginLink":"contentBlocks","title":"Content Blocks Plugin"}
                    // TODO enable check:
                    $scope.pluginInstalled = true;
                    //$scope.pluginInstalled = data.installed;

                });

        // TODO: fetch docTypes
        $http({
            method: 'GET',
            url: $rootScope.REST.compounds
        }).success(function (data) {
                    $scope.providers = data.items;
                    angular.forEach($scope.providers, function (provider, key) {
                        $scope.map[provider.key] = provider;
                    });
                });
        $http({
            method: 'GET',
            url: $rootScope.REST.documentTypes
        }).success(function (data) {
                    $scope.documentTypes = data.items;

                    angular.forEach($scope.documentTypes, function (docType, key) {
                        docType.providers.ritems = [];
                        /*angular.forEach(docType.providers.items, function (providerItem, key) {
                            $log.info($scope.map[providerItem.key]);
                            $log.info(docType.providers.ritems.push($scope.map[providerItem.key]));
                        });*/
                    });



                });


    };


    $scope.init();

});