package demellj.minihttpd;

import java.io.InputStream;
import java.util.List;


public class Request {
	public enum Type { GET, POST };
	
	public final Type type;
	public final String path;
	public final List<Option> options;
	
	public Request(Type type, String path, List<Option> options) {
		this.type = type;
		this.path = path;
		this.options = options;
	}
	
	public static class Parser {
		public static Request parse(InputStream in) {
			
			return null;
		}
	}
}
