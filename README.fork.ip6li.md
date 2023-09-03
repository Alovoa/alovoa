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
You should consider to save at least private key in vault.

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

## Vault Policy

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

## Vault Token

Now generate a dedicated token for Alovoa.

```
vault token create -policy=alovoa-policy
```

Save output of that command.
