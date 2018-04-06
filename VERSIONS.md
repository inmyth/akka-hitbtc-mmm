1.6 in progress


1.5.2 interim
-



1.5.1
- websocket onSendError and onError should return -1

1.5
- added email reporting, working on email send - shutdown future
- actorized error handling in MainActor
- updated gitigore (*.log)
- updated readme (email config)
- updated Config
- fixed assembly deduplication error between slf4j and commons-logging. https://stackoverflow.com/a/44078655/1014413

Add this on `build.sbt`
```
excludeDependencies += "commons-logging" % "commons-logging"
```

1.4
- added min max prices
- enumerated Side
- fixed shutdown mechanism

1.3
- enumerated strategies
- added program argument for log dir
- provided init for MyLoggingSingle
- updated readme
- updated dependencies


1.2
- added full strategy
- refactored Strategy
- updated test

1.1
- used floor and ceil for ppt
- added qtyScale on bot config
- displayed clientOrderId on log orderbook
- updated tests

1.0
- ppt version
- added sbt-assembly

0.5
- test run

0.4
- working version ppt with seed and counter
- all seeds will go to main map
- added java.utils.Logging
- fixed Readme

0.3
- refactored sequences to actors

0.2
- seed strategy implemented

0.1
- config reader
- websocket connection

## TODO
- [x] test run ppt
- [x] handle not enough fund response
- [x] handle smaller than minimum trade price response
- [x] seed will start after init
- [x] each order consumed will trigger counter and balancer
- [x] get sample multiple filled orders (params object or array)
- [x] get sample filled order
- [x] get sample partially-filled order
- [x] commonalize ppt strategy
- [x] added minimum trade quantity
- [x] update config and readme for minimum trade quantity
- [x] implement rounding over min trade quantity
- [x] implement full strategy
- [x] limit by max min prices
- [x] enumerate side
- [] check all senders()
- [x] aws email sender
- [x] ws listeners handling
- [x] need to check email sender on small errors
- [x] assembly deduplication error
- [x] clean up warnings
- [] test reconnect after server down
- [] base needs scale
- [x] estimate midPrice from any order in the orderbook
- [x] generate orders from order zero
- [x] ppt supports amtPower
- [] seed one side : reconstruct only orders close to spread
- [] add amtPower : Int in botConfig
- [] initial seed amount must also be different on each price level
- [] keep number of orders according to config
- [] merge emails
- [x] changed stateActor to orderbookActor InitOrders so even empty orders will call orderbook
- [] seed : reconstruct new seed from a single order, cancel all existing orders (safe for BigDecimal comparison, will deprioritize new orders)
- [] seed : if side is not empty, only refill the hole close to spread (optimal but cannot cover holes between orders)
- [x] get tick after orders are initiated
- [] after tick, seed