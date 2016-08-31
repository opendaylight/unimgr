#!/bin/bash
pushd netvirt/
mvn clean install -DskipTests
if [[ ! $? ]]; then exit; fi
popd

karunimgrnetvirt=$(find karaf/ -name unimgr-netvirt-*-SNAPSHOT.jar)
newunimgrnetvirt=$(find netvirt/ -name unimgr-netvirt-*-SNAPSHOT.jar)

echo "cp -f $newunimgrnetvirt $karunimgrnetvirt" 
cp -f $newunimgrnetvirt $karunimgrnetvirt 

bootorig='featuresBoot = config,standard,region,package,kar,ssh,management,odl-unimgr-ui'
bootours='featuresBoot = config,standard,region,package,kar,ssh,management,odl-unimgr-netvirt'
echo sed -i "s:$bootorig:$bootours:g" karaf/target/assembly/etc/org.apache.karaf.features.cfg
sed -i "s:$bootorig:$bootours:g" karaf/target/assembly/etc/org.apache.karaf.features.cfg

