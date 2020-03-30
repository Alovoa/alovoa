#!/bin/bash
mvn install
heroku deploy:jar target/alovoa-0.0.1-SNAPSHOT.jar --app alovoa
