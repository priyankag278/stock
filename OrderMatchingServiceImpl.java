package stock.order.matching.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.LinkedMultiValueMap;
import stock.order.matching.model.StockOrder;
import stock.order.matching.model.TradeOrder;
import stock.order.matching.model.TxnType;
import stock.order.matching.model.request.TradeRequest;
import stock.order.matching.repository.OrderMatchingService;

@Slf4j
public class OrderMatchingServiceImpl implements OrderMatchingService {

    @Override
    public Map<String, Map<TxnType,PriorityQueue<StockOrder>>> loadTodaysStock(TradeRequest request) {
        Map<String,List<StockOrder>> stockMapBySymbol = filterOrdersByStockSymbole(request);
        Map<String, Map<TxnType,PriorityQueue<StockOrder>>> stocksMap = new LinkedHashMap<>();

        for (Map.Entry<String,List<StockOrder>> entry: stockMapBySymbol.entrySet()) {
            Map<TxnType,List<StockOrder>> stocksByTxnType = filterOrdersByTxnType(entry.getValue());
            Map<TxnType,PriorityQueue<StockOrder>>  sellMap = new HashMap<>();
            Map<TxnType,PriorityQueue<StockOrder>>  buyMap = new HashMap<>();

            PriorityQueue<StockOrder> sellQueue = sortStocksPriceByPrice(TxnType.SELL,stocksByTxnType.get(TxnType.SELL).size(),null,stocksByTxnType.get(TxnType.SELL));
            PriorityQueue<StockOrder> buyQueue = sortStocksPriceByPrice(TxnType.BUY,null,stocksByTxnType.get(TxnType.BUY).size(),stocksByTxnType.get(TxnType.BUY));

            sellMap.put(TxnType.SELL,sellQueue);
            buyMap.put(TxnType.BUY,buyQueue);

            stocksMap.put(entry.getKey(),sellMap);
            stocksMap.put(entry.getKey(),buyMap);

        }
        return stocksMap;
    }

    @Override
    public List<TradeOrder> executeMatchEngine(Map<String, Map<TxnType,PriorityQueue<StockOrder>>> stocksMap){
        //1.if buy is not empty
        //2. pop sell queue queue

        List<TradeOrder> todayTrades = new ArrayList<>();

        for (Map.Entry<String, Map<TxnType,PriorityQueue<StockOrder>>> trades: stocksMap.entrySet()) { // for each stock symbol
            PriorityQueue<StockOrder> sellerPQ = trades.getValue().get(TxnType.SELL);
            PriorityQueue<StockOrder> buyerPQ = trades.getValue().get(TxnType.BUY);

            while (!buyerPQ.isEmpty() && !sellerPQ.isEmpty()) {
                StockOrder buyer = buyerPQ.peek();
                StockOrder seller = sellerPQ.peek();

                if(buyer.getPrice().compareTo(seller.getPrice()) > 0) {
                    if(buyer.getQuantity()<seller.getQuantity()){
                        buyerPQ.poll();
                        seller.setQuantity(seller.getQuantity()-buyer.getQuantity());
                    }
                    else if(buyer.getQuantity().equals(seller.getQuantity())) {
                        buyerPQ.poll();
                        sellerPQ.poll();
                    }
                    else {
                        sellerPQ.poll();
                    }
                    TradeOrder tradeOrder = TradeOrder.builder()
                            .sellerOrderId(seller.getOrderId())
                            .price(seller.getPrice())
                            .quantity(seller.getQuantity()<=buyer.getQuantity() ? seller.getQuantity() : seller.getQuantity()-buyer.getQuantity())
                            .buyerOrderId(buyer.getOrderId())
                            .build();
                    todayTrades.add(tradeOrder);
                }
                else {
                    // no match available all buyer too lower , no sell can happen
                    break;
                }
            }
        }
        return todayTrades;
    }

    private Map<String,List<StockOrder>> filterOrdersByStockSymbole(TradeRequest request){
        Map<String,List<StockOrder>> stocksBySymbol = request.getOrders().stream().collect(Collectors.groupingBy(StockOrder::getStockSymbol));
        return stocksBySymbol;
    }

    private Map<TxnType,List<StockOrder>> filterOrdersByTxnType(List<StockOrder> orders){
        Map<TxnType,List<StockOrder>> stocksByTxnType = orders.stream().collect(Collectors.groupingBy(StockOrder::getTxnType));
        return stocksByTxnType;
    }

    private PriorityQueue<StockOrder> sortStocksPriceByPrice(TxnType txnType, Integer sellCapacity, Integer buyCapacity, List<StockOrder> stockOrders){

        switch (txnType){
            case SELL:
                PriorityQueue<StockOrder> ordersToSell  = new PriorityQueue<>(sellCapacity, (order1, order2) -> {
                    if (order1.getPrice().compareTo(order2.getPrice()) == 0) {  // if same price whoever comes first
                        return order1.getCreateAt().compareTo(order2.getCreateAt());
                    }
                    return order1.getPrice().compareTo(order2.getPrice()); // Ascending order of prices
                });
                ordersToSell.addAll(stockOrders);
                return ordersToSell;
            case BUY:
                PriorityQueue<StockOrder> ordersToBuy  = new PriorityQueue<>(buyCapacity, (order1, order2) -> {
                    if (order2.getPrice().compareTo(order1.getPrice()) == 0) { // if same price whoever comes first
                        return order1.getCreateAt().compareTo(order2.getCreateAt());
                    }
                    return order2.getPrice().compareTo(order1.getPrice()); // descending order of prices
                });
                ordersToBuy.addAll(stockOrders);
                return ordersToBuy;
        }

        return null;
    }
}
