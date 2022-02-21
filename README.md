# Alovoa

<p align="center">
<img src="https://raw.githubusercontent.com/Alovoa/alovoa/master/src/main/resources/static/img/android-chrome-192x192.png">
</p>

Website: https://alovoa.com/
Mirror: <a href="https://codeberg.org/Nonononoki/alovoa">Codeberg</a>

Alovoa aims to be the first widespread open-source dating web application on the web. What differs this from other platform?
- No ads
- No selling your data
- No paid features (no "pay super-likes", "pay to swipe", "pay to view profile" or "pay to start a chat")
- No unsecure servers
- No closed-source libraries
- No seeing people you don't want to see with advanced filters
- Your most private data is securely encrypted

### How to build:
- Install a JDK >= 9 (OpenJDK 11 is officially supported)
- Install maven: https://maven.apache.org/install.html
- Setup a database (MariaDB is officially supported)
- Setup an email server or use an existing one (any provider with IMAP support should work)
- Enter credentials for database server, email server and encryption keys in application.properties
- Execute "mvn install" in the folder where the pom.xml resides

### Debugging
- Spring Tool Suite is recommended for debugging
- Find the lombok.jar (should be in the ~/.m2) and execute it with "java -jar"

### Documentation:
- Please read the DOCUMENTATION.md

### Licenses:
- All code is licensed under the AGPLv3 license, unless stated otherwise. 
- All images are proprietary, unless stated otherwise.
- Third-party web libraries can be found under resources/css/lib and resources/js/lib and have their own license.
- Third-party Java libraries can be found in the pom.xml and have their own license.
