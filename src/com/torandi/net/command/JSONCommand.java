package com.torandi.net.command;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;


import org.json.JSONArray;
import org.json.JSONObject;

import com.torandi.net.Logger;
import com.torandi.net.XServerSocket;
import com.torandi.net.XSocket;
import com.torandi.net.XStreamListener;


public class JSONCommand<T> implements XStreamListener {
	T object;
	XServerSocket socket;
	HashMap<String,Method> methods = new HashMap<String, Method>();
	Logger logger;
	ArrayList<XSocket> clients = new ArrayList<XSocket>();
	
	public JSONCommand(Logger logger, T obj, String bind,int port) {
		object = obj;
		this.logger = logger;
		
		for(Method m : object.getClass().getMethods()) {
			if(m.getAnnotation(Exposed.class) != null) {
				if(methods.put(m.getName(), m) != null) {
					logger.error("Duplicate method name " + m.getName() + " in class " + object.getClass().getName());
				} else {
					logger.info("Expose method "+m.getName() + " via json");
				}
			}
		}
		
		try {
			socket = new XServerSocket(bind, port, this);
			logger.info("JSONCommand listening on port "+port+" for class "+object.getClass().getName());
		} catch (IOException e) {
			e.printStackTrace();
			logger.error("Exception: " + e);
		}
	} 
	
	private void send_object(XSocket sck, JSONObject obj) {
		sck.writeln(obj.toString());
	}
	
	@Override
	public void dataRecived(String data, ByteBuffer b, XSocket sck) {
		// Parse the data:
		JSONObject json = new JSONObject(data);
		JSONObject return_obj = new JSONObject();
		String function = json.optString("function");
		Method method = methods.get(function);
		if(method == null) {
			if(json.optBoolean("reflect")) {
				return_obj.put("status", "success");
				return_obj.put("reflect", true);
				JSONArray array = new JSONArray();
				for(Method m : methods.values()) {
					JSONObject mobj = new JSONObject();
					mobj.put("name", m.getName());
					JSONArray args = new JSONArray();
					for(Class<?> type : m.getParameterTypes()) {
						args.put(type.getName());
					}
					mobj.put("args", args);
					array.put(mobj);
				}
				return_obj.put("functions", array);
			} else {
				return_obj.put("status", "error");
				return_obj.put("data", "Unknown function " + function);
			}
		} else {
			try {
				Object args[] = new Object[method.getParameterTypes().length];
				if(args.length > 0) {
					JSONArray json_args = json.getJSONArray("args");
					for(int i=0; i<method.getParameterTypes().length; ++i) {
						args[i] = json_args.get(i);
					}
				}
				Object ret = method.invoke(object, args);
				return_obj.put("status", "success");
				if(!method.getReturnType().equals(Void.TYPE)) {
					return_obj.put("data", ret);
				}
			} catch (Exception e) {
				return_obj.put("status", "error");
				return_obj.put("data", "Exception: "+e);
				e.printStackTrace();
			}
		}
		send_object(sck, return_obj);
		return;
	}
	
	@Override
	protected void finalize() throws Throwable {
		logger.info("Shuting down JSON command interface");
		socket.stopListen();
		for(XSocket client : clients) {
			try {
				client.close();
			} catch (IOException e) {
			}
		}
		super.finalize();
	}

	@Override
	public void newClient(XSocket client, XServerSocket srvr) {
		client.setListener(this);
		clients.add(client);
	}

	@Override
	public void reconnect(XSocket sck) {
	}

	@Override
	public void connectionClosed(XSocket sck) {
	}
	
	
	
}
