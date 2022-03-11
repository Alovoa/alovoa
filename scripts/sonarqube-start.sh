#!/bin/bash
sudo systemctl start podman
podman run -d --name sonarqube -p 9000:9000 -p 9092:9092 -v sonarqube-conf:/opt/sonarqube/conf -v sonarqube-data:/opt/sonarqube/data -v sonarqube-logs:/opt/sonarqube/logs -v sonarqube-extensions:/opt/sonarqube/extensions sonarqube

