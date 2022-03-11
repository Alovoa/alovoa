#!/bin/bash
cd ..
mvn sonar:sonar   -Dsonar.projectKey=Alovoa   -Dsonar.host.url=http://localhost:9000   -Dsonar.login=admin -Dsonar.password=a975104f8864c39a746c232fac6852cc2026a691
