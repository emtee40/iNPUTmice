# Unified Push Provider (XMPP Transport)

This is a [Unified Push](https://unifiedpush.org/) Provider that uses XMPP to talk to a Push Distributor.

### Compile & run
```shell
git clone https://codeberg.org/iNPUTmice/up.git
cd up
./gradlew shadowJar
java -jar build/libs/up-0.1-all.jar -c config.json
```

### Protocol

The UnifiedPush specification defines an endpoint as *the URL of the [...] the Push Server where push messages are sent to for a specific end user Application, from the Push Gateway.*

Conversations retrieves the *endpoint* from the Push Provider by sending a registration to it.

The client registers with the Push Provider.
```xml
<iq type="set" to="up.conversations.im" from="juliet@example.com/Conversations.r4nD">
  <register instance="EtD/1XPLmoIph7W73CZ/uyv6FAnhEAXY1GySHX17vAw=" application="bK3sdmrbtj3bf6XA2S12b8jJUA5TT1dhPEyeoZfVSRM=" xmlns="http://gultsch.de/xmpp/drafts/unified-push"/>
</iq>
```

The Push Provider responds with the endpoint if the registration was successful.

```xml
<iq type="result" xml:lang="en" xmlns="jabber:client" id="obCcWXk37WZE" from="up.conversations.im" to="juliet@example.com/Conversations.r4nD">
  <registered expiration="2023-01-05T12:54:54.036Z" endpoint="https://up.conversations.im/push/v2.local.8r50Ti5V6ZNqb_5Z38olNF0gQiGak2CgBYo3WMBy0ZMM-wCHajxO-Zz3iVgYOiW2VIyu-3TJZ77jsxYIw8aMVhljPd1iC8J5QrP6HR-dIqj9t1O2gKqgE0jvfvhDA81zi5DSec2okpwXiqlkoF8hnq1Jm6kqdmUzTlR5x-xGKpAuJBpOvl3AeR74fRDf0211hgTzPELql1B2My34LORNu9qGg-xXwx94JdtK3rxWBmoCMxDQ82DQB2Lb5WSRWQv_q52M41XafPQ3Jy35t0Mi6Ufk-MPUqNuCiC9nRjqJU7gbjnI" xmlns="http://gultsch.de/xmpp/drafts/unified-push"/>
</iq>

```

The endpoint (URL) is only valid until it's expiration. After that the client has to (automatically) renew it’s registration.

The Push Provider is stateless if the full XMPP address of the user (client that registered the endpoint), the application, the instance and the expiration are endcoded in the endpoint URL (via JWT or simliar).

When a push occurs, this is when an Application Server (Push Gateway) POSTs a payload to the endpoint, the Push Provider relays the payload by sending push element wrapped by an IQ-set to the client.

```xml
<iq type="set" xmlns="jabber:client" id="0279096a-26c5-41e4-b83b-ca47be278184" from="up.conversations.im" to="juliet@example.com/Conversations.r4nD">
  <push instance="EtD/1XPLmoIph7W73CZ/uyv6FAnhEAXY1GySHX17vAw=" application="bK3sdmrbtj3bf6XA2S12b8jJUA5TT1dhPEyeoZfVSRM=" xmlns="http://gultsch.de/xmpp/drafts/unified-push">dGl0bGU9VGVzdCZtZXNzYWdlPVdpdGgrVW5pZmllZFB1c2gmcHJpb3JpdHk9NSY=</push>
</iq>
```

The payload is the base64 encoded raw body of the POST.
