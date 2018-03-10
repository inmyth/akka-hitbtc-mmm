# HitBtc Market Maker


To package
```
sbt assembly
```
(jar is in `/target`)

To run
```
java -jar program.jar <path to config file> <path to log directory>
```

## Config

### Credentials 
- only supports HS256

**pKey (String)**

API Key. Must provide permissions to
- Order book, History, Trading balance 
- Place/cancel orders 

**nonce (String)**

Random text

**signature (String)**

HS256 signature. Use https://www.freeformatter.com/hmac-generator.html to generate signature 
- nonce -> string input
- apiKey -> secret

### Env

**(Optional) Emails (String[]), sesKey and sesSecret (String)**

Emails to report to. Requires AWS SES to function.

`sesKey` and `sesSecret` are IAM credentials with SES sending email permission.

**logSeconds (int)**

Interval in seconds of orderbook log

### Bots

**pair (String)**

Currency symbol

**startMiddlePrice (String)**

Starting rate for seed

**gridSpace` (String)**

Price level between orders, behavior determined by strategy. 

**buyGridLevels and sellGridLevels  (int)**

Number of seed orders for buy and sell sides respectively. 

**buyOrderQuantity and sellOrderQuantity (String)**

Amount of seed and counter order.

**qtyScale (int)**
Scale of minimum quantity for base currency.

Example

XRP : minimum quantity = 1 XRP -> scale = 0

ETH : minimum quantity = 0.001 ETH -> scale = 3

DOGE : minimum quantity = 1000 DOGE -> scale = -3

**(Optional) maxPrice and minPrice (BigDecimal)**
Maximum and minimum price the bot will operate on

**strategy (String)**

Strategy to be used. Refer to strategy section for valid names. 

## Strategies

### Proportional `ppt`

Both base quantity and unit price are spaced by percentage point of the previous offerCreate level.

For sell direction p1 = (1 + gridSpace / 100) * p0 and q1 = q0 / (1 + gridSpace / 100)^0.5

For buy direction p1 = p0  / (1 + gridSpace / 100) and q1 = q0 * (1 + gridSpace / 100)^0.5

Pay attention minimum quantity. Ideally minimum quantity should be smaller than (gridSpace / 100 * quantity)

### Full-fixed `fullfixed`

The new unit price is spaced rigidly by gridSpace, e.g. if a buy order with price X is consumed then a new sell order selling the same quantity with price X + gridSpace will be created. If a sell order with price Y is consumed then a buy order with the same quantity and price Y - gridSpace will be created.
Fixed Full fullfixed


## Systemd
- A typical service configuration

```
[Unit]
Description=HITBTC BOT one
After=network.target
StartLimitIntervalSec=0

[Service]
Type=simple
Restart=on-abnormal
RestartSec=10
ExecStart=/usr/bin/java -jar /home/ubuntu/hitbtc/jars/akka-hitbtc-mmm-assembly-1.3.jar /home/ubuntu/hitbtc/bots/one/config.txt /home/ubuntu/hitbtc/bots/one/

[Install]
WantedBy=multi-user.target
```

- Create above file `/lib/systemd/system/hitbtcbot.service`

- Run it `systemctl restart hitbtcbot.service`
