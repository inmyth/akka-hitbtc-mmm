## Notes

#### Order Cancelation
- only two things can remove an item by non filled : cancel and insufficient fund

#### ClientOrderId and Id
ClientOrderId is set by client and maps to order and its activities (new, filled, partiallyFilled, etc), and has to be unique.

Id can be set by client or HitBTC and it's optional. It is associated with a request (uniqueness is not needed)

ClientOrderId only comes back in response if the request is successful. Id comes back if request is successful or not. 

Use Id to intercept submission failure (not enough fund)