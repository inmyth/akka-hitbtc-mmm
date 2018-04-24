# HitBtc Market Maker

Market making bot with ping-pong based strategy.

Whenever the bot starts, it will seed the orderbook using ticker's last offer as starting price.

If there are existing orders in the account, these orders will be canceled and recreated as ping orders.
The price will follow the recalculated grids but the quantity will continue from the last order left in the orderbook.

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

**pair (String)**

Currency symbol

**gridSpace` (String)**

Price level between orders, behavior determined by strategy. 

**buyGridLevels and sellGridLevels  (int)**

Number of seed orders for buy and sell sides respectively. 

**buyOrderQuantity and sellOrderQuantity (String)**

Amount of seed and counter order.

**quantityPower (int)**

Only used in **ppt**. Rate of quantity appreciation / depreciation.  Denoted as (sqrt(gridSpace))^ quantityPower.

**counterScale  and baseScale (int)**

Scale or number of digits to the right of decimal point for counter / base currency.

Examples:

XRP : minimum quantity = 1 XRP -> scale = 0

ETH : minimum quantity = 0.001 ETH -> scale = 3

DOGE : minimum quantity = 1000 DOGE -> scale = -3

BTC = 9

ETH = 8

USDT = 7

**isHardReset (boolean)**

If true then when the bot starts it will remove all orders and seed new orders with recalculated middle price. This method will clear any holes (missing levels) but lose all ping/pong information from the old orders.

If false then the bot will only fill the hole closest to market middle price. This will preserve the ping/pong info of each order but not fill all possible holes.

**isStrictLevels (boolean)**

If true then number of orders is kept according to buyGridLevels pr sellGridLevels by removing ping (seed / balancer) orders. WARNING : this may cause holes in orderbook.

**isNoQtyCutoff (boolean)**

If true then order's quantity will never become zero. Instead it will be replaced with the currency's minimum amount.

**(Optional) maxPrice and minPrice (String)**

Maximum and minimum price the bot will operate on

**strategy (String)**

Strategy to be used. Refer to strategy section for valid names. 

## Strategies

### Proportional `ppt`

Both base quantity and unit price are spaced by percentage point of the previous offerCreate level.

For sell direction p1 = (1 + gridSpace / 100) * p0 and q1 = q0 / ((1 + gridSpace / 100)^0.5)^quantityPower

For buy direction p1 = p0  / (1 + gridSpace / 100) and q1 = q0 * ((1 + gridSpace / 100)^0.5)^quantityPower

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
