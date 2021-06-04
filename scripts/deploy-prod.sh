#!/bin/bash
export JASYPT_ENCRYPTOR_PASSWORD=secret
cd ..
java -jar -Dspring.profiles.active=prod target/alovoa-1.0.0.jar > /dev/null
sleep 10
unset JASYPT_ENCRYPTOR_PASSWORD
