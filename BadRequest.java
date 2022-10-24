package stock.order.matching.exception;


public class BadRequest extends BaseException {

    public BadRequest(String message, String code) {
        super(message, code);
    }

    public static BadRequest invalidTxnType(String type){
        return  new BadRequest(String.format("%s transaction type is invalid",type),"TRADE_BAD_TXN_REQUEST");
    }

    public static BadRequest invalidTradeRequest(){
        return  new BadRequest("Invalid Trade transactions","TRADE_BAD_REQUEST");
    }
}
