define([ 'app/cpeui/cpeui.module' ], function(cpeui) {

  cpeui.register.controller('TenantTableCtrl', function($scope, CpeuiSvc, CpeuiDialogs, $mdDialog, $mdMedia) {

    // Tenants

    $scope.updateTenantView = function() {
      CpeuiSvc.getTenantList(function(tenant_list) {
        $scope.tenantArray = tenant_list;
      });
    }

    $scope.AddTenant = function(serviceType) {
      CpeuiSvc.addTenant($scope.tenantId, serviceType, function() {
        $scope.updateTenantView();
      });
    };

    $scope.tenantDialog = new CpeuiDialogs.Dialog('AddTenant', {},
        function(obj) {
          CpeuiSvc.addTenant(obj.id, obj.service_type, function() {
            $scope.updateTenantView();
          });
        });

    $scope.OpenTenantPortal = function(tenant) {
      $scope.currentTenent = tenant;
      window.location = "#cpeui/tenant/" + tenant.name;
    };

    $scope.DeleteTenant = function(tenantID) {
      CpeuiDialogs.confirm(function() {
        CpeuiSvc.deleteTenant(tenantID, function() {
          $scope.updateTenantView();
        });
      });
    };

    $scope.updateTenantView();
  });
});
