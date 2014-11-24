package amfsmall;

public interface Formatter<T> {
	String toString(T v);
	int getNumberOfItemsPerLine();
	String getFormatString();
}
