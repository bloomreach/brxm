app.controller('contentBlocksCtrl', function ($scope, $sce, $log, $rootScope, $http, MyHttpInterceptor) {

    // TODO fetch columns
    $scope.columns = {"items": [
        {"key": "Left column", "value": "left"},
        {"key": "Right column", "value": "right"}
    ]};
    $scope.welcomeMessage = "Content blocks plugin";
    $scope.pluginClass = "org.onehippo.cms7.essentials.dashboard.contentblocks.ContentBlocksPlugin";
    $scope.pluginInstalled = true;
    $scope.payload = {"cbpayload": {"items": {"items": []}}};
    $scope.selection = [];
    $scope.providerInput = "";
    $scope.selectedItem = [];
    $scope.test = [
        {"key": "1f2f3r4g", "value": "mydemoessentials:1f2f3r4g", "path": "/hippo:namespaces/mydemoessentials/1f2f3r4g"},
        {"key": 23131232143324, "value": "mydemoessentials:23131232143324", "path": "/hippo:namespaces/mydemoessentials/23131232143324"},
        {"key": "dfgdfgdfgdfgdfgdfgdfgdfg", "value": "mydemoessentials:dfgdfgdfgdfgdfgdfgdfgdfg", "path": "/hippo:namespaces/mydemoessentials/dfgdfgdfgdfgdfgdfgdfgdfg"}
    ];
    $scope.documentTypes = [
        {"key": "News document", "value": "namespace:news", "providers": {"items": [
            // {"key": "Provider 1", "value": "hippogogreen:testprov", "path": "hippogogreen/testprov"},
            {"key": "provider 2", "value": "provider:2", "path": "hippogogreen/testprov"}    ,
            {"key": "Provider 1", "value": "hippogogreen:testprov", "path": "hippogogreen/testprov"}
        ]}},
        {"key": "Events document", "value": "namespace:events", "providers": {"items": [
            {"key": "provider 2", "value": "provider:1"}
        ]}}
    ];
    $scope.providers = [
        {"key": "Provider 1", "value": "hippogogreen:testprov", "path": "hippogogreen/testprov"},
        {"key": "Provider 2", "value": "hippogogreen:testprov", "path": "hippogogreen/testprov"},
        {"key": "Provider 3", "value": "hippogogreen:testprov", "path": "hippogogreen/testprov"}
    ];

    $scope.baseCmsNamespaceUrl = "http://localhost:8080/cms?path=";
    $scope.baseConsoleNamespaceUrl = "http://localhost:8080/cms/console?path=";


    $scope.selectChange = function (docName, selectedItem) {
        $log.info(docName, selectedItem);
    };
    $scope.onDelete = function (docName) {
        var index = $scope.providers.indexOf(docName)
        $scope.providers.splice(index, 1);
        $log.info(docName);
    };
    $scope.onAdd = function (docName) {
        $log.info(docName);
       // $scope.providers.push({"key": docName});
        $scope.providerInput = "";

        //$scope.documentTypes[1].providers.items.push({"key": "Provider 1", "value": "hippogogreen:testprov", "path": "hippogogreen/testprov"});

        // TODO: put providers
        $http({
            method: 'PUT',
            url: $rootScope.REST.compoundsCreate + docName,
            data: docName
        }).success(function (data) {
                    $scope.providers.push(data);
                    //$log.info(data);
                    //$scope.documentTypes.providers = [];

                });
    };
    $scope.addProviderToDocType = function (prov, docName) {
        var index = $scope.documentTypes.indexOf(docName)
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
//    $scope.saveBlocksConfiguration = function () {
//        $http({
//            method: 'POST',
//            url: $rootScope.REST.contentblocksCreate,
//            data: $scope.documentTypes
//        }).success(function (data) {
//                    $log.info(data);
//                    //$scope.documentTypes.providers = [];
//
//                });
//    };
    $scope.installPlugin = function () {
        $log.info("installing plugin");
        $http({
            method: 'GET',
            url: $rootScope.REST.pluginInstall + $scope.pluginClass
        }).success(function (data) {

                    $scope.installMessage = data.value;
                });
    };

    $scope.saveBlocksConfiguration = function () {
        $log.info("Saving configuration for:");
        $scope.payload = {"cbpayload": {"items": {"items": []}}};
        $scope.payload.cbpayload.items.items = $scope.documentTypes
        $log.info($scope.documentTypes);
        $log.info($scope.payload);
        $http({
            method: 'POST',
            url: $rootScope.REST.contentblocksCreate,
            data: $scope.payload
        }).success(function (data) {
                    $log.info(data);
                    //$scope.documentTypes.providers = [];

                });
        $log.info("Saved");
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
            url: $rootScope.REST.documentTypes
        }).success(function (data) {
                    $scope.documentTypes = data.items;
                    $log.info('document types loaded');
                    $log.info($scope.documentTypes);

                });

        $http({
            method: 'GET',
            url: $rootScope.REST.compounds
        }).success(function (data) {
                    $scope.providers = data.items;

                });


    };


    $scope.init();

});