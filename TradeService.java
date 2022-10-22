package stock.order.matching.repository;

import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import stock.order.matching.model.StockOrder;
import stock.order.matching.model.request.TradeRequest;
import stock.order.matching.model.response.ProcessedOrderResponse;

@Service
public interface TradeService {

    MultiValueMap<String, PriorityQueue<StockOrder>> rearrangeTradeByPriceAndStockSymbol(TradeRequest tradeRequest);
    ProcessedOrderResponse processOrders(TradeRequest tradeRequest,
                                         MultiValueMap<String, PriorityQueue<StockOrder>> priorityOrders);

  //  void saveTradesByStockSymbol(String stockSymbol);
  //  List<StockOrder> getAllTrades();
  //  List<StockOrder> getAllTradesByStockSymbol(String stockSymbol);

}
