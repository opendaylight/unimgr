#!/bin/bash
dir=karaf/target/assembly
for i in data journal snapshots; do rm -rf $dir/$i; done
$dir/bin/karaf clean
