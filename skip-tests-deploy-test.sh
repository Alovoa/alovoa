#!/bin/bash
mvn install -DskipTests
heroku deploy:jar target/alovoa-0.0.1-SNAPSHOT.jar --app alovoa
