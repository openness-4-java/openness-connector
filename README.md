# Java Connector for OpenNess API 

This connector maps available API on the OpenNess controller. Additional information about the available API are 
available at the following link: https://www.openness.org/developers#apidoc

Currently mapped APIs are the following: 

- Edge Application Authentication API - https://www.openness.org/api-documentation/?api=auth
- Edge Application API - https://www.openness.org/api-documentation/?api=eaa

# API Port Forwarding for Development 

You can also use port forwarding to connect to other ports.

The target example command is: 

```bash
ssh -p SSH_PORT -L LOCAL_PORT:10.10.20.32:TARGET_PORT USERNAME@NODE_IP
```

Then you can access the target service towards 127.0.0.1:LOCAL_PORT

In particular considering the target services the required forwarding are the following: 

## Edge Application Authentication API (Port 80 - HTTP)

```bash 
ssh -L 7080:10.16.0.32:80 -i ~/.ssh/mp_personal -p 5766 root@93.146.227.218
```

## Edge Application API (Port 443 - HTTPS)

```bash
ssh -L 7443:10.16.0.32:443 -i ~/.ssh/mp_personal -p 5766 root@93.146.227.218
```

## CURL Test Edge Application API With Certificate and Key

```bash
curl --cert certs/9de46fe885a6ca9a92cef5678751b5e4aa10045c4696efb365549dd86394d59b.crt --key certs/id_ec -k -v https://127.0.0.1:7443/services
```

```bash
curl --cert certs/9de46fe885a6ca9a92cef5678751b5e4aa10045c4696efb365549dd86394d59b.crt --key certs/id_ec --cacert certs/9de46fe885a6ca9a92cef5678751b5e4aa10045c4696efb365549dd86394d59b_ca_chain.crt -v https://127.0.0.1:7443/services
```

Change /etc/hosts file by adding the custom resolution to domain eaa.openness

```bash
curl --cert certs/9de46fe885a6ca9a92cef5678751b5e4aa10045c4696efb365549dd86394d59b.crt --key certs/id_ec --cacert certs/9de46fe885a6ca9a92cef5678751b5e4aa10045c4696efb365549dd86394d59b_ca_chain.crt -v https://eaa.openness:7443/services
```

Verify SSL Protocol Version

```bash
openssl s_client -connect eaa.openness:7443
```

## KeyTool Command Line

Reference Links:

- https://blog.behrang.org/2019/01/30/ssl-mutual-authentication-java.html
- https://discuss.aerospike.com/t/how-to-use-mutual-authentication-tls-mtls-in-java/7314

```bash
keytool -importcert -storetype jks -alias eaa.openness -keystore example.ca.jks -file certs/9de46fe885a6ca9a92cef5678751b5e4aa10045c4696efb365549dd86394d59b_ca_chain.crt -storepass changeit
```

```bash
cat certs/9de46fe885a6ca9a92cef5678751b5e4aa10045c4696efb365549dd86394d59b.crt certs/9de46fe885a6ca9a92cef5678751b5e4aa10045c4696efb365549dd86394d59b_ca_chain.crt certs/id_ec > example.client.chain.crt
```

```bash
openssl pkcs12 -export -in example.client.chain.crt -out example.client.chain.p12 -password pass:"changeit" -name example.client -noiter -nomaciter
```

```bash
keytool -list -keystore example.client.chain.p12 -storepass changeit
```

```bash
openssl pkcs12 -export -in certs/9de46fe885a6ca9a92cef5678751b5e4aa10045c4696efb365549dd86394d59b.crt -inkey certs/id_ec -out example.client.chain.p12 -name "DIPI-UniMore" -certfile certs/9de46fe885a6ca9a92cef5678751b5e4aa10045c4696efb365549dd86394d59b_ca_chain.crt
```

## Apache HTTP Library - References

Examples and Tutorials:

- Apache Http Client Examples - https://mkyong.com/java/apache-httpclient-examples/