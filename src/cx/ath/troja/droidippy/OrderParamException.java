
package cx.ath.troja.droidippy;

public class OrderParamException extends RuntimeException {
    public int messageResource;
    public OrderParamException(String s, int messageResource) {
	super(s);
	this.messageResource = messageResource;
    }
}

