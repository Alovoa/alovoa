#!/bin/bash
export JASYPT_ENCRYPTOR_PASSWORD=secret
java -jar -Dspring.profiles.active=test target/alovoa-0.0.1-SNAPSHOT.jar > alovoa.txt
sleep 60
unset JASYPT_ENCRYPTOR_PASSWORD
