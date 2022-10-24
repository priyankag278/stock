package stock.order.matching.exception;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import stock.order.matching.model.response.TxnResponseError;

@Component("tradeErrorFormatter")
public class ErrorFormatter {

    private FieldErrorTransformer fieldErrorTransformer = new FieldErrorTransformer();

    public List<TxnResponseError> formatException(MethodArgumentNotValidException e) {
        return e.getBindingResult().getFieldErrors().stream()
                .map(fieldErrorTransformer::createValidationError)
                .collect(Collectors.toList());
    }
}

