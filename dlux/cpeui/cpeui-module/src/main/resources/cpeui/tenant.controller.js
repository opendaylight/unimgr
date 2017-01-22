define([ 'app/cpeui/cpeui.module' ], function(cpeui) {
  cpeui.register.controller('OpenTenantCtrl', function($scope, CpeuiSvc, CpeuiDialogs, $stateParams) {

    $scope.curTenant = $stateParams.tenantid;
    $scope.unisTables = {};
    $scope.unis = [];
    $scope.ces = [];
    $scope.ipvcs = [];
    $scope.evcs = [];
    $scope.subnets = {};
    $scope.profiles =[];
    $scope.cesDisplayNames = {};
    $scope.unisMap = {};
    $scope.networkNames = {};
    $scope.expandFlags = {
        ipuni:{},
        tuni:{},
        L2:{},L3:{}
    };

    var tabIndexs = {
        "inventory" : 0,
        "L2" : 1,
        "L3" : 2,
        "unis" : 6,
      }
    if ($stateParams.tenantTabName in tabIndexs) {
      $scope.tab.tenantData = tabIndexs[$stateParams.tenantTabName];
    } else {
        $scope.tab.tenantData = tabIndexs.inventory;
    }

    CpeuiSvc.getTenantList(function(tenant_list) {
        if (tenant_list.filter(t => t.name == $scope.curTenant).length == 0) {
            window.location = "#/cpeui/admin/tenants";
        }
    });

    function init(){
      $scope.updateUnis(function(unis){
        CpeuiSvc.getCes(function(ces) {
          $scope.ces = ces.filter(function(item) {

            var filteredUnis = unis.filterByField('device', item["dev-id"]);
            filteredUnis = filteredUnis.filterByField('prettyID', 'br-int', true);
            filteredUnis = filteredUnis.filter(function(i){return !(i.prettyID && i.prettyID.startsWith('tun'));});

            return (filteredUnis.length);
          });
          ces.forEach(function(ce){
            $scope.cesDisplayNames[ce['dev-id']] = ce['device-name'] ? ce['device-name'] : ce['dev-id'];
          });
          $scope.updateEvcView();
        });
      });

      CpeuiSvc.getNetworkNames(function(networks){
        networks.forEach(function(net){
          $scope.networkNames[net.uuid] = net.name;
        });
      });

      CpeuiSvc.getProfiles(function(profiles) {
        $scope.profiles = profiles;
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
      CpeuiSvc.getServices($scope.curTenant, function(services) {

        $scope.evcs = services.filter(function(svc){ return svc.evc != undefined;});
        $scope.ipvcs = services.filter(function(svc){ return svc.ipvc != undefined;});
        $scope.updateUnis();
        function mapUniToService(uni, service) {
            var uniObj = $scope.unis.filterByField('uni-id',uni['uni-id'])[0];
            if (uniObj === undefined) {
                return;
            }
            if (!uniObj.vlanToService) {
                uniObj.vlanToService = [];
            }
            uniObj.hasService = true;
            if (uni['evc-uni-ce-vlans'] && uni['evc-uni-ce-vlans']['evc-uni-ce-vlan']){
                uni['evc-uni-ce-vlans']['evc-uni-ce-vlan'].forEach(function(v){
                    uniObj.vlanToService.push({"vlan":v.vid, "svc":service});
                });
            } else {
                if (uni["ip-uni-id"]) {
                    var ipuni = $scope.unis.filterByField('uni-id',uni['uni-id'])[0];
                    ipuni["ip-unis"]["ip-uni"].forEach(function(ipu){
                        if (ipu['ip-uni-id'] == uni["ip-uni-id"]){
                            var vlan = ipu.vlan ? ipu.vlan : 0;
                            uniObj.vlanToService.push({"vlan":vlan, "svc":service});
                        }
                    });
                } else {
                    uniObj.vlanToService.push({"vlan":0, "svc":service});
                }
            }
        }

        $scope.ipvcs.forEach(function(e){
          if (e.ipvc.unis != undefined && e.ipvc.unis.uni != undefined){
              e.ipvc.unis.uni.forEach(function(u){
                u.device = u['uni-id'].split(":")[u['uni-id'].split(":").length-2];
                u.prettyID = u['uni-id'].split(":")[u['uni-id'].split(":").length-1];
                mapUniToService(u,e);
            });
          }
        });
        $scope.evcs.forEach(function(e){
          e.isTree = (e.evc['evc-type'] == 'rooted-multipoint');
          e.device2unis = {};
          if (e.evc.unis != undefined && e.evc.unis.uni != undefined){
            e.evc.unis.uni.forEach(function(u){
              if (e.device2unis[$scope.unisMap[u['uni-id']].device] == undefined){
                e.device2unis[$scope.unisMap[u['uni-id']].device] = [];
              }
              u.prettyID = u['uni-id'].split(":")[u['uni-id'].split(":").length - 1];
              e.device2unis[$scope.unisMap[u['uni-id']].device].push(u);
              mapUniToService(u,e);
            });
          }
        });
      });
      CpeuiSvc.getAllIpUniSubnets(function(subnets){
        $scope.subnets = subnets;
      });
    };

    $scope.doesAllUniHasService = function(ceUnis) {
        for (var i=0 ; i< ceUnis.length; ++i) {
            if (!ceUnis[i].hasService) {
                return false;
            }
        }
        return true;
    }

    $scope.title = function(str) {
      if (!str) {
        return str;
        }
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

    $scope.ipvcDialog = new CpeuiDialogs.Dialog('AddIpvc', {}, function(obj) {
      CpeuiSvc.addIpvc(obj, $scope.curTenant, function() {
            $scope.updateEvcView();
          });
    });

    $scope.linkIpvcUniDialog = new CpeuiDialogs.Dialog('LinkIpvcUni', {},
        function(obj) {
          CpeuiSvc.addIpvcUni(obj.svc_id, obj.uni['uni-id'], obj.ip_uni, obj.profile_name,
              function() {
                $scope.updateEvcView();
              });
        });

    var ipUniDialogController = function($scope, $mdDialog) {
      $scope.hasVlans = false;
      if ($scope.params.uni['ip-unis'] && $scope.params.uni['ip-unis']['ip-uni']) {
        var ipunis = $scope.params.uni['ip-unis']['ip-uni'];
        for (i = 0; i < ipunis.length; i++) {
          if (ipunis[i].vlan){
            $scope.hasVlans = true;
          }
        }
      }
    };

    $scope.ipUniDialog = new CpeuiDialogs.Dialog('AddIpUni', {}, function(obj) {
      CpeuiSvc.addIpUni(obj['uni-id'], obj['ip-address'], obj.vlan, obj['segmentation-id'], function() {
          $scope.updateUnis();
        });
      }, ipUniDialogController);

    $scope.openIpUniDialog = function(event,uni){
      if (uni['ip-unis'] && (uni['ip-unis']['ip-uni'] != undefined)){
        var ipunis = uni['ip-unis']['ip-uni'];
        for (i = 0; i < ipunis.length; i++) {
          if (!ipunis[i].vlan){
            CpeuiDialogs.alert("Error","You Can't have more then one ip-uni with no vlan. please remove the non-vlan ip-uni before adding new.")
            return;
          }
        }
      }
      $scope.ipUniDialog.show(event,{'uniid':uni['uni-id'], uni:uni})
    }

    $scope.ipUniSubnetDialog = new CpeuiDialogs.Dialog('AddIpUniSubnet', {}, function(obj) {
      CpeuiSvc.addIpUniSubnet(obj.uniid, obj.ipuniid, obj.subnet, obj.gateway, function() {
        if ($scope.subnets[obj.uniid] == undefined){
          $scope.subnets[obj.uniid] = {};
        }
        if ($scope.subnets[obj.uniid][obj.ipuniid] == undefined) {
          $scope.subnets[obj.uniid][obj.ipuniid] = [];
        }
        $scope.subnets[obj.uniid][obj.ipuniid].push({
          "uni-id": obj.uniid,
          "ip-uni-id": obj.ipuniid,
          "subnet": obj.subnet,
          "gateway": obj.gateway
        });
      });
    });

    $scope.deleteIpUni = function(uniid, ipuni_id) {
        CpeuiDialogs.confirm(function() {
          CpeuiSvc.deleteIpUni(uniid, ipuni_id, function() {
            $scope.updateEvcView(); // TODO update unis only
          });
        });
      };
      $scope.deleteIpvcUni = function(svc_id, uni_id, ipuni_id) {
        CpeuiDialogs.confirm(function() {
          CpeuiSvc.deleteIpvcUni(svc_id, uni_id, ipuni_id, function() {
            $scope.updateEvcView();
          });
        });
      };
      $scope.getMefInterfaceIpvc = function(uni_id,ipuni_id){
        var uni = $scope.unis.filterByField('uni-id',uni_id)[0];
        if ((uni == undefined) || (uni['ip-unis'] == undefined) || (uni['ip-unis']['ip-uni'] == undefined)) {
          return undefined;
        }
        return uni['ip-unis']['ip-uni'].filterByField('ip-uni-id',ipuni_id)[0];
      }

      $scope.deleteIpUniSubnet = function(uniid, ipuni_id, subnet) {
          CpeuiDialogs.confirm(function() {
            CpeuiSvc.deleteIpUniSubnet(uniid, ipuni_id, subnet, function() {
              $scope.updateEvcView(); // TODO update unis only
            });
          });
    };

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
          if (u.prettyID == 'br-int') {
            return false;
          }
          if (u.prettyID && u.prettyID.startsWith('tun')) {
            return false;
          }
          if (evc.evc == undefined || evc.evc.unis.uni == undefined){
            return true;
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
          CpeuiSvc.addEvcUni(obj.svc_id, obj.uni_id, obj.role, obj.vlans, obj.profile_name,
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

    $scope.sortUni = function(uni) {
      return uni['uni-id'];
    };

    $scope.isEmpty = function(obj){
        return angular.equals({}, obj);
    }

    init();
  });
});
