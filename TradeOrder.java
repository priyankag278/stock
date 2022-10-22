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
public class TradeOrder {

    private BigInteger sellerOrderId;
    private Integer quantity;
    private BigDecimal price;
    private BigInteger buyerOrderId;
}
