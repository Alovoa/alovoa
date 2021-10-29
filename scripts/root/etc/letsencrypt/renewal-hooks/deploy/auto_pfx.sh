#!/bin/bash
# Adjust these variables as necessary

# Where you want to final PKCS12 file to be stored.
CERT_PATH="/opt/alovoa/alovoa.pfx"

# Password to encrypt the PKCS12 file.
CERT_PW="your-password-here"

# Path to LE files, RENEWED_LINEAGE provided by CertBot
PRIV_KEY_PEM="$RENEWED_LINEAGE/privkey.pem"
CERT_PEM="$RENEWED_LINEAGE/cert.pem"
CHAIN_PEM="$RENEWED_LINEAGE/chain.pem"

# If there's already a .pfx file, back it up
if [[ -f "$CERT_PATH" ]]; then
    now=`date +%Y-%m-%d-%T`
    mv $CERT_PATH $CERT_PATH.bak.$now
fi

# Le Conversion
openssl pkcs12 -export -out $CERT_PATH -inkey $PRIV_KEY_PEM -in $CERT_PEM -certfile $CHAIN_PEM -password pass:$CERT_PW

