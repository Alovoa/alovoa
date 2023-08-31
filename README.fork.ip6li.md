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

# Redis

**Do not use yet**

## Why?

* Redis scales much better than MariaDB or Postgresql.
* Redis cluster is not transaction aware, but this is mostly not required for Alovoa.
* Primary usecase: Save user images or videos in Redis instead of MariaDB.

# OpenID/Connect

It does not make sense to support different IDPs by Alovoa itself. There are some Open Source IDP solutions,
which can IDP better than Alovoa, because these solutions are made for IDP. One recommended solution is
[Keycloak](https://www.keycloak.org/). Keycloak supports also delegation to many external OIDC and OAuth2 IDPs,
e.g. Github, Google, Microsoft and many others. **Warning: Never allow admin access to Alovoa authenticated
by external IDPs.** Hint: Configure Keycloak realm for multi factor authentication to Alovoa admin membership.

## Architecture change proposal

Alovoa should no longer provide local authentication/authorization but should delegate it to an OpenID/Connect
or OAuth2 solution. For non IDP experts: OpenID/Connect depends on OAuth2. Local registration and login should
be disabled.

# Status

* RedisConfiguration class is able to connect to a Redis cluster now
* Single host is not tested yet
* Save and retrieve key/value pair works
* It uses mTLS to connect Redis. For obvious reasons Redis cluster must be configured for mTLS, also. For security reasons it is strongly recommended to use mTLS.
* Redis credentials are stored in vault.
* Private key is still in resorces folder - this is a security flaw. ToDo: Move to vault.
* Admin login possible by private OIDC IDP.

