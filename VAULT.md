# Hashicorp Vault

How to set up for Alovoa

docker-compose.yml

```
version: '3.7'

services:
  vault:
    hostname: vault
    build:
      context: .
    volumes:
      - ./data:/vault:cached
    ports:
      - 8200:8200
    extra_hosts:
      - "mysql:172.17.0.1"
      - "vault.felsing.net:127.0.0.1"
    logging:
      driver: json-file
```

Dockerfile

```
FROM debian:bookworm-slim

run DEBIAN_FRONTEND=noninteractive apt-get update
run DEBIAN_FRONTEND=noninteractive apt-get install -q -y apt-utils
run DEBIAN_FRONTEND=noninteractive apt-get install -q -y \
  locales

RUN sed -i -e 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen && \
    locale-gen
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8

run DEBIAN_FRONTEND=noninteractive apt update && apt -q -y full-upgrade

# Install Postfix.
run echo "postfix postfix/main_mailer_type string Internet site" > preseed.txt
run echo "postfix postfix/mailname string ${fqname}" >> preseed.txt
# Use Mailbox format.
run debconf-set-selections preseed.txt
run DEBIAN_FRONTEND=noninteractive apt-get install -q -y \
  postfix

run DEBIAN_FRONTEND=noninteractive apt-get install -q -y \
  procps \
  cron \
  curl \
  lsb-release \
  gnupg2

ARG hostname=vault
ARG domain=example.com
ARG fqname=${hostname}.${domain}

RUN \
  groupadd --gid 90000 vault && \
  useradd -d /vault -g vault -s /bin/bash --uid 90000 vault

COPY ./mk-crt /usr/local/bin/mk-crt
RUN \
  chmod 755 /usr/local/bin/mk-crt

run \
  cd /tmp && \
  curl -LsS https://apt.releases.hashicorp.com/gpg | gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg && \
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | tee /etc/apt/sources.list.d/hashicorp.list && \
  DEBIAN_FRONTEND=noninteractive apt update && \
  DEBIAN_FRONTEND=noninteractive apt install -q -y vault

COPY ./entrypoint.sh /entrypoint.sh
RUN \
  chown vault:vault /entrypoint.sh && \
  chmod 700 /entrypoint.sh

USER vault
ENTRYPOINT [ "/entrypoint.sh" ]
```

entrypoint.sh:

```
#!/usr/bin/env bash

if [ ! -f "/vault/cert.crt" ]; then
  cd /vault
  CRT_DAYS="3650" /usr/local/bin/mk-crt --rsa --mod 4096 --ip 127.0.0.1 --host vault.example.com vault.example.com
  mv vault.example.com.crt cert.crt
  mv vault.example.com.key cert.key
  cd ~
fi

/usr/bin/vault server -config /vault/vault.conf
```

You may consider to use official vertificates for Hashicorp Vault. Spring Cloud Vault is a nightmare
if using self signed certificates.

Create a file ./data/vault.conf

```
ui            = true
cluster_addr  = "https://0.0.0.0:8201"
api_addr      = "https://0.0.0.0:8200"
disable_mlock = true

storage "file" {
  path = "/vault/hashicorp"
}

listener "tcp" {
  address       = "0.0.0.0:8200"
  tls_cert_file = "/vault/cert.crt"
  tls_key_file  = "/vault/cert.key"
}
```

Now you have a https Hashicorp Vault. After start you need to set up Hashicorp Vault. While set up it
creates a file named like *vault-cluster-vault-2023-08-14T06 48 11.267Z.json*.

**You must save this file, otherwise you cannot unseal your Vault**

Copy file to private/vault-credentials.json

Following script unseals your Vault:

```
#!/usr/bin/env bash

for i in 0 1 2; do
  KEY=$(cat private/vault-credentials.json | jq ".keys[$i]" | sed 's/"//g')
  docker compose exec vault vault operator unseal -address=https://vault.felsing.net:8200 -ca-cert=/vault/ca.crt ${KEY}
done
```

Edit docker-compose.yml extra_host and this script matching your Let's Encrypt certificate.

# MySQL

Create MySQL user for Vault. This is an administrative user, keep credentials really secret.
Your application do not need that credentials.

```
CREATE USER 'vault'@'%' IDENTIFIED BY 'A*bad*Password';
GRANT ALL PRIVILEGES ON *.* TO 'vault'@'%' WITH GRANT OPTION;
```

# Hashicorp Vault

```
vault write database/config/alovoa \
  plugin_name=mysql-database-plugin \
  connection_url="{{username}}:{{password}}@tcp(mysql)/" \
  allowed_roles="mysqlrole" \
  username="vault" \
  password="A*bad*Password"
```

```
vault write database/roles/mysqlrole \
  db_name=alovoa \
  creation_statements="CREATE USER '{{name}}'@'%' IDENTIFIED BY '{{password}}'; GRANT ALL PRIVILEGES ON alovoa.* TO {{name}}'@'%';" \
  default_ttl="1h" \
  max_ttl="24h"
```

Test it, you should get temporary MySQL credentials.

```
vault read database/creds/mysqlrole
```

Set up a Vault policy.

```
./vault policy write alovoa-policy - << EOF
path "database/creds/mysqlrole" {
  capabilities = ["read"]
}
path "oauth-idp/ip6li" {
  capabilities = ["read"]
}
path "oauth-idp/google" {
  capabilities = ["read"]
}
path "oauth-idp/facebook" {
  capabilities = ["read"]
}
path "alovoa/creds" {
  capabilities = ["read"]
}
EOF
```

Now create a Token for Alovoa database.

```
vault token create -policy=alovoa-policy
Key                  Value
---                  -----
token                hvs.---some cryptic chars---
token_accessor       ---some cryptic chars---
token_duration       768h
token_renewable      true
token_policies       ["alovoa-policy" "default"]
identity_policies    []
policies             ["alovoa-policy" "default"]
```

You need "token" for Alovoa application.

# OAuth2 Secrets

```
REMOTE_URL="-address=https://vault.example.com:8200"
vault secrets enable ${REMOTE_URL} -path=vault-idp kv
vault kv put ${REMOTE_URL} -mount=vault-idp ip6li client-id=oauthClientId client-secret=oauthClientSecret

vault kv put ${REMOTE_URL} -mount=vault-idp google client-id=oauthClientId client-secret=oauthClientSecret

vault kv put ${REMOTE_URL} -mount=vault-idp facebook client-id=oauthClientId client-secret=oauthClientSecret
```

# Other Secrets

```
./vault secrets enable -path=alovoa kv

./vault kv put -mount=alovoa creds \
  app.text.key=*** app.text.salt=*** app.admin.key=*** app.admin.email=alovoa@example.com spring.mail.username=alovoa@example.com spring.mail.password=*** app.login.remember.key=*** app.vapid.public=*** app.vapid.private=***
```
