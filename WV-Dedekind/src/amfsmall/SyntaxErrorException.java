package amfsmall;

@SuppressWarnings("serial")
public class SyntaxErrorException extends Exception {

	public SyntaxErrorException() {
		super();
	}
	
	public SyntaxErrorException(String string) {
		super(string);
	}

}
