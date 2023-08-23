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

