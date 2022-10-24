package stock.order.matching.exception;


public class TradeOrderException extends BaseException {

    public TradeOrderException(String message, String code) {
        super(message,code);
    }

    public static TradeOrderException emptyTradesRecorded(){
        return  new TradeOrderException("No Trade Recoded for the day", "TRADE_NO_ORDER_BOOKED");
    }
}
