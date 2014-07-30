package demellj.minihttpd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;


public class Request {
	public enum Type { 
		GET, POST;
		
		public static Type fromString(String s) {
			final String ls = s.toUpperCase();
			if (ls.equals("GET"))
				return GET;
			if (ls.equals("POST"))
				return POST;
			return null;
		}
		
		public String toString() {
			switch (this) {
			case GET: return "GET";
			case POST: return "POST";
			}
			return null;
		}
	}
	
	public final Type               type;
	public final String             path;
	public final List<Option>       headers;
    public final String             query;
    public final Map<String,String> urlparams;
	
	public Request(Type type, String path, List<Option> options) {
		this.type = type;
		this.path = path;
		this.headers = options;

        this.urlparams = new TreeMap<String,String>();

        final int qidx  = path.indexOf("?");
        if (qidx > -1) {
            this.query = path.substring(qidx+1);

            for (final String param : this.query.split("&")) {
                final int pidx  = param.indexOf("=");

                if (pidx >= 0 && pidx < param.length()) {
                    try {
                        final String key = java.net.URLDecoder.decode(param.substring(0,pidx), "UTF-8");
                        final String val = java.net.URLDecoder.decode(param.substring(pidx+1), "UTF-8");

                        this.urlparams.put(key, val);
                    } catch (Exception e) { }
                }
            }
        } else {
            this.query = "";
        }
	}
	
	public static class Parser {
		public static Request parse(InputStream in) {
			BufferedReader bin = new BufferedReader(new InputStreamReader(in));
			
			try {
				String buff = bin.readLine();
				
				if (buff == null) return null;
				
				String[] words = buff.split(" +");
				
				final Type type = Type.fromString(words[0]);
				final String path = words[1];
				
				List<Option> opts = new ArrayList<Option>();
				
				while (bin.ready() && (buff = bin.readLine()) != null) {
					words = buff.split(": +");
					
					if (words != null && words.length > 1)
						opts.add(new Option(words[0], words[1]));
				}
				
				return new Request(type, path, opts);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return null;
		}
		
	}
}
