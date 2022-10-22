package stock.order.matching.model;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockOrder {

    private BigInteger orderId;
    private ZonedDateTime createAt;
    private String stockSymbol;
    private TxnType txnType;
    private Integer quantity;
    private BigDecimal price;
    private boolean isActive;
}
