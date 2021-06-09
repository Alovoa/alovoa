#!/bin/bash
read -sp 'Password: ' pw
cd ../target
fuser -k 8843/tcp
export JASYPT_ENCRYPTOR_PASSWORD=$pw
nohup java -jar -Dspring.profiles.active=prod alovoa-1.0.0.jar &
sleep 5
unset JASYPT_ENCRYPTOR_PASSWORD
