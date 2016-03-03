package com.nitroxy.wmz.module;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wowza.wms.application.IApplication;
import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.http.HTTPServerVersion;
import com.wowza.wms.http.HTTProvider2Base;
import com.wowza.wms.http.IHTTPRequest;
import com.wowza.wms.http.IHTTPResponse;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.vhost.IVHost;

public class RESTProvider extends HTTProvider2Base {
	protected Pattern url_pattern;
	
	@Override
	public void init(){
		super.init();
		
		url_pattern = Pattern.compile(this.getRequestFilters().replace("*", "(.*)"));
	}
	
	@Override
	public void onHTTPRequest(IVHost vhost, IHTTPRequest req, IHTTPResponse resp) {
		WMSLogger log = WMSLoggerFactory.getLogger(HTTPServerVersion.class);
		log.info("request");
		
		final IApplication app = vhost.getApplication("nitroxy");
		if ( app == null ){
			log.error("Failed to find nitroxy application");
			this.exitError(500, "NitroXyWowzaModule is not properly loaded", resp);
			return;
		}
		
		final IApplicationInstance appInstance = app.getAppInstance(IApplicationInstance.DEFAULT_APPINSTANCE_NAME);
		if ( appInstance == null ){
			log.error("Failed to find nitroxy application instance");
			this.exitError(500, "NitroXyWowzaModule is not properly loaded", resp);
			return;
		}
		
		final Object module = appInstance.getModuleInstance("NitroXyModule");
		if ( module == null || !(module instanceof NitroXyModule) ){
			log.error("Failed to find NitroXyModule instance");
			this.exitError(500, "NitroXyWowzaModule is not properly loaded", resp);
			return;
		}
		
		final ApplicationManager mngr = ((NitroXyModule)module).getManager(appInstance);
		if ( mngr == null ){
			log.error("Failed to find ApplicationManager instance");
			this.exitError(500, "NitroXyWowzaModule is not properly loaded", resp);
			return;
		}
		
		final ObjectMapper mapper = new ObjectMapper();
		final Map<String,Object> reply = new HashMap<String,Object>();
		resp.setHeader("Content-Type", "application/json");
		
		/* generate reply */
		try {
			final Object result = handleRequest(mngr, req);
			reply.put("status", "success");
			if ( result != null ){
				reply.put("data", result);
			}
		} catch (IllegalAccessException e){
			resp.setResponseCode(404);
			reply.put("status", "error");
		} catch (Exception e) {
			log.error("HTTPProviderStreamReset.onHTTPRequest: "+e.toString());
			e.printStackTrace();
			reply.put("status", "error");
		}
		
		/* try to reply */
		try {
			final String string = mapper.writeValueAsString(reply);
			final OutputStream out = resp.getOutputStream();
			out.write(string.getBytes());
		} catch (Exception e) {
			log.error("HTTPProviderStreamReset.onHTTPRequest: "+e.toString());
			e.printStackTrace();
		}
	}
	
	protected Object handleRequest(ApplicationManager mngr, IHTTPRequest req) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		final String method = req.getMethod().toUpperCase();
		
		if ( method.equals("POST")) {
			req.parseBodyForParams(true);
		}
		
		///@TODO Find a better way to extract the local portion of the url ("<RequestFilers>foobar*</RequestFilters>")
		Matcher matcher = url_pattern.matcher(req.getPath());
		if ( !matcher.matches() ){
			throw new IllegalArgumentException("Failed to match url");
		}
		
		/* Flatten parameter array, multiple arguments with same name is not supported here */
		Map<String,List<String>> parameters = req.getParameterMap();
		Map<String,String> flat = new HashMap<String,String>();
		for ( Entry<String, List<String>> entry : parameters.entrySet() ){
			flat.put(entry.getKey(), entry.getValue().get(0));
		}

		/* Let ApplicationManager dispatch this request */
		String url = matcher.group(1);
		return mngr.routeRequest(method, url, flat);
	}

	protected void exitError(int code, String message, IHTTPResponse resp){
		OutputStream out = resp.getOutputStream();
		try {
			out.write(message.getBytes());
		} catch (IOException e) {

		}
		resp.setResponseCode(code);

	}
}
