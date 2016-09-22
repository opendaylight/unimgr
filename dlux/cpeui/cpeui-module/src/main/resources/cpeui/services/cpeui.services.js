define(['app/cpeui/cpeui.module'],function(cpeui) {

    cpeui.factory('CpeuiSvc', function($http) {
        var baseUrl = "/restconf/config/mef-global:mef-global/";
        var svc = {};

        svc.getTenantList = function(callback) {
            var tenantList = [];
            $http({
                method:'GET',
                url:baseUrl + "tenants-instances"
            }).then(function successCallback(response) {
                tenantList = response.data["tenants-instances"]["tenant-list"];
                if (callback != undefined) {
                    callback(tenantList);
                }
            }, function errorCallback(response) {
                console.log(response);
            });
        };

        svc.addTenant = function(name, serviceType, callback){
            $http({
                method:'POST',
                url:baseUrl + "tenants-instances/",
                data: {"tenant-list":[{
                      "name": name,
                      "service_type": serviceType
                    }]}
            }).then(function successCallback(response) {
                if (callback != undefined) {
                    callback();
                }
            });
        };
        svc.deleteTenant = function(id, callback) {
            $http({
                method:'DELETE',
                url:baseUrl + "tenants-instances/tenant-list/" + id + "/",
            }).then(function successCallback(response) {
                if (callback != undefined) {
                    callback();
                }
            });
        };
        svc.addCe = function(id, name, callback) {
            $http({
                method:'POST',
                url:"/restconf/config/mef-topology:mef-topology/devices/",
                data:{
                      "device": [
                                 {
                                   "dev-id": id,
                                   "device-name":name,
                                   "role": "ce",
                                   "interfaces": {"interface": []}
                                 }
                               ]
                             }

            }).then(function successCallback(response) {
                if (callback != undefined) {
                    callback();
                }
            });
        };

        svc.getCes = function(callback) {
            var ces;
            $http({
                method:'GET',
                url:"/restconf/config/mef-topology:mef-topology/devices/"
            }).then(function successCallback(response) {
                ces = response.data["devices"]["device"];
                ces.forEach(function(c){
                  c.displayName = c['device-name'] ? c['device-name'] : c['dev-id'];
                });
                if (callback != undefined) {
                    callback(ces);
                }
            }, function errorCallback(response) {
                console.log(response);
            });

            return ces;

        };
        svc.removeCe = function(ceid, callback) {
             $http({
                method:'DELETE',
                url:"/restconf/config/mef-topology:mef-topology/devices/device/" + ceid + "/"
            }).then(function successCallback(response) {
                if (callback != undefined) {
                    callback();
                }
            });
        };

        // UNIs

        svc.addUni = function(id, tenantid, callback) {
            $http({
                method:'POST',
                url:"/restconf/config/mef-interfaces:mef-interfaces/unis/",
                data:{"uni": [{
                    "uni-id": id,
                    "tenant-id":tenantid,
                    "admin-state-enabled":true
                    }]}
            }).then(function successCallback(response) {
                if (callback != undefined) {
                    callback();
                }
            });
        };


        svc.updateUni = function(id, tenantid, callback) {
            // TODO didn't find a better way to keep other uni fields, PATCH method is not supported :(
            $http({
                method:'GET',
                url:"/restconf/config/mef-interfaces:mef-interfaces/unis/uni/" + id + "/"
            }).then(function successCallback(response) {
                uni = response.data;
                uni["uni"][0]["tenant-id"] = tenantid;
                if (!tenantid) {
                  delete uni["uni"][0]["tenant-id"];
                }
                uni["uni"][0]["admin-state-enabled"] = true;
                $http({
                  method:'PUT',
                  url:"/restconf/config/mef-interfaces:mef-interfaces/unis/uni/" + id + "/",
                    data:uni
                }).then(function successCallback(response) {
                    if (callback != undefined) {
                        callback();
                    }
                });
            });
        };

        svc.getUnis = function(callback) {
            var unis;
            $http({
                method:'GET',
                url:"/restconf/config/mef-interfaces:mef-interfaces/unis/"
            }).then(function successCallback(response) {
                unis = response.data["unis"]["uni"];
                if (unis != undefined){
                    for (i=0; i < unis.length; i++) {
                        if ((unis[i]["physical-layers"] != undefined) && (unis[i].device = unis[i]["physical-layers"].links != undefined)){
                            unis[i].device = unis[i]["physical-layers"].links.link[0].device;
                        }
                    }
                }

                if (callback != undefined) {
                    callback(unis);
                }
            }, function errorCallback(response) {
                console.log(response);
            });
        };

        svc.getTenantUnis = function(tenantid, callback) {
            var unis;
            svc.getUnis(function(unis){
                if (unis != undefined){
                    unis = unis.filter(function(u){return u["tenant-id"] == tenantid;});
                }
                if (callback != undefined) {
                    callback(unis);
                }
            });
        };

        svc.removeUni = function(uniid, callback) {
             $http({
                method:'DELETE',
                url:"/restconf/config/mef-interfaces:mef-interfaces/unis/uni/" + uniid + "/"
            }).then(function successCallback(response) {
                if (callback != undefined) {
                    callback();
                }
            });
        };

        // EVCs

        function getJsonUnis(unis) {
            var uni_json = [];
            if (unis == undefined) {
                unis = [];
            }
            unis.forEach(function(i){uni_json.push({"uni-id":i});});
            return uni_json;
        }

        svc.addEvc = function(evc, evc_type, tenant, callback) {
            var uni_json = getJsonUnis(evc.unis);
//            preserved-vlan
            var data = {
              "mef-service" :  {
                "svc-id" : evc.id,
                "svc-type" : evc.svc_type,
                "tenant-id" : tenant,
                "evc" : {
                  "evc-id" : evc.id,
                  "evc-type" : evc_type,
                  "preserve-ce-vlan-id" : evc.is_preserve_vlan,
                  "max-svc-frame-size" : evc.mtu_size,
                  "unicast-svc-frm-delivery" : evc.unicast,
                  "multicast-svc-frm-delivery" : evc.multicast,
                  "unis" : {
                    "uni" : uni_json
                  },
                  "admin-state-enabled" : true
                }
              } 
            };
            if (evc.is_preserve_vlan) {
              data["mef-service"]["evc"]["preserved-vlan"] = evc.preserved_vlan;
            }
            $http({
                method:'POST',
                url:"/restconf/config/mef-services:mef-services/",
                data:data
            }).then(function successCallback(response) {
                if (callback != undefined) {
                    callback();
                }
            });
        };

        svc.getEvc = function(tenantid, callback) {
            var evcs;
            $http({
                method:'GET',
                url:"/restconf/config/mef-services:mef-services/"
            }).then(function successCallback(response) {
                evcs = response.data["mef-services"]["mef-service"]; // TODO try to filter on server side

                if (evcs != undefined) {
                    evcs = evcs.filter(function(evc){return evc["tenant-id"] == tenantid;});
                    for (i=0; i < evcs.length; i++) {
                        var unis = evcs[i].evc.unis.uni;
                        if (unis != undefined) {
                            for (j=0; j < unis.length; j++) {
                                if ((unis[j]['evc-uni-ce-vlans'] != undefined) && (unis[j]['evc-uni-ce-vlans']['evc-uni-ce-vlan'] != undefined)){
                                    unis[j].vlans = unis[j]['evc-uni-ce-vlans']['evc-uni-ce-vlan'].map(function(u){return u.vid;}).sort();
                                } else {
                                    unis[j].vlans = [];
                                }
                            }
                        }
                    }
                }
                if (callback != undefined) {
                    callback(evcs);
                }
            }, function errorCallback(response) {
                console.log(response);
            });

            return evcs;
        };
        svc.removeEvc = function(svcid, callback) {
             $http({
                method:'DELETE',
                url:"/restconf/config/mef-services:mef-services/mef-service/" + svcid + "/"
            }).then(function successCallback(response) {
                if (callback != undefined) {
                    callback();
                }
            });
        };

        svc.addEvcUni = function(svcid, uni_id, role, vlans, callback) {
            var data = {"uni":{
                            "uni-id":uni_id,
                            "role":role,
                            "admin-state-enabled":true
                            }
                        };
            if (vlans != undefined) {
                data.uni['evc-uni-ce-vlans'] = {"evc-uni-ce-vlan":[]}
                for (var i=0; i< vlans.length; ++i) {
                    data.uni['evc-uni-ce-vlans']["evc-uni-ce-vlan"].push({"vid":vlans[i]});
                }
            }
             $http({
                method:'POST',
                url:"/restconf/config/mef-services:mef-services/mef-service/" + svcid + "/evc/unis",
                data: data
            }).then(function successCallback(response) {
                if (callback != undefined) {
                    callback();
                }
            }, function failureCallback(response) {
                if (callback != undefined) {
                    callback();
                }
            });
        };

        svc.addEvcUniVlan = function(svcid, uni_id, vlan, callback) {
             $http({
                method:'POST',
                url:"/restconf/config/mef-services:mef-services/mef-service/" + svcid + "/evc/unis/uni/" + uni_id + "/evc-uni-ce-vlans/",
                data:{"evc-uni-ce-vlan":{"vid":vlan}}
            }).then(function successCallback(response) {
                if (callback != undefined) {
                    callback();
                }
            });
        };

        svc.deleteEvcUni = function(svcid, uni_id, callback) {
             $http({
                method:'DELETE',
                url:"/restconf/config/mef-services:mef-services/mef-service/" + svcid + "/evc/unis/uni/" + uni_id + "/"
            }).then(function successCallback(response) {
                if (callback != undefined) {
                    callback();
                }
            });
        };



        svc.deleteVlan = function(svc_id, uni_id, vlan,callback) {
            $http({
                method:'DELETE',
                url:"/restconf/config/mef-services:mef-services/mef-service/" + svc_id + "/evc/unis/uni/" + uni_id + "/evc-uni-ce-vlans/evc-uni-ce-vlan/"+ vlan+"/"
            }).then(function successCallback(response) {
                if (callback != undefined) {
                    callback();
                }
            });
        };

        svc.addVlan = function(svc_id, uni_id, vlan, callback) {
            $http({
                method:'PUT',
                url:"/restconf/config/mef-services:mef-services/mef-service/" + svc_id + "/evc/unis/uni/"+uni_id+"/evc-uni-ce-vlans/evc-uni-ce-vlan/"+vlan+"/",
                data:{"evc-uni-ce-vlan":{"vid":vlan}}
            }).then(function successCallback(response) {
                if (callback != undefined) {
                    callback();
                }
            });
        };

        return svc;

    });
});
