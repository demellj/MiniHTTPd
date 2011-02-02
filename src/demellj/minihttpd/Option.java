package demellj.minihttpd;

public class Option {
	public final String key;
	public final String value;

	public Option(String key, String value) {
		this.key = key;
		this.value = value;
	}
	
	public String toString() {
		return this.key + ": " + this.value;
	}
}
