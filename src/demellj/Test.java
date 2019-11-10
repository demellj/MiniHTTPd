package demellj;

import demellj.minihttpd.MiniHTTPd;
import demellj.minihttpd.Request;
import demellj.minihttpd.Responder;
import demellj.minihttpd.Response;

public class Test {
	public static void main(String[] args) throws Exception {
		MiniHTTPd server = new MiniHTTPd(9000);
		
		server.setResponder(new Responder() {
			@Override
			public Response respond(Request req) {
				if (req.path.equals("/favicon.ico"))
					return Response.Factory.new404();
				else
					return Response.Factory.new200("<b>path:</b>&nbsp;" + req.path + "</br><b>type:</b> " + req.type + "</br>" + req.headers);
			}
		});
		
		server.startup(4);
	}
}
