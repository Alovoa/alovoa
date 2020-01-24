# Alovoa

Alovoa aims to be the first widespread open-source dating web application on the web. What differs us from other platform?
- No ads
- No selling your data
- No paid features (no "pay super-likes", "pay to swipe", "pay to view profile" or "pay to start a chat")
- No unsecure servers
- No closed-source libraries
- No seeing people you don't want to see

### DONE
- Home view
- Profile view and edit
- Registration
- Login
- Search and filter

### TODO
- Liking user
- Hiding user
- Blocking user
- Reporting user
- View details of user
- Donation view and functions
- Notification view and functions
- Chat view and functions
- Resetting password
- GDPR compliance (see saved data, delete all data)
- PWA compability

### How to build:
- Install a JDK (OpenJDK 8 is officially supported)
- Install maven: https://maven.apache.org/install.html
- Setup a database (MariaDB is officially supported)
- (Optional) Setup an email server
- Enter credentials for database server, email server and encryption keys in application.properties
- Execute "mvn install"
