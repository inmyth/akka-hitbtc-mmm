## HitBtc Market Maker



### Credentials

- only supports HS256
- use https://www.freeformatter.com/hmac-generator.html to generate signature (nonce -> string input)

### Config
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

Amount of seed and counter order. This value is used for any strategy beside *partial*

**strategy (String)**

Strategy to be used. Refer to strategy section for valid names. 

## Strategies

#### Proportional `ppt`

In this mode both base quantity and unit price are spaced by percentage point of the previous offerCreate level.

For sell direction p1 = (1 + gridSpace / 100) * p0 and q1 = q0 / (1 + gridSpace / 100)^0.5

For buy direction p1 = p0  / (1 + gridSpace / 100) and q1 = q0 * (1 + gridSpace / 100)^0.5

