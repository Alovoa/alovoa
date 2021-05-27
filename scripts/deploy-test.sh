#!/bin/bash
cd ..
mvn install
heroku deploy:jar target/alovoa-1.0.0.jar --app alovoa
