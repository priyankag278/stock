package stock.order.matching.repository;

import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import org.springframework.util.MultiValueMap;
import stock.order.matching.model.StockOrder;
import stock.order.matching.model.TradeOrder;
import stock.order.matching.model.TxnType;
import stock.order.matching.model.request.TradeRequest;

public interface OrderMatchingService {

     Map<String, Map<TxnType,PriorityQueue<StockOrder>>> loadTodaysStock(TradeRequest request);
     List<TradeOrder> executeMatchEngine(Map<String, Map<TxnType,PriorityQueue<StockOrder>>> stocksMap);
}
