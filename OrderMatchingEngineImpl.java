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

        //Store sell & buy queue for each stock symbol
        Map<String, PriorityQueue<StockOrder>> perStockSell = new LinkedHashMap<>();
        Map<String, PriorityQueue<StockOrder>> perStockBuy = new LinkedHashMap<>();

        List<TradeOrder> orderBooks = new ArrayList<>();

        for (StockOrder newOrder : stockOrders) {
            String stockSymbol = newOrder.getStockSymbol();
            log.debug("OrderMatchingEngineImpl::executeMatchEngine stockSymbol :{}",stockSymbol);

            if (newOrder.getTxnType() == TxnType.buy) {

                //if stock symbol found first time, initialize txn queue
                if (!perStockBuy.containsKey(stockSymbol)) {
                    PriorityQueue<StockOrder> queue = initializeQueueForBuyer(stockOrders.size());
                    perStockBuy.put(newOrder.getStockSymbol(), queue);
                }
                PriorityQueue<StockOrder> buyerQueue = perStockBuy.get(stockSymbol);

                //look for already available SELLER orders if any match found
                if (!perStockSell.isEmpty() && perStockSell.containsKey(stockSymbol) && null != perStockSell.get(stockSymbol)
                        && !perStockSell.get(stockSymbol).isEmpty()) {

                    PriorityQueue<StockOrder> sellerQueue = perStockSell.get(stockSymbol);
                    // add buyer in queue to do further processing
                    StockOrder buyer = newOrder; // keeping in local variable just for naming convention of buyer and seller orrder
                    buyerQueue.offer(buyer);
                    log.debug("OrderMatchingEngineImpl::executeMatchEngine buyer {}",buyer);

                    //fulfill seller orders if price/quantity criteria matched
                    checkIfAnySellerAvailableToBuy(buyer, sellerQueue, buyerQueue, orderBooks);
                } else { //add buyer for future orders
                    buyerQueue.offer(newOrder);
                    perStockBuy.put(newOrder.getStockSymbol(), buyerQueue);
                }
            } else if (newOrder.getTxnType() == TxnType.sell) {

                //if stock symbol found first time, initialize txn queue
                if (!perStockSell.containsKey(stockSymbol)) {
                    PriorityQueue<StockOrder> queue = initializeQueueForSeller(stockOrders.size());
                    perStockSell.put(newOrder.getStockSymbol(), queue);
                }
                PriorityQueue<StockOrder> sellerQueue = perStockSell.get(stockSymbol);

                //look for already available BUYER orders if any match found
                if (!perStockBuy.isEmpty() && perStockBuy.containsKey(stockSymbol) &&
                        null != perStockBuy.get(stockSymbol) && !perStockBuy.get(stockSymbol).isEmpty()) {
                    PriorityQueue<StockOrder> buyerQueue = perStockBuy.get(stockSymbol);

                    // add seller in queue to do further processing
                    StockOrder seller = newOrder;
                    sellerQueue.offer(seller);

                    log.debug("OrderMatchingEngineImpl::executeMatchEngine seller {}",seller);

                    //fulfill buyer orders if price/quantity criteria matched
                    checkIfBuyerAvailableToSell(seller, sellerQueue, buyerQueue, orderBooks);
                } else {//add seller for future orders
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
     *  @param buyer
     * @param sellerQueue
     * @param buyerQueue
     * @param orderBooks
     */
    private void checkIfAnySellerAvailableToBuy(StockOrder buyer, PriorityQueue<StockOrder> sellerQueue,
                                                PriorityQueue<StockOrder> buyerQueue, List<TradeOrder> orderBooks) {
        log.info("OrderMatchingEngineImpl::checkIfAnySellerAvailableToBuy check if any seller waiting for stock to buy");

        //loop till buyer price is greater then equal to seller price and sufficient quantity available
        while (!sellerQueue.isEmpty() && null != sellerQueue.peek() && buyer.getPrice().compareTo(sellerQueue.peek().getPrice()) >= 0
                && buyer.getQuantity() > 0) {
            StockOrder seller = sellerQueue.peek();
            log.debug("OrderMatchingEngineImpl::checkIfAnySellerAvailableToBuy Buyer :{} , Seller :{}", buyer, seller);
            if (seller == null) break;
            Integer soldQty = Math.min(buyer.getQuantity(), seller.getQuantity());

            //fulfil seller and buyer with price-matching algorithm
            matchPriceAndQuantity(seller,buyer,sellerQueue,buyerQueue);

            //add matched entry in order book
            orderBooks.add(bookTrade(seller.getOrderId(), soldQty, seller.getPrice(), buyer.getOrderId()));
        }
        log.info("OrderMatchingEngineImpl::Exiting from checkIfAnySellerAvailableToBuy");
    }

    /**
     * This method look for all buyer created order and
     * waiting for new seller having order less then
     * equal to buyer stock price. On success match add
     * a new transaction to order book
     *  @param seller
     * @param sellerQueue
     * @param buyerQueue
     * @param orderBooks
     */
    private void checkIfBuyerAvailableToSell(StockOrder seller, PriorityQueue<StockOrder> sellerQueue,
                                             PriorityQueue<StockOrder> buyerQueue, List<TradeOrder> orderBooks) {
        log.info("OrderMatchingEngineImpl::checkIfBuyerAvailableToSell check if any buyer waiting for stock to sell");
        while (!buyerQueue.isEmpty() && null != buyerQueue.peek() && seller.getPrice().compareTo(buyerQueue.peek().getPrice()) <= 0
                && seller.getQuantity() > 0) {
            StockOrder buyer = buyerQueue.peek();
            log.debug("OrderMatchingEngineImpl::checkIfBuyerAvailableToSell Buyer :{} , Seller :{}",buyer,seller);
            if (null == buyer) break;
            Integer soldQty = Math.min(buyer.getQuantity(), seller.getQuantity());

            //fulfil seller and buyer with price-matching algorithm
            matchPriceAndQuantity(seller,buyer,sellerQueue,buyerQueue);

            orderBooks.add(bookTrade(seller.getOrderId(), soldQty, seller.getPrice(), buyer.getOrderId()));
        }
        log.info("OrderMatchingEngineImpl::checkIfBuyerAvailableToSell Exiting from checkIfBuyerAvailableToSell");
    }

    /**
     * This method verify buyer and seller quantity and clear queue
     * if condition satisfy
     *
     *
     * @param seller
     * @param buyer
     * @param sellerQueue
     * @param buyerQueue
     */
    private void matchPriceAndQuantity(StockOrder seller, StockOrder buyer , PriorityQueue<StockOrder> sellerQueue , PriorityQueue<StockOrder> buyerQueue){
        log.debug("OrderMatchingEngineImpl::checkIfBuyerAvailableToSell Buyer :{} , Seller :{}",buyer,seller);

        //if buyer quantity is lesser , then pull out from queue after fulfilling this seller
        //keep seller in queue for coming buyers
        if (buyer.getQuantity() < seller.getQuantity()) {
            seller.setQuantity(seller.getQuantity() - buyer.getQuantity());

            buyerQueue.poll();
            buyer.setQuantity(0);
        }
        //if buyer quantity is more, then kepp this and pull out seller
        else if (buyer.getQuantity() > seller.getQuantity()) {
            Integer remainingQty = buyer.getQuantity() - seller.getQuantity();
            buyer.setQuantity(remainingQty);
            seller.setQuantity(0);
            log.debug("OrderMatchingEngineImpl::checkIfBuyerAvailableToSell Remaining{}",remainingQty);

            sellerQueue.poll();
        } else { //buyer and seller having same quantity found perfect match , pull out both
            seller.setQuantity(0);
            buyer.setQuantity(0);

            buyerQueue.poll();
            sellerQueue.poll();
        }
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
