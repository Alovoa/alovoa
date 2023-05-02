#!/bin/bash
cd ..
mvn install -DskipTests
heroku deploy:jar target/alovoa-1.1.0.jar --app alovoa
