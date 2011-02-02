package demellj.minihttpd;

public interface Response {
	String getRawText();
	
	public static class Factory {
		static Response http100 = new Response() {
			@Override
			public String getRawText() {
				return "HTTP/1.1 100 Continue\r\n\r\n";
			}
		};
		
		static Response http404 = new Response() {
			@Override
			public String getRawText() {
				return "HTTP/1.1 404 Not found\r\n\r\n";
			}
		};
		
		public static Response new100() {
			return http100;
		}
		
		public static Response new200(final String body) {
			return new Response() {
				@Override
				public String getRawText() {
					return "HTTP/1.1 200 OK\r\nContent-type: text/html\r\n\r\n" + body;
				}
			};
		}
		
		public static Response new404() {
			return http404;
		}
	}
}
