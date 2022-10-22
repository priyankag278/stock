package stock.order.matching.model;

public enum TxnType {
    BUY("BUY"),
    SELL("SELL");

    private String value;

    TxnType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
