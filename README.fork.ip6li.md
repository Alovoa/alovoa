# Generation vapi Keypair

Alovoa need for class *PushAsyncService* used in *NotificationService* a key pair.

```
$ git clone --depth 1 https://github.com/web-push-libs/webpush-java.git
$ cd webpush-java
$ ./gradlew run --args="generate-key"
> Task :run
PublicKey:
BBGKmyzGuw53tFpqxy-ol7v1FF5932sRV6iKPdtGtcFPTSC76QmevQw89pVSSzvPQCN9f_v2JbsF7n4ieAG2nhQ=
PrivateKey:
JIEdQkCnF-YFAI-kOExRzPIDO4qxSFeHT-RGdxmTuFs=
```

Save generated private and public key in vault as *app.vapid.public* and *app.vapid.private*.

# Vault

It is a really bad idea to store credentials or private keys in source code or
property files. Spring Boot support vault solutions like Hashicorp Vault.

Before starting Alovoa the first time you should set up Hashicorp Vault.

## Config

Following config is able to store mostly everything which may be configurable
in *application.properties*. Alovoa fork (ab)uses vault as an alternative
properties store.

Generate an individual vapi keypair as shown above.

```
vault secrets enable -path=alovoa kv
vault kv put -mount=alovoa creds app.text.key=32charKey app.text.salt=16charSalt app.admin.key=InitialAdmiPassword app.admin.email=AdminEmail spring.mail.username=SmtpUsername spring.mail.password=SmtpPassword app.vapid.private=vapidPrivateKey app.vapid.public=vapidPublicKey
```

Feel free to add more properties you would not like to see in application.properties.

## Database Credentials

Add a MySQL user which is allowed to set passwords. Configure that user in Hashicorp Vault:

```
vault write database/config/alovoa \
  plugin_name=mysql-database-plugin \
  connection_url="{{username}}:{{password}}@tcp(mysql)/" \
  allowed_roles="mysqlrole" \
  username="vault" \
  password="PasswordForMysqlUserVault"
```

Tell vault how to create a temp user. This user is retrieved and used by Alovoa:

```
vault write database/roles/mysqlrole \
  db_name=alovoa \
  creation_statements="CREATE USER '{{name}}'@'%' IDENTIFIED BY '{{password}}'; GRANT ALL PRIVILEGES ON alovoa.* TO {{name}}'@'%';" \
  default_ttl="1h" \
  max_ttl="24h"
```

Alovoa should not use root vault token, it should use a dedicated token with specific entitelments:

```
vault policy write alovoa-policy - << EOF
path "database/creds/mysqlrole" {
  capabilities = ["read"]
}
path "oauth-idp/keycloak" {
  capabilities = ["read"]
}
path "alovoa/creds" {
  capabilities = ["read"]
}
EOF
```

At least OAuth2 credentials are added:

```
vault secrets enable -path=oauth-idp kv
vault kv put -mount=oauth-idp keycloak client-id=TheClientId client-secret=TheClientSecret
```

# Redis

**Do not use yet**

For now Alovoa throws some exceptions while trying to initialize Redis connection. You need not
care about those exceptions for now.

## Why Redis?

* Redis scales much better than MariaDB or Postgresql.
* Redis cluster is not transaction aware, but this is mostly not required for Alovoa.
* Primary usecase: Save user images or videos in Redis instead of MariaDB.

## Redis Details

Redis should be used with mTLS only so mutual authentication is ensured. Following items are used
in *application.properties*:

```
app.redis.enabled=false
spring.data.redis.cluster.nodes=10.96.1.31:6373,10.96.1.32:6374,10.96.1.33:6375,10.96.1.34:6376,10.96.1.35:6377,10.96.1.36:6378
spring.data.redis.ssl.enabled=true

# keytool -import -alias "redis" -file "redis-truststore.pem" -keystore "redis-truststore.jks" -storepass changeit -noprompt
# openssl pkcs12 -export -name redis -in redis-user.pem -out redis-user.p12
spring.ssl.bundle.jks.redisbundle.key.alias=redis

spring.ssl.bundle.jks.redisbundle.keystore.location=classpath:redis-user.p12
spring.ssl.bundle.jks.redisbundle.keystore.password=changeit
spring.ssl.bundle.jks.redisbundle.keystore.type=pkcs12

spring.ssl.bundle.jks.redisbundle.truststore.location=classpath:redis-truststore.jks
spring.ssl.bundle.jks.redisbundle.truststore.password=changeit
spring.ssl.bundle.jks.redisbundle.truststore.type=jks

spring.data.redis.ssl.bundle=redisbundle
```

Snippet above shows also how to generate a keystore and a truststore. Feel free to use a professional
PKI solution like EJBCA oder Hashicorp Vault PKI to provide a internal certificate service.
For server certificates you may consider to use Let's Encrypt certificates.

# OpenID/Connect

It does not make sense to support different IDPs by Alovoa itself. There are some Open Source IDP solutions,
which can IDP better than Alovoa, because these solutions are made for IDP. One recommended solution is
[Keycloak](https://www.keycloak.org/). Keycloak supports also delegation to many external OIDC and OAuth2 IDPs,
e.g. Github, Google, Microsoft and many others. **Warning: Never allow admin access to Alovoa authenticated
by external IDPs.** Hint: Configure Keycloak realm for multi factor authentication for Alovoa admins. Alovoa
admins needs Realm role *AlovoaAdmin*. At *Users* ⇒ *&lt;admin user&gt;* ⇒  *User details* ⇒  *Role mapping* add
*AlovoaAdmin*. Now this user has admin entitlement in Alovoa.

## Architecture change proposal

Alovoa should no longer provide local authentication/authorization but should delegate it to an OpenID/Connect
or OAuth2 solution. For non IDP experts: OpenID/Connect depends on OAuth2. Local registration and login should
be disabled. For now this fork makes local login/registration configurable. Check

```
app.local.login.enabled=true
```

If *true* Alovoa support local login/registration.

# Donations

Donation function is configurable with

```
donate.enabled
```

# Chat

Alovoa provides an internal chat solution. This fork adds Matrix client support with Element as alternative
chat solution. Element needs a Matrix server e.g. Synapse. This combination makes sense only if 
single sign on is implemented for Matrix server and Alovoa. Keycloak is a good choice here.

# Status

* RedisConfiguration class is able to connect to a Redis cluster now
* Single host is not tested yet
* Save and retrieve key/value pair works
* It uses mTLS to connect Redis. For obvious reasons Redis cluster must be configured for mTLS, also. For security reasons it is strongly recommended to use mTLS.
* Redis credentials are stored in vault.
* Private key is still in resorces folder - this is a security flaw. ToDo: Move to vault.
* Admin login possible by private OIDC IDP.

