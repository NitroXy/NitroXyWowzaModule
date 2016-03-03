package com.nitroxy.wmz.module;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.nitroxy.wmz.module.Config.Settings;
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
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
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
		} catch (InvocationTargetException e) {
			Throwable ei = e.getCause();
			if ( ei == null ) ei = e.getTargetException();
			if ( ei == null ) ei = e;
			log.error("HTTPProviderStreamReset.onHTTPRequest: "+ei.toString());
			e.printStackTrace();
			reply.put("status", "error");
			reply.put("error", ei.getMessage());
			reply.put("stacktrace", ei.getStackTrace());
		} catch (Exception e) {
			log.error("HTTPProviderStreamReset.onHTTPRequest: "+e.toString());
			e.printStackTrace();
			reply.put("status", "error");
			reply.put("error", e.getMessage());
			reply.put("stacktrace", e.getStackTrace());
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
	
	protected Object handleRequest(ApplicationManager mngr, IHTTPRequest req) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, JsonParseException, JsonMappingException, IOException {
		final String method = req.getMethod().toUpperCase();
		WMSLogger log = WMSLoggerFactory.getLogger(HTTPServerVersion.class);
		Map<String,String> args = null;
		
		if ( method.equals("POST") ) {
			if ( req.getContentType().equals("application/json") ){
				/* parse body instead of form data */
				final ObjectMapper mapper = new ObjectMapper();
				InputStream io = req.getInputStream();
				TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {};
				args  = mapper.readValue(io, typeRef);
			} else {
				/* Flatten parameter array, multiple arguments with same name is not supported here */
				req.parseBodyForParams(true);
				args = new HashMap<String,String>();
				Map<String,List<String>> parameters = req.getParameterMap();
				for ( Entry<String, List<String>> entry : parameters.entrySet() ){
					log.info(entry.getKey());
					args.put(entry.getKey(), entry.getValue().get(0));
				}
			}
		}
		
		///@TODO Find a better way to extract the local portion of the url ("<RequestFilers>foobar*</RequestFilters>")
		Matcher matcher = url_pattern.matcher(req.getPath());
		if ( !matcher.matches() ){
			throw new IllegalArgumentException("Failed to match url");
		}

		/* Let ApplicationManager dispatch this request */
		String url = matcher.group(1);
		return mngr.routeRequest(method, url, args);
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
