define([ 'app/cpeui/cpeui.module' ], function(cpeui) {
  cpeui.register.controller('OpenTenantCtrl', function($scope, CpeuiSvc, CpeuiDialogs, $stateParams) {

    $scope.curTenant = $stateParams.tenantid;

    $scope.unisTables = {};
    $scope.unis = [];
    $scope.ces = [];
    $scope.cesDisplayNames = {};
    $scope.unisMap = {};


    function init(){
      $scope.updateUnis(function(unis){
        CpeuiSvc.getCes(function(ces) {
          $scope.ces = ces.filter(function(item) {
            return (unis.filterByField('device', item["dev-id"]).length);
          });
          ces.forEach(function(ce){
            $scope.cesDisplayNames[ce['dev-id']] = ce['device-name'] ? ce['device-name'] : ce['dev-id'];
          });
          $scope.updateEvcView();
        });
      });
    }

    $scope.updateUnis = function(callback) {
      CpeuiSvc.getUnis(function(unis) {
        unis.forEach(function(u) {
          u.prettyID = u['uni-id'].split(":")[u['uni-id'].split(":").length - 1];
          $scope.unisMap[u['uni-id']] = u;
        });
        $scope.unis = unis.filter(function(u){return u["tenant-id"] == $scope.curTenant;});

        if (callback) {
          callback($scope.unis);
        }
      });
    };

    $scope.updateEvcView = function() {
      CpeuiSvc.getEvc($scope.curTenant, function(evcs) {
        $scope.evcs = evcs;

        $scope.updateUnis();

        $scope.evcs.forEach(function(e){
          e.isTree = (e.evc['evc-type'] == 'rooted-multipoint');
          e.device2unis = {};
          if (e.evc.unis.uni != undefined){
            e.evc.unis.uni.forEach(function(u){
              if (e.device2unis[$scope.unisMap[u['uni-id']].device] == undefined){
                e.device2unis[$scope.unisMap[u['uni-id']].device] = [];
              }
              e.device2unis[$scope.unisMap[u['uni-id']].device].push(u);
            });
          }
        });
      });
    };

    $scope.title = function(str) {
      return str.split('-').map(function(s) {
        return s[0].toUpperCase() + s.slice(1);
      }).join(' ');
    }

    $scope.svcTypes = [ 'epl', 'evpl', 'eplan', 'evplan', 'eptree', 'evptree' ]
    var evcTypes = {
      'epl' : 'point-to-point',
      'evpl' : 'point-to-point',
      'eplan' : 'multipoint-to-multipoint',
      'evplan' : 'multipoint-to-multipoint',
      'eptree' : 'rooted-multipoint',
      'evptree' : 'rooted-multipoint'
    }

    var addEvcController = function($scope, $mdDialog) {
      $scope.validate = function(form) {
        form.svc_type.$setTouched(); // patch because angular bug http://stackoverflow.com/questions/36138442/error-not-showing-for-angular-material-md-select
        console.log($scope);
        return form.$valid;
      };
    };

    $scope.evcDialog = new CpeuiDialogs.Dialog('AddEvc', {}, function(obj) {
      CpeuiSvc.addEvc(obj, evcTypes[obj.svc_type], $scope.curTenant, function() {
            $scope.updateEvcView();
          });
    }, addEvcController);

    $scope.deleteEvc = function(svcid) {
      CpeuiDialogs.confirm(function() {
        CpeuiSvc.removeEvc(svcid, function() {
          $scope.updateEvcView();
        });
      });
    };

    $scope.deleteEvcUni = function(svc_id, uni_id) {
      CpeuiDialogs.confirm(function() {
        CpeuiSvc.deleteEvcUni(svc_id, uni_id, function() {
          $scope.updateEvcView();
        });
      });
    };

    var linkEvcUniController = function($scope, $mdDialog, params) {
      $scope.params = params;
      $scope.obj = {
        vlans : []
      };
      $scope.deleteVlan = function(vlan) {
        $scope.obj.vlans.splice($scope.obj.vlans.indexOf(vlan), 1);
      };
      $scope.addVlan = function(vlan) {
        if ($scope.obj.vlans.indexOf(vlan) == -1) {
          $scope.obj.vlans.push(vlan);
          console.log(vlan);
        }
        $('#vlan_input').val(undefined);
      };
      
      $scope.filterUsedUnis = function(evc){
        return function(u) {
          if (evc.evc.unis.uni == undefined) {
            evc.evc.unis.uni = [];
          }
          return evc.evc.unis.uni.filterByField('uni-id',u['uni-id']).length == 0;
        };
      };
    };

    $scope.linkEvcUniDialog = new CpeuiDialogs.Dialog('LinkEvcUni', {},
        function(obj) {
          if (!obj.role) {
            obj.role = "root";
          }
          CpeuiSvc.addEvcUni(obj.svc_id, obj.uni_id, obj.role, obj.vlans,
              function() {
                $scope.updateEvcView();
              });
        }, linkEvcUniController);

    var editVlanController = function($scope, $mdDialog, params) {
      $scope.params = params;

      $scope.deleteVlan = function(svc_id, uni_id, vlan, allvlans) {
        CpeuiSvc.deleteVlan(svc_id, uni_id, vlan, function() {
          allvlans.splice(allvlans.indexOf(vlan), 1)
        });
      };
      $scope.addVlan = function(svc_id, uni_id, vlan, allvlans) {
        if (allvlans == undefined) {
          allvlans = [];
        }
        if (allvlans.indexOf(vlan) == -1) {
          CpeuiSvc.addVlan(svc_id, uni_id, vlan, function() {
            allvlans.push(vlan);
          });
        }
        $('#vlan_input').val(undefined);
      };
    };

    $scope.editVlanDialog = new CpeuiDialogs.Dialog('EditVlans', {}, undefined, editVlanController);

    $scope.sortEvc = function(evc) {
      return evc.evc['evc-id'];
    };
    $scope.sortUni = function(uni) {
      return uni['uni-id'];
    };

    init();
  });
});
