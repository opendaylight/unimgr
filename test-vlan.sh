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
        "name": "188900966400001:s1-eth3",
        "enabled": true,
        "odl-interface:l2vlan-mode": "trunk",
        "type": "iana-if-type:l2vlan",
        "odl-interface:parent-interface": "188900966400001:s1-eth3"
      },
      {
        "name": "188900966400001:s1-eth2",
        "enabled": true,
        "odl-interface:l2vlan-mode": "trunk",
        "type": "iana-if-type:l2vlan",
        "odl-interface:parent-interface": "188900966400001:s1-eth2"
      },
      {
        "name": "188900966400001:s1-eth7.28",
        "enabled": true,
        "odl-interface:l2vlan-mode": "trunk-member",
        "odl-interface:vlan-id": 28,
        "type": "iana-if-type:l2vlan",
        "odl-interface:parent-interface": "188900966400001:s1-eth7"
      },
      {
        "name": "188900966400001:s1-eth6.15",
        "enabled": true,
        "odl-interface:l2vlan-mode": "trunk-member",
        "odl-interface:vlan-id": 15,
        "type": "iana-if-type:l2vlan",
        "odl-interface:parent-interface": "188900966400001:s1-eth6"
      },
      {
        "name": "188900966400001:s1-eth7.24",
        "enabled": true,
        "odl-interface:l2vlan-mode": "trunk-member",
        "odl-interface:vlan-id": 24,
        "type": "iana-if-type:l2vlan",
        "odl-interface:parent-interface": "188900966400001:s1-eth7"
      },
      {
        "name": "188900966400001:s1-eth7",
        "enabled": true,
        "odl-interface:l2vlan-mode": "trunk",
        "type": "iana-if-type:l2vlan",
        "odl-interface:parent-interface": "188900966400001:s1-eth7"
      },
      {
        "name": "188900966400001:s1-eth6",
        "enabled": true,
        "odl-interface:l2vlan-mode": "trunk",
        "type": "iana-if-type:l2vlan",
        "odl-interface:parent-interface": "188900966400001:s1-eth6"
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
        "name": "188900966400001:s1-eth2",
        "elan-instance-name": "1"
      },
      {
        "name": "188900966400001:s1-eth3",
        "elan-instance-name": "1"
      },
      {
        "name": "188900966400001:s1-eth6.15",
        "elan-instance-name": "1"
      },
      {
        "name": "188900966400001:s1-eth7.24",
        "elan-instance-name": "1"
      },
      {
        "name": "188900966400001:s1-eth7.28",
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
