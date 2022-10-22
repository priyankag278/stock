package stock.order.matching.model.request;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import stock.order.matching.model.StockOrder;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeRequest {

    private List<StockOrder> orders;
}
