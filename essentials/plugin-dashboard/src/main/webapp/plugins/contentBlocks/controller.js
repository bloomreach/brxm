app.controller('contentBlocksCtrl', function ($scope, $sce, $log, $rootScope, $http, MyHttpInterceptor) {

    $scope.welcomeMessage = "Content blocks plugin";

    $scope.documentTypes = [
        {"value": "namespace:news", "key":"News document"},
        {"value": "namespace:events", "key": "Events document"}
    ];

    $scope.documentTypesSelected = [];



    $scope.init = function () {

    };
    $scope.init();




});