app.controller('contentBlocksCtrl', function ($scope, $sce, $log, $rootScope, $http, MyHttpInterceptor) {

    // TODO fetch columns
    $scope.columns = {"items": [
        {"key": "Left column", "value": "left"},
        {"key": "Right column", "value": "right"}
    ]};
    $scope.welcomeMessage = "Content blocks plugin";
    $scope.selection = [];
    $scope.documentTypes = [];



    $scope.toggleCheckBox = function(docName) {
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
        $http({
            method: 'GET',
            url: $rootScope.REST.documentTypes
        }).success(function (data) {
                    $scope.documentTypes = data.items;

                });
    };
    $scope.init();

});