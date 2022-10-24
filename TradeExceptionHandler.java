package stock.order.matching.exception;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import stock.order.matching.model.response.TxnFieldErrorResponse;
import stock.order.matching.model.response.TxnResponseError;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestControllerAdvice
@ControllerAdvice
@Slf4j
public class TradeExceptionHandler {

    @Autowired
    ErrorFormatter errorFormatter;

    @ExceptionHandler(TradeOrderException.class)
    public ResponseEntity<TxnResponseError> handleTradeException(TradeOrderException e) {
        return new ResponseEntity<>(new TxnResponseError(e.getMessage(), e.getCode()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadRequest.class)
    public ResponseEntity<TxnResponseError> handleBadRequest(TradeOrderException e) {
        return new ResponseEntity<>(new TxnResponseError(e.getMessage(), e.getCode()), HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(BAD_REQUEST)
    public TxnFieldErrorResponse sendErrorResponse(MethodArgumentNotValidException e) {

        List<TxnResponseError> errors = errorFormatter.formatException(e);
        return TxnFieldErrorResponse.builder()
                .errors(errors)
                .build();
    }


}
