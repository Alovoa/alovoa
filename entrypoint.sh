#!/usr/bin/env bash

java \
  -XX:+HeapDumpOnOutOfMemoryError \
  -Xmx128m \
  -jar \
  -Dfile.encoding=UTF-8 \
  -Dspring.profiles.active=prod \
  alovoa-1.1.0.jar

