# Unified Push Provider (XMPP Transport)

This is a [Unified Push](https://unifiedpush.org/) Provider that uses XMPP to talk to a Push Distributor ([Conversations](http://codeberg.org/iNPUTmice/Conversations)).

### Compile & run
```shell
git clone https://codeberg.org/iNPUTmice/up.git
cd up
./gradlew shadowJar
java -jar build/libs/up-0.1-all.jar -c config.json
```

### Configuration
`config.json` should be fairly self explanatory. The component section matches the component configuration of your XMPP server. `hostname` will be localhost in most cases (if XMPP server and this server run on the same machine). `jid` is the external name.

`registration.sharedSecret` is used to encrypt tokens in the endpoint URL. A random secret can be generated with `openssl rand -base64 32`.

## Protocol

The avid reader should familiarize themselves with the [definitions](https://unifiedpush.org/spec/definitions/) and the general architecture of [UnifiedPush](https://unifiedpush.org/) before continue reading here.

### Registration
The [UnifiedPush specification](https://unifiedpush.org/developers/intro/) defines an endpoint as *the URL of [...] the Push Server where push messages are sent to for a specific end user Application, from the Push Gateway.*

Conversations retrieves the *endpoint* from the Push Provider by sending a registration to it.

The client registers with the Push Provider.
```xml
<iq type="set" to="up.conversations.im" from="juliet@example.com/Conversations.r4nD">
  <register instance="EtD/1XPLmoIph7W73CZ/uyv6FAnhEAXY1GySHX17vAw=" application="bK3sdmrbtj3bf6XA2S12b8jJUA5TT1dhPEyeoZfVSRM=" xmlns="http://gultsch.de/xmpp/drafts/unified-push"/>
</iq>
```

*application* and *instance* is terminology borrowed from the UnifiedPush specification. (Application being self explanatory and *instance* (or sometimes *token*) being used because one app might need multiple endpoints for mulitple accounts.)

As far as the Push Provider is concerned both could be opague strings but Conversations uses salted, base64 encoded SHA256 hashes of the application id and the instance (token). It is RECOMMENDED that the Push Provider enforces this convention.

This will also restrict application and instances to known length.

The Push Provider responds with the endpoint if the registration was successful.

```xml
<iq type="result" xml:lang="en" xmlns="jabber:client" id="obCcWXk37WZE" from="up.conversations.im" to="juliet@example.com/Conversations.r4nD">
  <registered expiration="2023-01-05T12:54:54.036Z" endpoint="https://up.conversations.im/push/v2.local.8r50Ti5V6ZNqb_5Z38olNF0gQiGak2CgBYo3WMBy0ZMM-wCHajxO-Zz3iVgYOiW2VIyu-3TJZ77jsxYIw8aMVhljPd1iC8J5QrP6HR-dIqj9t1O2gKqgE0jvfvhDA81zi5DSec2okpwXiqlkoF8hnq1Jm6kqdmUzTlR5x-xGKpAuJBpOvl3AeR74fRDf0211hgTzPELql1B2My34LORNu9qGg-xXwx94JdtK3rxWBmoCMxDQ82DQB2Lb5WSRWQv_q52M41XafPQ3Jy35t0Mi6Ufk-MPUqNuCiC9nRjqJU7gbjnI" xmlns="http://gultsch.de/xmpp/drafts/unified-push"/>
</iq>

```

The endpoint (URL) is only valid until it's expiration. After that the client has to (automatically) renew it’s registration.

The Push Provider is stateless if the full XMPP address of the user (client that registered the endpoint), the application, the instance and the expiration are endcoded in the endpoint URL (via JWT or simliar).

### Push
When a push occurs, this is when an Application Server (Push Gateway) POSTs a payload to the endpoint, the Push Provider relays the payload by sending push element wrapped by an IQ-set to the client.

```xml
<iq type="set" id="0279096a-26c5-41e4-b83b-ca47be278184" from="up.conversations.im" to="juliet@example.com/Conversations.r4nD">
  <push instance="EtD/1XPLmoIph7W73CZ/uyv6FAnhEAXY1GySHX17vAw=" application="bK3sdmrbtj3bf6XA2S12b8jJUA5TT1dhPEyeoZfVSRM=" xmlns="http://gultsch.de/xmpp/drafts/unified-push">dGl0bGU9VGVzdCZtZXNzYWdlPVdpdGgrVW5pZmllZFB1c2gmcHJpb3JpdHk9NSY=</push>
</iq>
```
The payload is the base64 encoded raw body of the POST.

If the client has reasonably ensured that the push message has been relayed to the application it will respond with an empty `<iq type="result"/>` to the Push Provider.
```xml
<iq type="result" id="0279096a-26c5-41e4-b83b-ca47be278184" from="juliet@example.com/Conversations.r4nD" to="up.conversations.im"/>
```

If the receiving application has been uninstalled or if the application has unregistered itself from the Push Distributor the client will respond with in IQ-error `item-not-found`. When receiving an `item-not-found` as a response to a push the Push Provider should consider the endpoint as revoked. (Stateless providers might need to add the endpoint to a revocation list until it expires naturally.)

In the unlikely event of a `feature-not-implemented` response the Push Provider SHOULD also consider the endpoint as revoked. All other IQ errors (especially `service-unavailable`) SHOULD be considered temporary.

### HTTP Endpoint

The HTTP endpoint is a *push resource* according to [RFC 8030 §5](https://www.rfc-editor.org/rfc/rfc8030#section-5) with a somewhat reduced feature set.

### Reliability & Retry Behaviour

The Push section of this specification defines which error conditions should trigger endpoint revocation but is deliberately vague about what to do with the push message itself on other error conditions.

The push server MAY choose to store the message and attempt to deliver it again when the client comes back online.

To let the push server know it came online the client SHOULD send a direct presence on resource bind when it has more than zero endpoints registered with the push server.

A push server that implements storage and multiple delivery attempts SHOULD implement the `TTL` and `Topic` headers from RFC 8030. According to RFC 8030 §5.2 *a push service MAY retain a push message for a shorter duration than requested*; since this value can be 0 a push server that does not store messages is still a valid WebPush service. Push servers that do not implement storage MUST include a `TTL: 0` header in the response.
