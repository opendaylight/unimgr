define([ 'app/cpeui/cpeui.module' ], function(cpeui) {

  cpeui.factory('CpeuiDialogs', function($mdDialog, $mdMedia, CpeuiSvc) {
    var svc = {};

    svc.Dialog = function(tpl, _params, callback, customController) {

      this.customFullscreen = $mdMedia('xs') || $mdMedia('sm');

      this.dialogController = function($scope, $mdDialog, params) {
        $scope.params = params;
        $scope.callback = callback;

        $scope.obj = {};
        $scope.hide = function() {
          $mdDialog.hide();
        };
        $scope.cancel = function() {
          $mdDialog.cancel();
        };
        $scope.done = function() {
          if ($scope.projectForm.$valid) {
            $scope.callback($scope.obj);
            $mdDialog.hide();
          }
        };

        if (customController != undefined) {
          customController($scope, $mdDialog, params, CpeuiSvc);
        }

      };

      this.show = function(ev, params) {
        if (_params) {
            params = angular.merge(_params,params);
        }
        $mdDialog.show({
          controller : this.dialogController,
          templateUrl : 'src/app/cpeui/dialogs/' + tpl + '.tpl.html',
          parent : angular.element(document.body),
          targetEvent : ev,
          clickOutsideToClose : false,
          fullscreen : this.customFullscreen,
          locals : {
            params : params
          },
         onComplete: function() {$('md-dialog').draggable();}
        });
      };
    };

    svc.confirm = function(callback_ok, callback_cancel) {
      svc.customConfirm('Are you Sure?', "", callback_ok, callback_cancel);
    };

    svc.customConfirm = function(title, content, callback_ok, callback_cancel) {
      var confirm = $mdDialog.confirm().title(title).textContent(content).ok(
          'Yes!').cancel('Cancel');
      $mdDialog.show(confirm).then(callback_ok, callback_cancel);
    };

    svc.alert = function(title, content, callback_ok) {
      var alert = $mdDialog.alert().title(title).textContent(content).ok(
          'Ok');
      $mdDialog.show(alert).then(callback_ok);
    };
    
    return svc;
  });

});