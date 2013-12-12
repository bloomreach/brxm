app.controller('contentBlocksCtrl', function ($scope, $sce, $log, $rootScope, $http, MyHttpInterceptor) {

    // TODO fetch columns
    $scope.columns = {"items": [
        {"key": "Left column", "value": "left"},
        {"key": "Right column", "value": "right"}
    ]};
    $scope.welcomeMessage = "Content blocks plugin";
    $scope.welcomeMessage2 = "Content blocks plugin";
    $scope.selection = [];
    $scope.selectedItem = [];
    $scope.documentTypes = [
        {"key": "News document", "value": "namespace:news", "providers": [
            {"key": "Provider 2"}
        ]},
        {"key": "News document2", "value": "namespace:news2", "providers": [
            {"key": "Provider 1"},
            {"key": "Provider 2"}
        ]},
        {"key": "Events document", "value": "namespace:events", "providers": [
            {"key": "Provider 2"},
            {"key": "Provider 3"}
        ]}
    ];
    $scope.providers = [
        {"key": "Provider 1", "value": "hippogogreen:testprov", "path": "hippogogreen/testprov"},
        {"key": "Provider 2", "value": "hippogogreen:testprov", "path": "hippogogreen/testprov"},
        {"key": "Provider 3", "value": "hippogogreen:testprov", "path": "hippogogreen/testprov"}
    ];
    $scope.baseCmsNamespaceUrl = "https://cms.demo.onehippo.com/?path=/hippo:namespaces/";


    $scope.selectChange = function (docName, selectedItem) {
        $log.info(docName, selectedItem);
    };
    $scope.onbuttonclick = function () {
        $log.info($scope.selection);
        $scope.welcomeMessage2 = "foo";
    };
    $scope.onDelete = function (docName) {
        var index = $scope.providers.indexOf(docName)
        $scope.providers.splice(index, 1);
        $log.info(docName);
    };
    $scope.onAdd = function (docName) {
        $log.info(docName);
        $scope.providers.push({"key": docName});
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
        // TODO: fetch docTypes
        /* $http({
         method: 'GET',
         url: $rootScope.REST.documentTypes
         }).success(function (data) {
         $scope.documentTypes = data.items;

         });*/
    };
    $scope.init();

});