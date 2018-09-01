package tiger.unfamous.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import android.util.Log;

public class MyHttpClient {
	private static final MyLog log = new MyLog();
	public DefaultHttpClient httpclient = null;
	private static HttpHost proxy = null;
	protected boolean bFlater;
	private long content_length;
	private static MyHttpClient instance;
	private static final int TRY_TIMES = 3;
	private static final int TIMEOUT_MS = 10 * 1000;
	private static final int RETRY_INTERVAL = 500;

	public static MyHttpClient getInstance() {
		if (instance == null) {
			instance = new MyHttpClient();
		}
		return instance;
	}

	private MyHttpClient() {
		SchemeRegistry schemeRegistry = new SchemeRegistry();
		SocketFactory sf = PlainSocketFactory.getSocketFactory();
		schemeRegistry.register(new Scheme("http", sf, 80));
		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, "UTF-8");
		HttpProtocolParams.setUseExpectContinue(params, false);
		// connect timeout 30;
		HttpConnectionParams.setSoTimeout(params, TIMEOUT_MS);
		HttpConnectionParams.setConnectionTimeout(params, TIMEOUT_MS);
		// socket timeout 30s
		// HttpConnectionParams.setSoTimeout(params, TIMEOUT_MS);
		ClientConnectionManager ccm = new ThreadSafeClientConnManager(params,
				schemeRegistry);
		httpclient = new DefaultHttpClient(ccm, params);
		httpclient
				.setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(
						TRY_TIMES, true));

		if (proxy != null)
			httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,
					proxy);
	}

	public static void setProxy(String host, int port) {
		if (null == host || host.length() == 0) {
			proxy = null;
			log.d("set proxy to null");
		} else {
			proxy = new HttpHost(host, port, "http");
			log.d("set proxy: " + proxy.toString());
		}
	}

	
	public HttpResponse execute(HttpUriRequest req) {
		HttpResponse rsp = null;
		try {
			rsp = httpclient.execute(req);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rsp;
	}
	
	// 尝试三次请求
//	public HttpResponse execute(HttpUriRequest req) {
//		HttpResponse rsp = null;
//		for (int i = 0; i < TRY_TIMES; i++) {
//			try {
//				rsp = httpclient.execute(req);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
////			Log.v("run", "path is:"+req.getURI()+"is num:"+i);
//			if (rsp == null) {
//				try {
//					Thread.sleep(RETRY_INTERVAL);
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}				
//			} else {
//				break;
//			}
//		}
//		return rsp;
//	}

	public InputStream sendHttpGet(String url, Map<String, String> headers) {
		log.d("httpGet:" + url);
		
		HttpGet req = new HttpGet(url);
		// set header
		if (headers != null) {
			for (Iterator<?> iter = headers.entrySet().iterator(); iter.hasNext();) {
				@SuppressWarnings("unchecked")
				Entry<String, String> element = (Entry<String, String>) iter.next();
				req.setHeader(element.getKey(), element.getValue());
			}
		}
		HttpEntity resEntity = null;
		for (int i = 0; i < TRY_TIMES; i++) {
			Log.v("run","is num="+i);
			HttpResponse rsp = null;
			try {
				rsp = httpclient.execute(req);
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
			
			if (rsp == null) {
				try {
					Thread.sleep(RETRY_INTERVAL);
				} catch (Exception e) {
					log.e(e);
				}
				continue;
			}
			if (rsp.getStatusLine().getStatusCode() != HttpStatus.SC_OK
					&& rsp.getStatusLine().getStatusCode() != HttpStatus.SC_PARTIAL_CONTENT)
				return null;
			parseHeader(rsp.getAllHeaders());
			resEntity = rsp.getEntity();
			InputStream is;
			try {
				is = resEntity.getContent();
				if (bFlater) {
					GZIPInputStream gis = null;
					try {
						gis = new GZIPInputStream(is);
						return gis;
					} catch (IOException e) {
						log.e(e);
					}
				}
				return is;
			} catch (IllegalStateException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		return null;
	}

//	public InputStream sendHttpPost(String url, Map<String, String> headers)
//			throws Exception {
//		HttpPost req = new HttpPost(url);
//		// set header
//		if (headers != null) {
//			for (Iterator iter = headers.entrySet().iterator(); iter.hasNext();) {
//				Entry<String, String> element = (Entry) iter.next();
//				req.setHeader(element.getKey(), element.getValue());
//			}
//		}
//		HttpEntity resEntity = null;
//		for (int i = 0; i < 3; i++) {
//			HttpResponse response = null;
//			if (response == null) {
//				
//				try {
//					response = execute(req);
//				} catch (Exception e1) {
//					response  = null;
//					e1.printStackTrace();
//				}finally{
//					try {
//					Thread.sleep(RETRY_INTERVAL);
//					} catch (Exception e) {
//					log.e(e);
//					}
//					
//				}
//				continue;
//			}
//			parseHeader(response.getAllHeaders());
//			resEntity = response.getEntity();
//			InputStream is = resEntity.getContent();
//			if (bFlater) {
//				GZIPInputStream gis = null;
//				try {
//					gis = new GZIPInputStream(is);
//					return gis;
//				} catch (IOException e) {
//					log.e(e);
//				}
//			}
//			return is;
//
//		}
//		return null;
//	}

	public int parseHeader(Header[] headers) {
		for (Header header : headers) {
			if (header.getName().equalsIgnoreCase(("Content-Encoding"))) {
				if (header.getValue().indexOf("gzip") >= 0) {
					this.bFlater = true;
				}
			} else if (header.getName().equalsIgnoreCase(("X-Powered-By"))) {

			} else if (header.getName().equalsIgnoreCase(("Content-Length"))) {
				content_length = Long.parseLong(header.getValue());
			}
		}
		return 0;
	}

	/**
	 * 
	 * @param req
	 *            request
	 * @param sc
	 *            status code byte array
	 * @return inputstream from server.You can read data from this inputstream
	 * @throws IOException
	 * @throws ClientProtocolException
	 */
//	public InputStream executeGet(HttpGet req, byte[] sc)
//			throws ClientProtocolException, IOException {
//		for (int i = 0; i < 3; i++) {
//			HttpEntity resEntity = null;
//			HttpResponse response = httpclient.execute(req);
//			int statusCode = response.getStatusLine().getStatusCode();
//			sc[0] = (byte) (statusCode >> 24 & 0xff);
//			sc[1] = (byte) (statusCode >> 16 & 0xff);
//			sc[2] = (byte) (statusCode >> 8 & 0xff);
//			sc[3] = (byte) (statusCode & 0xff);
//
//			parseHeader(response.getAllHeaders());
//
//			resEntity = response.getEntity();
//			InputStream is = resEntity.getContent();
//			if (bFlater) {
//				GZIPInputStream gis = null;
//				try {
//					gis = new GZIPInputStream(is);
//					return gis;
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//			return is;
//		}
//		return null;
//	}

	public long getContent_length() {
		return content_length;
	}

}
