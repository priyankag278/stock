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
import stock.order.matching.repository.OrderMatchingEngine;

@Slf4j
@Service
public class OrderMatchingEngineImpl implements OrderMatchingEngine {

    /**
     * Task of this method to match seller and buyer demand
     * if  buyer price is equal to and greater than seller
     * price , and in case of price level preferred trader who
     * initiated early transactions
     *
     * @param stockOrders@return
     */
    @Override
    public List<TradeOrder> executeMatchEngine(List<StockOrder> stockOrders) {
        log.info("OrderMatchingEngineImpl::executeMatchEngine executing order matching process");

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
                    checkIfAnySellerAvailableToBuy(perStockBuy, buyer, sellerQueue, buyerQueue, orderBooks);
//                    while (!sellerQueue.isEmpty() && null != sellerQueue.peek() && buyer.getPrice().compareTo(sellerQueue.peek().getPrice()) >= 0
//                            && buyer.getQuantity() > 0) {
//                        StockOrder seller = sellerQueue.peek();
//
//                        if (seller == null) break;
//                        Integer soldQty =  Math.min(buyer.getQuantity(), seller.getQuantity());
//
//                        if (buyer.getQuantity() < seller.getQuantity()) {
//                            int remainingQty = seller.getQuantity() - buyer.getQuantity();
//                            seller.setQuantity(remainingQty);
//                            buyer.setQuantity(0);
//
//                            if (remainingQty > 0) {
//                                buyerQueue.poll();
//                            }
//                            //no need to add buyer in the queue, buyer exhausted
//                            //keep seller in queue for coming buyer
//                        } else if (buyer.getQuantity() > seller.getQuantity()) {
//                            sellerQueue.poll(); // take out seller , as sold this order
//
//                            Integer remainingQty = buyer.getQuantity() - seller.getQuantity();
//                            buyer.setQuantity(remainingQty);
//                            perStockBuy.putIfAbsent(buyer.getStockSymbol(), buyerQueue); //
//                        } else {
//                            seller.setQuantity(0);
//                            buyer.setQuantity(0);
//                            sellerQueue.poll();
//                            buyerQueue.poll();
//                        }
//                        //add in order book
//                        orderBooks.add(bookTrade(seller.getOrderId(), soldQty, seller.getPrice(), buyer.getOrderId()));
//                    }
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

                    checkIfBuyerAvailableToSell(perStockSell, seller, sellerQueue, buyerQueue, orderBooks);
//                    while (!buyerQueue.isEmpty() && null != buyerQueue.peek() && seller.getPrice().compareTo(buyerQueue.peek().getPrice()) <= 0
//                            && seller.getQuantity() > 0) {
//                        StockOrder buyer = buyerQueue.peek();
//                        if (null == buyer) break;
//                        Integer soldQty =  Math.min(buyer.getQuantity(), seller.getQuantity());
//
//                        if (buyer.getQuantity() < seller.getQuantity()) {
//                            seller.setQuantity(seller.getQuantity() - buyer.getQuantity());
//                            //no need to add buyer in the queue
//                            buyerQueue.poll();// buyer is exhaust
//                            buyer.setQuantity(0);
//
//                            perStockSell.putIfAbsent(seller.getStockSymbol(), sellerQueue);
//                        } else if (buyer.getQuantity() > seller.getQuantity()) {
//                            Integer remainingQty = buyer.getQuantity() - seller.getQuantity();
//                            buyer.setQuantity(remainingQty);
//                            seller.setQuantity(0);
//                            if (remainingQty > 0) {
//                                sellerQueue.poll();
//                            }
//                        } else { // equal remove buyer
//                            buyerQueue.poll();
//                            sellerQueue.poll();
//                            seller.setQuantity(0);
//                            buyer.setQuantity(0);
//                        }
//                        orderBooks.add(bookTrade(seller.getOrderId(), soldQty, seller.getPrice(), buyer.getOrderId()));
//                    }
                } else {
                    sellerQueue.offer(newOrder);
                    perStockSell.put(newOrder.getStockSymbol(), sellerQueue);
                }
            }
        }

        return orderBooks;
    }

    /**
     * This method look for all seller created order and
     * waiting for new buyer having order greater then
     * equal to seller stock price. On success match add
     * a new transaction to order book
     *
     * @param perStockBuy
     * @param buyer
     * @param sellerQueue
     * @param buyerQueue
     * @param orderBooks
     */
    private void checkIfAnySellerAvailableToBuy(Map<String, PriorityQueue<StockOrder>> perStockBuy, StockOrder buyer,
                                                PriorityQueue<StockOrder> sellerQueue, PriorityQueue<StockOrder> buyerQueue, List<TradeOrder> orderBooks) {
        log.info("OrderMatchingEngineImpl::checkIfAnySellerAvailableToBuy check if any seller waiting for stock to buy");
        while (!sellerQueue.isEmpty() && null != sellerQueue.peek() && buyer.getPrice().compareTo(sellerQueue.peek().getPrice()) >= 0
                && buyer.getQuantity() > 0) {
            StockOrder seller = sellerQueue.peek();

            if (seller == null) break;
            Integer soldQty = Math.min(buyer.getQuantity(), seller.getQuantity());

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
        log.info("OrderMatchingEngineImpl::Exiting from checkIfAnySellerAvailableToBuy");
    }

    /**
     * This method look for all buyer created order and
     * waiting for new seller having order less then
     * equal to buyer stock price. On success match add
     * a new transaction to order book
     *
     * @param perStockSell
     * @param seller
     * @param sellerQueue
     * @param buyerQueue
     * @param orderBooks
     */
    private void checkIfBuyerAvailableToSell(Map<String, PriorityQueue<StockOrder>> perStockSell, StockOrder seller,
                                             PriorityQueue<StockOrder> sellerQueue, PriorityQueue<StockOrder> buyerQueue, List<TradeOrder> orderBooks) {
        log.info("OrderMatchingEngineImpl::checkIfBuyerAvailableToSell check if any buyer waiting for stock to sell");
        while (!buyerQueue.isEmpty() && null != buyerQueue.peek() && seller.getPrice().compareTo(buyerQueue.peek().getPrice()) <= 0
                && seller.getQuantity() > 0) {
            StockOrder buyer = buyerQueue.peek();
            if (null == buyer) break;
            Integer soldQty = Math.min(buyer.getQuantity(), seller.getQuantity());

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
        log.info("OrderMatchingEngineImpl::checkIfBuyerAvailableToSell Exiting from checkIfBuyerAvailableToSell");
    }

    /**
     * Create a new instance of price matched transaction
     * betwee  buyer and seller
     *
     * @param sellerId
     * @param quantity
     * @param price
     * @param buyerId
     * @return
     */
    private TradeOrder bookTrade(BigInteger sellerId, Integer quantity, BigDecimal price, BigInteger buyerId) {
        log.info("OrderMatchingEngineImpl::bookTrade found match create new trade entry");
        return TradeOrder.builder()
                .sellerOrderId(sellerId)
                .quantity(quantity)
                .price(price)
                .buyerOrderId(buyerId)
                .build();
    }

    /**
     * Create a  new instance of seller queue with sorting based
     * on stock price in ascending order ,in case of price level
     * seller who placed order earlier will get priority
     *
     * @param queueSize
     * @return
     */
    private PriorityQueue<StockOrder> initializeQueueForSeller(int queueSize) {
        log.info("OrderMatchingEngineImpl::initializeQueueForSeller get new queue for seller");
        return new PriorityQueue<>(queueSize, (order1, order2) -> {
            if (order1.getPrice().compareTo(order2.getPrice()) == 0) {  // if same price whoever comes first
                return order1.getCreatedAt().compareTo(order2.getCreatedAt());
            }
            return order1.getPrice().compareTo(order2.getPrice()); // Ascending order of prices
        });
    }

    /**
     * Create a  new instance of buyer queue with sorting based
     * on stock price in descending order ,in case of price level
     * buyer who placed order earlier will get priority
     *
     * @param queueSize
     * @return
     */
    private PriorityQueue<StockOrder> initializeQueueForBuyer(int queueSize) {
        log.info("OrderMatchingEngineImpl::initializeQueueForBuyer get new queue for buyer");
        return new PriorityQueue<>(queueSize, (order1, order2) -> {
            if (order2.getPrice().compareTo(order1.getPrice()) == 0) { // if same price whoever comes first
                return order1.getCreatedAt().compareTo(order2.getCreatedAt());
            }
            return order2.getPrice().compareTo(order1.getPrice()); // descending order of prices
        });
    }
}
