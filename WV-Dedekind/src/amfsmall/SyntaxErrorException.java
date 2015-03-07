package amfsmall;

public class SyntaxErrorException extends Exception {

	private static final long serialVersionUID = -8955962731296128550L;

	public SyntaxErrorException() {
		super();
	}
	
	public SyntaxErrorException(String string) {
		super(string);
	}

}
