package stock.order.matching.services;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.rest.core.util.MapUtils;
import stock.order.matching.model.StockOrder;
import stock.order.matching.model.TradeOrder;
import stock.order.matching.model.TxnType;
import stock.order.matching.model.request.TradeRequest;
import stock.order.matching.repository.OrderMatchingService;

@Slf4j
public class OrderMatchingServiceImpl implements OrderMatchingService {

  //  @Override
  //  public Map<String, Map<TxnType,PriorityQueue<StockOrder>>> rearrangeTradeByPriceAndStockSymbol(TradeRequest request) {

//        Map<String,List<StockOrder>> stockMapBySymbol = filterOrdersByStockSymbole(request);
//        Map<String, Map<TxnType,PriorityQueue<StockOrder>>> stocksMap = new LinkedHashMap<>();
//
//        for (Map.Entry<String,List<StockOrder>> entry: stockMapBySymbol.entrySet()) {
//
//            Map<TxnType,List<StockOrder>> stocksByTxnType = filterOrdersByTxnType(entry.getValue());
//            Map<TxnType,PriorityQueue<StockOrder>>  sellMap = new HashMap<>();
//            Map<TxnType,PriorityQueue<StockOrder>>  buyMap = new HashMap<>();
//
//           // PriorityQueue<StockOrder> sellQueue = sortStocksByPriceAndTime(TxnType.SELL,stocksByTxnType.get(TxnType.SELL).size(),null,stocksByTxnType.get(TxnType.SELL));
//            //PriorityQueue<StockOrder> buyQueue = sortStocksByPriceAndTime(TxnType.BUY,null,stocksByTxnType.get(TxnType.BUY).size(),stocksByTxnType.get(TxnType.BUY));
//
//            sellMap.put(TxnType.SELL,sellQueue);
//            buyMap.put(TxnType.BUY,buyQueue);
//
//            stocksMap.put(entry.getKey(),sellMap);
//            stocksMap.put(entry.getKey(),buyMap);
//
//        }
//        return stocksMap;
    //}


    private Map<String,List<StockOrder>> filterOrdersByStockSymbole(TradeRequest request){
        Map<String,List<StockOrder>> stocksBySymbol = request.getOrders().stream().collect(Collectors.groupingBy(StockOrder::getStockSymbol));
        return stocksBySymbol;
    }

    private Map<TxnType,List<StockOrder>> filterOrdersByTxnType(List<StockOrder> orders){
        Map<TxnType,List<StockOrder>> stocksByTxnType = orders.stream().collect(Collectors.groupingBy(StockOrder::getTxnType));
        return stocksByTxnType;
    }

//    @Override
//    public List<TradeOrder> executeMatchEngine(Map<String, Map<TxnType,PriorityQueue<StockOrder>>> stocksMap){
//        //1.if buy is not empty
//        //2. pop sell queue queue
//
//        List<TradeOrder> todayTrades = new ArrayList<>();
//
//        for (Map.Entry<String, Map<TxnType,PriorityQueue<StockOrder>>> trades: stocksMap.entrySet()) { // for each stock symbol
//            PriorityQueue<StockOrder> sellerPQ = trades.getValue().get(TxnType.SELL);
//            PriorityQueue<StockOrder> buyerPQ = trades.getValue().get(TxnType.BUY);
//
//            while (!buyerPQ.isEmpty() && !sellerPQ.isEmpty()) {
//                StockOrder buyer = buyerPQ.peek();
//                StockOrder seller = sellerPQ.peek();
//
//                if(buyer.getPrice().compareTo(seller.getPrice()) > 0) {
//                    if(buyer.getQuantity()<seller.getQuantity()){
//                        buyerPQ.poll();
//                        seller.setQuantity(seller.getQuantity()-buyer.getQuantity());
//                    }
//                    else if(buyer.getQuantity().equals(seller.getQuantity())) {
//                        buyerPQ.poll();
//                        sellerPQ.poll();
//                    }
//                    else {
//                        sellerPQ.poll();
//                    }
//                    TradeOrder tradeOrder = TradeOrder.builder()
//                            .sellerOrderId(seller.getOrderId())
//                            .price(seller.getPrice())
//                            .quantity(seller.getQuantity()<=buyer.getQuantity() ? seller.getQuantity() : seller.getQuantity()-buyer.getQuantity())
//                            .buyerOrderId(buyer.getOrderId())
//                            .build();
//                    todayTrades.add(tradeOrder);
//                }
//                else {
//                    // no match available all buyer too lower , no sell can happen
//                    break;
//                }
//            }
//        }
//        return todayTrades;
//    }

    @Override
    public List<TradeOrder> executeMatchEngine(TradeRequest tradeRequest){
        List<StockOrder> stockOrders = tradeRequest.getOrders();

        Map<String,  PriorityQueue<StockOrder>> perStockSell = new LinkedHashMap<>();
        Map<String,  PriorityQueue<StockOrder>> perStockBuy = new LinkedHashMap<>();

        PriorityQueue<StockOrder>  ordersToSell  = new PriorityQueue<>(stockOrders.size(), (order1, order2) -> {
            if (order1.getPrice().compareTo(order2.getPrice()) == 0) {  // if same price whoever comes first
                return order1.getCreateAt().compareTo(order2.getCreateAt());
            }
            return order1.getPrice().compareTo(order2.getPrice()); // Ascending order of prices
        });

        PriorityQueue<StockOrder>  ordersToBuy  = new PriorityQueue<>(stockOrders.size(), (order1, order2) -> {
            if (order2.getPrice().compareTo(order1.getPrice()) == 0) { // if same price whoever comes first
                return order1.getCreateAt().compareTo(order2.getCreateAt());
            }
            return order2.getPrice().compareTo(order1.getPrice()); // descending order of prices
        });

//        switch (txnType){
//            case SELL:
//                  ordersToSell  = new PriorityQueue<>(sellCapacity, (order1, order2) -> {
//                    if (order1.getPrice().compareTo(order2.getPrice()) == 0) {  // if same price whoever comes first
//                        return order1.getCreateAt().compareTo(order2.getCreateAt());
//                    }
//                    return order1.getPrice().compareTo(order2.getPrice()); // Ascending order of prices
//                });
//                ordersToSell.addAll(stockOrders);
//                return ordersToSell;
//            case BUY:
//                 ordersToBuy  = new PriorityQueue<>(buyCapacity, (order1, order2) -> {
//                    if (order2.getPrice().compareTo(order1.getPrice()) == 0) { // if same price whoever comes first
//                        return order1.getCreateAt().compareTo(order2.getCreateAt());
//                    }
//                    return order2.getPrice().compareTo(order1.getPrice()); // descending order of prices
//                });
//                ordersToBuy.addAll(stockOrders);
//                return ordersToBuy;
//        }

        List<TradeOrder> tradeOrders = new ArrayList<>();

        for (StockOrder newOrder: stockOrders) {
            String stockSymbol = newOrder.getStockSymbol();

            if(newOrder.getTxnType()==TxnType.BUY) {
                //1. any seller waiting
                if(perStockSell.containsKey(stockSymbol) && null!=perStockSell.get(stockSymbol)
                        && !perStockSell.get(stockSymbol).isEmpty()) {
                    StockOrder seller = perStockSell.get(stockSymbol).peek(); // so far most eligible seller , TOP seller
                    StockOrder buyer = newOrder;
                    PriorityQueue<StockOrder> buyQueue = perStockSell.getOrDefault(stockSymbol,ordersToBuy);

                    if(null!=seller && buyer.getPrice().compareTo(seller.getPrice())>=0){
                        if(buyer.getQuantity()<seller.getQuantity()) {
                            seller.setQuantity(seller.getQuantity()-buyer.getQuantity());
                            //no need to add buyer in the queue, buyer exhausted
                            //keep seller in queue for coming buyer
                        }else if(buyer.getQuantity()>seller.getQuantity()){
                            ordersToSell.poll(); // take out seller , as sold this order

                            Integer remainingQty = buyer.getQuantity()-seller.getQuantity();
                            buyer.setQuantity(remainingQty);

                            buyQueue.offer(buyer); // add new buyer in queue
                            perStockBuy.putIfAbsent(buyer.getStockSymbol(),buyQueue); //
                        }else {
                            ordersToSell.poll();
                        }
                        tradeOrders.add(bookTrade(seller.getOrderId(),seller.getQuantity(),
                                seller.getPrice(),buyer.getOrderId()));
                    }else {
                        buyQueue.offer(buyer);
                        perStockBuy.putIfAbsent(buyer.getStockSymbol(),buyQueue);
                    }
                }else {
                    ordersToBuy.offer(newOrder);// add buyer in waiting
                    perStockBuy.put(newOrder.getStockSymbol(),ordersToBuy);
                }
            }
            else if(newOrder.getTxnType()==TxnType.SELL){

                if(perStockBuy.containsKey(stockSymbol) && null!=perStockBuy.get(stockSymbol)
                        && !perStockBuy.get(stockSymbol).isEmpty()) {
                    StockOrder buyer = perStockSell.get(stockSymbol).peek();
                    StockOrder seller = newOrder;
                    PriorityQueue<StockOrder> sellQueue = perStockSell.getOrDefault(stockSymbol,ordersToSell);

                    if(null!=buyer && seller.getPrice().compareTo(buyer.getPrice())<=0){
                        if(buyer.getQuantity()<seller.getQuantity()) {
                            seller.setQuantity(newOrder.getQuantity()-buyer.getQuantity());
                            //no need to add buyer in the queue
                            ordersToBuy.poll();// buyer is exhaust

                            sellQueue.offer(seller);
                            perStockSell.putIfAbsent(seller.getStockSymbol(),sellQueue);
                        }else if(buyer.getQuantity()>seller.getQuantity()){
                            Integer remainingQty = buyer.getQuantity()-seller.getQuantity();
                            buyer.setQuantity(remainingQty);
                            //no need to add seller in queue as seller is done
                            // and buyer still waiting
                        }else { // equal remove buyer
                            ordersToBuy.poll();
                        }
                        tradeOrders.add(bookTrade(seller.getOrderId(),seller.getQuantity(),
                                seller.getPrice(),buyer.getOrderId()));
                    }else {
                        sellQueue.offer(seller); // add for future buyerr
                        perStockSell.putIfAbsent(seller.getStockSymbol(),sellQueue);
                    }
                }else { // new stock symbol started trades
                    ordersToSell.offer(newOrder);// add seller in waiting
                    perStockSell.put(newOrder.getStockSymbol(),ordersToSell);
                }
            }
        }

        return tradeOrders;
    }

    private TradeOrder bookTrade(BigInteger sellerId, Integer quantity, BigDecimal price ,BigInteger buyerId ){
        return TradeOrder.builder()
                .sellerOrderId(sellerId)
                .quantity(quantity)
                .price(price)
                .buyerOrderId(buyerId)
                .build();
    }
}
