package stock.order.matching.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import stock.order.matching.model.response.TxnResponseError;

@Slf4j
public class FieldErrorTransformer {

    public TxnResponseError createValidationError(FieldError error) {
        log.info("Fields :{}",error.getField());

        return TxnResponseError.builder()
                .code("TRADE_BAD_REQUEST")
                .message(error.getDefaultMessage())
                .build();
    }
}