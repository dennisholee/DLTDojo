# Simple API to Connect to XRP

Endpoint: GET /wallets

Endpoint: POST /wallets/{{payeeAcc}
Payload:
```
{
    "action": "top-up",
    "account": "rEc38Qw6eeKGc3i8wK4pEAAjCEK1XpuzC9",
    "amount": 1000,
    "balance": null
}
```

Endpoint: POST /accounts
Payload:
```
{
    "account" : "{{payeeAcc}}"
}
```

Endpoint: POST /payments
Payload:
```
{
    "transactionType":"type",
    "account":"{{payeeAcc}}",
    "amount":"10",
    "destination": "{{beneAcc}}"
}
```

# Links
XRP Devnet Explorer: https://devnet.xrpl.org
XRP Public Servers: https://xrpl.org/public-servers.html
