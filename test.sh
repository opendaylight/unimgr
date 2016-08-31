#!/bin/bash
put='-X PUT -H "Content-Type: application/json" -u admin:admin'
config=http://localhost:8181/restconf/config
ietfIfsApi=ietf-interfaces:interfaces/
elanInstApi=elan:elan-instances/
elanIfsApi=elan:elan-interfaces/
ietfIfs='{
  "interfaces": {
    "interface": [
      {
        "name": "s1-eth2",
        "enabled": true,
        "odl-interface:l2vlan-mode": "trunk",
        "type": "iana-if-type:l2vlan",
        "odl-interface:parent-interface": "188900966400001:s1-eth2"
      },
      {
        "name": "s1-eth1",
        "enabled": true,
        "odl-interface:l2vlan-mode": "trunk",
        "type": "iana-if-type:l2vlan",
        "odl-interface:parent-interface": "188900966400001:s1-eth1"
      }
    ]
  }
}'

elanInst='{
  "elan-instances": {
    "elan-instance": [
      {
        "elan-instance-name": "1",
        "mac-timeout": 30,
        "elan-tag": 5000
      }
    ]
  }
}'


elanIfs='{
  "elan-interfaces": {
    "elan-interface": [
      {
        "name": "s1-eth1",
        "elan-instance-name": "1"
      },
      {
        "name": "s1-eth2",
        "elan-instance-name": "1"
      }
    ]
  }
}'

echo curl $put -d "'"$ietfIfs"'" $config/$ietfIfsApi
sleep 3
echo curl $put -d "'"$elanInst"'" $config/$elanInstApi
sleep 3
echo curl $put -d "'"$elanIfs"'" $config/$elanIfsApi
