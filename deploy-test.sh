#!/bin/bash
mvn install
mvn pmd:pmd
heroku deploy:jar target/alovoa-0.0.1-SNAPSHOT.jar --app alovoa
