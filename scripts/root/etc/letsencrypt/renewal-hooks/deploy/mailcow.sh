#!/bin/bash
cp /etc/letsencrypt/live/alovoa.com/fullchain.pem /opt/mailcow-dockerized/data/assets/ssl/cert.pem
cp /etc/letsencrypt/live/alovoa.com/privkey.pem /opt/mailcow-dockerized/data/assets/ssl/key.pem
cp /etc/letsencrypt/live/alovoa.com/fullchain.pem /opt/mailcow-dockerized/data/assets/ssl/mail.alovoa.com/cert.pem
cp /etc/letsencrypt/live/alovoa.com/privkey.pem /opt/mailcow-dockerized/data/assets/ssl/mail.alovoa.com/key.pem
postfix_c=$(docker ps -qaf name=postfix-mailcow)
dovecot_c=$(docker ps -qaf name=dovecot-mailcow)
nginx_c=$(docker ps -qaf name=nginx-mailcow)
docker restart ${postfix_c} ${dovecot_c} ${nginx_c}


