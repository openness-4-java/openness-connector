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
 
ssh -L 7080:10.16.0.32:80 -i ~/.ssh/mp_personal -p 5766 root@93.146.227.218
