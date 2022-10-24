package stock.order.matching.services;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import stock.order.matching.model.StockOrder;
import stock.order.matching.model.TradeOrder;
import stock.order.matching.model.TxnType;
import stock.order.matching.model.request.TradeRequest;
import stock.order.matching.repository.OrderMatchingEngine;

@Slf4j
@Service
public class OrderMatchingEngineImpl implements OrderMatchingEngine {

    /**
     *  Task of this method to match seller and buyer demand
     *  if  buyer price is equal to and greater than seller
     *  price , and in case of price level preferred trader who
     *  initiated early transactions
     *
     * @param tradeRequest
     * @return
     */
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
                PriorityQueue<StockOrder> buyerQueue = perStockBuy.get(stockSymbol);

                //look for matching trades
                if (!perStockSell.isEmpty() && perStockSell.containsKey(stockSymbol) && null != perStockSell.get(stockSymbol)
                        && !perStockSell.get(stockSymbol).isEmpty()) {

                    PriorityQueue<StockOrder> sellerQueue = perStockSell.get(stockSymbol);
                    StockOrder buyer = newOrder;
                    buyerQueue.offer(buyer);
                    //till all lower price sold out
                    while (!sellerQueue.isEmpty() && null != sellerQueue.peek() && buyer.getPrice().compareTo(sellerQueue.peek().getPrice()) >= 0
                            && buyer.getQuantity() > 0) {
                        StockOrder seller = sellerQueue.peek();

                        if (seller == null) break;
                        Integer soldQty =  Math.min(buyer.getQuantity(), seller.getQuantity());

                        if (buyer.getQuantity() < seller.getQuantity()) {
                            int remainingQty = seller.getQuantity() - buyer.getQuantity();
                            seller.setQuantity(remainingQty);
                            buyer.setQuantity(0);

                            if (remainingQty > 0) {
                                buyerQueue.poll();
                            }
                            //no need to add buyer in the queue, buyer exhausted
                            //keep seller in queue for coming buyer
                        } else if (buyer.getQuantity() > seller.getQuantity()) {
                            sellerQueue.poll(); // take out seller , as sold this order

                            Integer remainingQty = buyer.getQuantity() - seller.getQuantity();
                            buyer.setQuantity(remainingQty);
                            perStockBuy.putIfAbsent(buyer.getStockSymbol(), buyerQueue); //
                        } else {
                            seller.setQuantity(0);
                            buyer.setQuantity(0);
                            sellerQueue.poll();
                            buyerQueue.poll();
                        }
                        //add in order book
                        orderBooks.add(bookTrade(seller.getOrderId(), soldQty, seller.getPrice(), buyer.getOrderId()));
                    }
                } else { //add buyer in waiting
                    buyerQueue.offer(newOrder);
                    perStockBuy.put(newOrder.getStockSymbol(), buyerQueue);
                }
            } else if (newOrder.getTxnType() == TxnType.sell) {
                if (!perStockSell.containsKey(stockSymbol)) { // found new key
                    PriorityQueue<StockOrder> queue = initializeQueueForSeller(stockOrders.size());
                    perStockSell.put(newOrder.getStockSymbol(), queue);
                }
                PriorityQueue<StockOrder> sellerQueue = perStockSell.get(stockSymbol);

                if (!perStockBuy.isEmpty() && perStockBuy.containsKey(stockSymbol) &&
                        null != perStockBuy.get(stockSymbol) && !perStockBuy.get(stockSymbol).isEmpty()) {
                    PriorityQueue<StockOrder> buyerQueue = perStockBuy.get(stockSymbol);
                    StockOrder seller = newOrder;
                    sellerQueue.offer(seller);

                    while (!buyerQueue.isEmpty() && null != buyerQueue.peek() && seller.getPrice().compareTo(buyerQueue.peek().getPrice()) <= 0
                            && seller.getQuantity() > 0) {
                        StockOrder buyer = buyerQueue.peek();
                        if (null == buyer) break;
                        Integer soldQty =  Math.min(buyer.getQuantity(), seller.getQuantity());

                        if (buyer.getQuantity() < seller.getQuantity()) {
                            seller.setQuantity(seller.getQuantity() - buyer.getQuantity());
                            //no need to add buyer in the queue
                            buyerQueue.poll();// buyer is exhaust
                            buyer.setQuantity(0);

                            perStockSell.putIfAbsent(seller.getStockSymbol(), sellerQueue);
                        } else if (buyer.getQuantity() > seller.getQuantity()) {
                            Integer remainingQty = buyer.getQuantity() - seller.getQuantity();
                            buyer.setQuantity(remainingQty);
                            seller.setQuantity(0);
                            if (remainingQty > 0) {
                                sellerQueue.poll();
                            }
                        } else { // equal remove buyer
                            buyerQueue.poll();
                            sellerQueue.poll();
                            seller.setQuantity(0);
                            buyer.setQuantity(0);
                        }
                        orderBooks.add(bookTrade(seller.getOrderId(), soldQty, seller.getPrice(), buyer.getOrderId()));
                    }
                } else {
                    sellerQueue.offer(newOrder);
                    perStockSell.put(newOrder.getStockSymbol(), sellerQueue);
                }
            }
        }

        return orderBooks;
    }

    private void checkIfAnySellerAvailableToBuy(Map<String, PriorityQueue<StockOrder>> perStockBuy,StockOrder buyer ,
                                                PriorityQueue<StockOrder> sellerQueue , PriorityQueue<StockOrder> buyerQueue , List<TradeOrder> orderBooks){
        while (!sellerQueue.isEmpty() && null != sellerQueue.peek() && buyer.getPrice().compareTo(sellerQueue.peek().getPrice()) >= 0
                && buyer.getQuantity() > 0) {
            StockOrder seller = sellerQueue.peek();

            if (seller == null) break;
            Integer soldQty =  Math.min(buyer.getQuantity(), seller.getQuantity());

            if (buyer.getQuantity() < seller.getQuantity()) {
                int remainingQty = seller.getQuantity() - buyer.getQuantity();
                seller.setQuantity(remainingQty);
                buyer.setQuantity(0);

                if (remainingQty > 0) {
                    buyerQueue.poll();
                }
                //no need to add buyer in the queue, buyer exhausted
                //keep seller in queue for coming buyer
            } else if (buyer.getQuantity() > seller.getQuantity()) {
                sellerQueue.poll(); // take out seller , as sold this order

                Integer remainingQty = buyer.getQuantity() - seller.getQuantity();
                buyer.setQuantity(remainingQty);
                perStockBuy.putIfAbsent(buyer.getStockSymbol(), buyerQueue); //
            } else {
                seller.setQuantity(0);
                buyer.setQuantity(0);
                sellerQueue.poll();
                buyerQueue.poll();
            }
            //add in order book
            orderBooks.add(bookTrade(seller.getOrderId(), soldQty, seller.getPrice(), buyer.getOrderId()));
        }
    }
    private void checkIfBuyerAvailableToSell(Map<String, PriorityQueue<StockOrder>> perStockSell,StockOrder seller ,
                                             PriorityQueue<StockOrder> sellerQueue , PriorityQueue<StockOrder> buyerQueue , List<TradeOrder> orderBooks){

        while (!buyerQueue.isEmpty() && null != buyerQueue.peek() && seller.getPrice().compareTo(buyerQueue.peek().getPrice()) <= 0
                && seller.getQuantity() > 0) {
            StockOrder buyer = buyerQueue.peek();
            if (null == buyer) break;
            Integer soldQty =  Math.min(buyer.getQuantity(), seller.getQuantity());

            if (buyer.getQuantity() < seller.getQuantity()) {
                seller.setQuantity(seller.getQuantity() - buyer.getQuantity());
                //no need to add buyer in the queue
                buyerQueue.poll();// buyer is exhaust
                buyer.setQuantity(0);

                perStockSell.putIfAbsent(seller.getStockSymbol(), sellerQueue);
            } else if (buyer.getQuantity() > seller.getQuantity()) {
                Integer remainingQty = buyer.getQuantity() - seller.getQuantity();
                buyer.setQuantity(remainingQty);
                seller.setQuantity(0);
                if (remainingQty > 0) {
                    sellerQueue.poll();
                }
            } else { // equal remove buyer
                buyerQueue.poll();
                sellerQueue.poll();
                seller.setQuantity(0);
                buyer.setQuantity(0);
            }
            orderBooks.add(bookTrade(seller.getOrderId(), soldQty, seller.getPrice(), buyer.getOrderId()));
        }
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
                return order1.getCreatedAt().compareTo(order2.getCreatedAt());
            }
            return order1.getPrice().compareTo(order2.getPrice()); // Ascending order of prices
        });
    }
    private PriorityQueue<StockOrder> initializeQueueForBuyer(int queueSize){

        return new PriorityQueue<>(queueSize, (order1, order2) -> {
            if (order2.getPrice().compareTo(order1.getPrice()) == 0) { // if same price whoever comes first
                return order1.getCreatedAt().compareTo(order2.getCreatedAt());
            }
            return order2.getPrice().compareTo(order1.getPrice()); // descending order of prices
        });
    }
}
