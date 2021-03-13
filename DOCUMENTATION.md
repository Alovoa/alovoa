# Documentation

## src/main/java

### com.nonononoki.alovoa
- AlovoaApplication.java
Main Function that starts the web server
- Tools.java
A Collection of static functions that can be used everywhere
 
### com.nonononoki.alovoa.component
- AuthFilter.java
Authentication filter necessary for authentication with Captcha, provides extended Token with Captcha info
- AuthProvider.java
Authentication provider necessary for basic wither username and password authentication
- FailureHandler.java
Class that redirects the user when authentication failed
- RestExceptionHandler.java
Class to print out exceptions instead of letting the server crash
- SuccessHandler.java
Class that redirects the user when authentication succeded
- TextEncryptorConverter.java
Class that can be used as Annotation to encrypt a column in the database

### com.nonononoki.alovoa.config
- EventListenerConfig.java
Sets some default database entries when server starts
- SecurityConfig.java
Sets security configurations for Spring Boot. Includes protected links, authentication redirection, login and logout URL, text encryption key and authentication components
- WebMvcConfig.java
Class for handling internationalization

### com.nonononoki.alovoa.entity
All classes in this package represent database tables and their relations

### com.nonononoki.alovoa.html
All classes represent html pages, referenced in src/main/resources/templates

### com.nonononoki.alovoa.lib
- OxCaptcha.java
Captcha library slightly modified to support transparent background

### com.nonononoki.alovoa.model
This package contains Data Objects without many methods

### com.nonononoki.alovoa.repo
This package contains JpaRepositories for all tables

### com.nonononoki.alovoa.rest
This package contains RestControllers for all REST APIs

### com.nonononoki.alovoa.service
This package contains Service classes that contain the logic of the REST APIs











