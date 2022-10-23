package stock.order.matching.services;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.order.matching.model.StockOrder;
import stock.order.matching.model.TradeOrder;
import stock.order.matching.model.TxnType;
import stock.order.matching.model.request.TradeRequest;
import stock.order.matching.repository.OrderMatchingService;

@Slf4j
@Service
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
    public List<TradeOrder> executeMatchEngine(TradeRequest tradeRequest) {
        List<StockOrder> stockOrders = tradeRequest.getOrders();

        Map<String, PriorityQueue<StockOrder>> perStockSell = new LinkedHashMap<>();
        Map<String, PriorityQueue<StockOrder>> perStockBuy = new LinkedHashMap<>();

        List<TradeOrder> orderBooks = new ArrayList<>();

        for (StockOrder newOrder : stockOrders) {
            String stockSymbol = newOrder.getStockSymbol();

            if (newOrder.getTxnType() == TxnType.buy) {
                //1. any seller waiting
                if (!perStockBuy.containsKey(stockSymbol)) {
                    PriorityQueue<StockOrder> queue = initializeQueueForBuyer(stockOrders.size());
                    perStockBuy.put(newOrder.getStockSymbol(), queue);
                }
                PriorityQueue<StockOrder> buyQueue = perStockBuy.get(stockSymbol);

                //look for matching trades
                if (!perStockSell.isEmpty() && perStockSell.containsKey(stockSymbol) && null != perStockSell.get(stockSymbol)
                        && !perStockSell.get(stockSymbol).isEmpty()) {

                    PriorityQueue<StockOrder> sellerQueue = perStockSell.get(stockSymbol);
                    StockOrder buyer = newOrder;
                    buyQueue.offer(buyer);

                    //till all lower price sold out
                    while (!sellerQueue.isEmpty() && null != sellerQueue.peek() && buyer.getPrice().compareTo(sellerQueue.peek().getPrice()) >= 0
                            && buyer.getQuantity() > 0) {
                        StockOrder seller = sellerQueue.peek();

                        if (seller == null) break;
                        Integer soldQty = null;

                        if (buyer.getQuantity() < seller.getQuantity()) {
                            soldQty = Math.min(buyer.getQuantity(), seller.getQuantity());
                            int remainingQty = seller.getQuantity() - buyer.getQuantity();
                            seller.setQuantity(remainingQty);
                            buyer.setQuantity(0);

                            if (remainingQty > 0) {
                                buyQueue.poll();
                            }
                            //no need to add buyer in the queue, buyer exhausted
                            //keep seller in queue for coming buyer
                        } else if (buyer.getQuantity() > seller.getQuantity()) {
                            soldQty = Math.min(buyer.getQuantity(), seller.getQuantity());
                            sellerQueue.poll(); // take out seller , as sold this order

                            Integer remainingQty = buyer.getQuantity() - seller.getQuantity();
                            buyer.setQuantity(remainingQty);
                            perStockBuy.putIfAbsent(buyer.getStockSymbol(), buyQueue); //
                        } else {
                            soldQty = Math.min(buyer.getQuantity(), seller.getQuantity());
                            seller.setQuantity(0);
                            buyer.setQuantity(0);
                            sellerQueue.poll();
                            buyQueue.poll();
                        }
                        //add in order book
                        orderBooks.add(bookTrade(seller.getOrderId(), soldQty, seller.getPrice(), buyer.getOrderId()));
                    }
                } else { //add buyer in waiting
                    buyQueue.offer(newOrder);
                    perStockBuy.put(newOrder.getStockSymbol(), buyQueue);
                }
            } else if (newOrder.getTxnType() == TxnType.sell) {

                if (!perStockSell.containsKey(stockSymbol)) { // found new key
                    PriorityQueue<StockOrder> queue = initializeQueueForSeller(stockOrders.size());
                    perStockSell.put(newOrder.getStockSymbol(), queue);
                }
                PriorityQueue<StockOrder> sellQueue = perStockSell.get(stockSymbol);

                if (!perStockBuy.isEmpty() && perStockBuy.containsKey(stockSymbol) && null != perStockBuy.get(stockSymbol)
                        && !perStockBuy.get(stockSymbol).isEmpty()) {
                    PriorityQueue<StockOrder> buyerQueue = perStockBuy.get(stockSymbol);
                    StockOrder seller = newOrder;
                    sellQueue.offer(seller);

                    while (!buyerQueue.isEmpty() && null != buyerQueue.peek() && seller.getPrice().compareTo(buyerQueue.peek().getPrice()) <= 0
                            && seller.getQuantity() > 0) {
                        StockOrder buyer = buyerQueue.peek();
                        if (null == buyer) break;
                        Integer soldQty = null;

                        if (buyer.getQuantity() < seller.getQuantity()) {
                            soldQty = Math.min(buyer.getQuantity(), seller.getQuantity());
                            seller.setQuantity(newOrder.getQuantity() - buyer.getQuantity());
                            //no need to add buyer in the queue
                            buyerQueue.poll();// buyer is exhaust
                            buyer.setQuantity(0);

                            perStockSell.putIfAbsent(seller.getStockSymbol(), sellQueue);
                        } else if (buyer.getQuantity() > seller.getQuantity()) {
                            soldQty = Math.min(buyer.getQuantity(), seller.getQuantity());
                            Integer remainingQty = buyer.getQuantity() - seller.getQuantity();
                            buyer.setQuantity(remainingQty);
                            seller.setQuantity(0);

                            if (remainingQty > 0) {
                                sellQueue.poll();
                            }
                        } else { // equal remove buyer
                            soldQty = Math.min(buyer.getQuantity(), seller.getQuantity());
                            buyerQueue.poll();
                            seller.setQuantity(0);
                            buyer.setQuantity(0);
                            sellQueue.poll();
                        }

                        orderBooks.add(bookTrade(seller.getOrderId(), soldQty, seller.getPrice(), buyer.getOrderId()));
                    }
                } else {
                    sellQueue.offer(newOrder);
                    perStockSell.put(newOrder.getStockSymbol(), sellQueue);
                }
            }
        }

        return orderBooks;
    }

    private TradeOrder bookTrade(BigInteger sellerId, Integer quantity, BigDecimal price ,BigInteger buyerId ){
        return TradeOrder.builder()
                .sellerOrderId(sellerId)
                .quantity(quantity)
                .price(price)
                .buyerOrderId(buyerId)
                .build();
    }
    private PriorityQueue<StockOrder> initializeQueueForSeller(int queueSize){
      return new PriorityQueue<>(queueSize, (order1, order2) -> {
            if (order1.getPrice().compareTo(order2.getPrice()) == 0) {  // if same price whoever comes first
                return order1.getCreateAt().compareTo(order2.getCreateAt());
            }
            return order1.getPrice().compareTo(order2.getPrice()); // Ascending order of prices
        });
    }
    private PriorityQueue<StockOrder> initializeQueueForBuyer(int queueSize){

        return new PriorityQueue<>(queueSize, (order1, order2) -> {
            if (order2.getPrice().compareTo(order1.getPrice()) == 0) { // if same price whoever comes first
                return order1.getCreateAt().compareTo(order2.getCreateAt());
            }
            return order2.getPrice().compareTo(order1.getPrice()); // descending order of prices
        });
    }
}
