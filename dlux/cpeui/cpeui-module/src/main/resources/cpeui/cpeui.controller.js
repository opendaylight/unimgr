var controllers = ['app/cpeui/cpeui.controller', 
                   'app/cpeui/admin.controller',
                   'app/cpeui/tenant.controller', 
                   'app/cpeui/tenantsTable.controller' ];
var services = ['app/cpeui/services/cpeui.services',
                'app/cpeui/services/utils',
                'app/cpeui/services/cpeui.dialogs', ];
var directives = [];
var modules = [ 'app/cpeui/cpeui.module' ];

define([].concat(modules).concat(services).concat(directives).concat(controllers), function(cpeui) {

  cpeui.controller('CpeuiCtrl', function($scope, $rootScope, $state, $mdDialog,$mdMedia) {

    $rootScope.section_logo = 'src/app/cpeui/static/logo_hpe.gif';

    var mainTabIndexs = {
      "tenants" : 1,
      "cpes" : 2,
      "unis" : 3,
      "networks" : 4,
      "profiles" : 5
    }

    $scope.tab = {
      tenantData : 1,
      admin : 1,
      L3subTable:0
    };
    var selectedTab = mainTabIndexs[$state.params.tabName];
    if (selectedTab != undefined) {
      $scope.tab.admin = selectedTab;
    }

    // / Methods

    $scope.setTab = function(key, newTab) {
      $scope.tab[key] = newTab;
    };

    $scope.isTabSet = function(key, tabNum) {
      return $scope.tab[key] == tabNum;
    };

  });

});
