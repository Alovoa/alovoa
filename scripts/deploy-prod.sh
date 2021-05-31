#!/bin/bash
export JASYPT_ENCRYPTOR_PASSWORD=secret
cd ..
java -jar -Dspring.profiles.active=test target/alovoa-1.0.0.jar > /dev/null
sleep 20
unset JASYPT_ENCRYPTOR_PASSWORD
