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
- [] check all senders()
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
- [] add max min price