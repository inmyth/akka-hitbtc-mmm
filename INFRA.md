## Infrastructure Migration


- tickers and market data (candled, etc) will be moved to outside the bot to dedicated exporter
- bot's amountPwr is technically a quantity boost factor that can be arbitrary. It doesn't have to be power(s) of root square of gridSpace.
- count the number of ping-pong (seed-counter)
- PPT should only have one quantity for both sides
- new bot config

```
    "bots" : [
        {
            "pair": {
                "name" : "NOAHBTC",
                "baseScale" : 10,
                "counterScale" : -3
            },
            "strategy" : {
                "name" : "ppt",
                "space" : "0.5",
                "quantity" : "5000",
                "quantityPower" : 2,
                "buyGridLevels": 2,
                "sellGridLevels": 2,
            },
            "grids" : {
                "isStrictLevels" : true,
                "isNoQtyCutoff" : true
            }
        },
        {
            "pair": {
                "name" : "XRPBTC",
                "baseScale" : 10,
                "counterScale" : 1
            },
            "strategy" : {
                "name" : "fullfixed",
                "space" : "2",
                "buyQuantity" : "5000",
                "sellQuantity" : "4000",
                "buyGridLevels": 2,
                "sellGridLevels": 2,
            },
            "grids" : {
                "isStrictLevels" : true,
                "isNoQtyCutoff" : true
            }
        }
    ]
```