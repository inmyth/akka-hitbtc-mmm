# HitBtc Market Maker

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

**logSeconds (int)**

Interval in seconds of orderbook log

### Bots

**pair (String)**

Currency or IOU with base.baseIssuer/quote.quoteIssuer format.

**startMiddlePrice (String)**

Starting rate for seeder.

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


**strategy (String)**

Strategy to be used. Refer to strategy section for valid names. 

## Strategies

### Proportional `ppt`

In this mode both base quantity and unit price are spaced by percentage point of the previous offerCreate level.

For sell direction p1 = (1 + gridSpace / 100) * p0 and q1 = q0 / (1 + gridSpace / 100)^0.5

For buy direction p1 = p0  / (1 + gridSpace / 100) and q1 = q0 * (1 + gridSpace / 100)^0.5

Pay attention minimum quantity. Ideally minimum quantity should be smaller than (gridSpace / 100 * quantity)