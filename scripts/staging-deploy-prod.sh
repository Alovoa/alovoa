#!/bin/bash
cd ..
git pull origin master
mvn clean install -DskipTests
cd target
read -sp 'Password: ' pw
fuser -k 8844/tcp
export JASYPT_ENCRYPTOR_PASSWORD=$pw
nohup java -XX:+HeapDumpOnOutOfMemoryError -Xmx128m -jar -Dfile.encoding=UTF-8 -Dspring.profiles.active=prod alovoa-1.1.0.jar &
sleep 5
unset JASYPT_ENCRYPTOR_PASSWORD