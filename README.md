# Alovoa

<p align="center">
<img src="https://raw.githubusercontent.com/Alovoa/alovoa/master/src/main/resources/static/img/android-chrome-192x192.png">
</p>

Demo: https://alovoa.herokuapp.com/. Database WILL be deleted after each update. A public MySQL update is used, do NOT post your personal information on the demo page, even though most of your info is encrypted. Profile picture size limit had to be lowered to 5KB for money reasons.

Alovoa aims to be the first widespread open-source dating web application on the web. What differs this from other platform?
- No ads
- No selling your data
- No paid features (no "pay super-likes", "pay to swipe", "pay to view profile" or "pay to start a chat")
- No unsecure servers
- No closed-source libraries
- No seeing people you don't want to see
- Encrypting your most private data

### How to build:
- Install a JDK (OpenJDK 11 is officially supported)
- Install maven: https://maven.apache.org/install.html
- Setup a database (MariaDB is officially supported)
- Setup an email server or use an existing one (any provider with IMAP support should work)
- Enter credentials for database server, email server and encryption keys in application.properties
- Execute "mvn install" in the folder where the pom.xml resides.
- Spring Tool Suite is recommended for debugging.

### Emoji support (not available on the test server)
1. Edit my.conf
2. Add following lines to the bottom <br>
`character-set-server = utf8mb4` <br>
`collation-server = utf8mb4_general_ci` <br>
`skip-character-set-client-handshake`
3. Restart mariadb

Example (Fedora):
- sudo nano /etc/my.cnf
- sudo systemctl restart mysqld

### Documentation:
- Please read the DOCUMENTATION.md


### Licenses:
- All code not otherwise stated is licensed under the AGPLv3 license. 
- Third-party web libraries can be found under resources/css/lib and resources/js/lib and have their own license.
- Third-party Java libraries can be found in the pom.xml and have their own license.

### Screenshots:

<img src="https://raw.githubusercontent.com/Alovoa/alovoa/master/screenshots/index.gif">
<img src="https://raw.githubusercontent.com/Alovoa/alovoa/master/screenshots/search.gif">
<img src="https://raw.githubusercontent.com/Alovoa/alovoa/master/screenshots/search_mobile.gif">
