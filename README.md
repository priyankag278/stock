# Stock Matching Service
A spring boot based order matching service based on price and time matching algorithm.

## Solution is based on following principles:
  
  1. Every incoming order whose transaction type is sales will be matched with existing buy orders of same stock 
   symbol and vice-versa.
  2. For every new buy order, sales order with least amount will be given priority. If there are more than
  one order matched for that amount, priority will be given to order with earlier timestamp.(FIFO).
  3. For every new sales order, buy order with max amount will be given priority. If there are more than 
  one order matched for that amount, priority will be given to order with earlier timestamp. (FIFO).
  4. Currently, to handle different date & timezone, we have taken ZonedDatetime(java.time.ZonedDateTime) instead of only timestamp.
  5. New order will be full filled according to the quantity, only if there are suitable matching orders.
  
## Implementation: 
  1. The problem has been realised as a HTTP REST services, with endpoints for book new orders.
  2. Possible validations for order booking is taken care.
  3. Handle custom exceptions with user friendly error messages to achieve api to fail early in case of invalid inputs.
  3. Added unit test cases to cover individual component controller, service , exceptions etc.
  4. Code coverage report can be seen using jacco plugin.
  
## TradeBookApplication

  Api Definition:
  --------------
    
  POST `http://localhost:8080/stock/book/order`  --> endpoint for creating new order.
  Request body: 
  `{
       "orders" : [
           {
               "orderId" : 1,
               "createdAt" : "2022-10-23T09:45:00Z",
               "stockSymbol": "BAC",
               "txnType":"sell",
               "quantity":100,
               "price":"240.10"
           },
            {
               "orderId" : 2,
               "createdAt" : "2022-10-23T09:45:00Z",
               "stockSymbol": "BAC",
               "txnType":"sell",
               "quantity":90,
               "price":"237.45"
           }
  }`
   
   Response object will have processed order based on price matching criteria.