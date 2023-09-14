#!/bin/bash

rm -rf ./tmp;
mkdir -p tmp && find . -name '*tar.gz' -exec tar -C tmp -xzf {} \;
find . -path './tmp/tpe1-g1-*/*' -exec chmod u+x {} \;

find . -name '*.sh' -exec sed -i -e 's/\r$//' {} \;