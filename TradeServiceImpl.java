package stock.order.matching.services;

import java.util.PriorityQueue;
import org.springframework.util.MultiValueMap;
import stock.order.matching.model.StockOrder;
import stock.order.matching.model.request.TradeRequest;
import stock.order.matching.model.response.ProcessedOrderResponse;
import stock.order.matching.repository.TradeService;

public class TradeServiceImpl implements TradeService {

    @Override
    public MultiValueMap<String, PriorityQueue<StockOrder>> rearrangeTradeByPriceAndStockSymbol(TradeRequest tradeRequest) {
        return null;
    }

    @Override
    public ProcessedOrderResponse processOrders(TradeRequest tradeRequest, MultiValueMap<String, PriorityQueue<StockOrder>> priorityOrders) {
        return null;
    }
}
