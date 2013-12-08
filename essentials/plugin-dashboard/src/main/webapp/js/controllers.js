/*
 //############################################
 // MAIN CONTROLLER
 //############################################

 */
app.controller('mainCtrl', function ($scope, $sce, $log, $rootScope, $http, MyHttpInterceptor) {
    $rootScope.showPowerpacks = true;

    $scope.stepVisible = [true, false];
    $scope.resultMessages = {"@page": "1", "@totalSize": "0", "message": [
        {"value": "Power Pack successfully installed"},
        {"value": "<h3>Please rebuild and restart your application:<h3>\n<pre>\nmvn clean package\nmvn -P cargo.run\n<\/pre>"},
        {"value": "<p><a href=\"http:\/\/www.onehippo.org\">Read more about Hippo Essentials<\/a><\/p>"}
    ]};
    $scope.selectedDescription = "Please make a selection";
    $scope.message = "Welcome";
    $scope.packs = {"powerpacks": [
        {"enabled": true, "name": "Basic News and Events site", "value": "news-events"},
        {"enabled": false, "name": "A REST only site that contains only REST services and no pages.", "value": "empty-rest"}
    ], "project": {"namespace": "marketplace"}, "steps": [
        {"buttonText": "Next", "name": "Select a powerpack"},
        {"buttonText": "Finish", "name": "Install"}
    ]};

    $scope.trustedContent = $sce.trustAsHtml($scope.resultMessages.message.value);
    $scope.selectChange = function () {
        $log.info($scope.selectedItem);
        $scope.selectedDescription = "foo";
    };
    $scope.onWizardButton = function (index, step) {
        $log.info($scope.packs.steps[0]);
        $log.info("====================" + index);
        if (index == 0) {
            $scope.stepVisible[0] = false;
            $scope.stepVisible[1] = true;
            $scope.packs.steps[0].buttonText = "Previous";
        }
        else {
            $scope.stepVisible[0] = true;
            $scope.stepVisible[1] = false;
            $scope.packs.steps[0].buttonText = "Next";


        }


        $log.info($scope.stepVisible);

    }


});

/*
 //############################################
 // MENU CONTROLLER
 //############################################

 */
app.controller('mainMenuCtrl', function ($scope, $log, $rootScope, $http, MyHttpInterceptor) {

    $scope.menu = [
        {item: "Plugins"},
        {item: "Tools"}
    ];

    $scope.onMenuClick = function (menuItem) {
        $log.info(menuItem);
    };


});