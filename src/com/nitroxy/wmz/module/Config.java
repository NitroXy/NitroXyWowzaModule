package com.nitroxy.wmz.module;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.wowza.wms.bootstrap.Bootstrap;

public class Config {
	
	public static class Settings {
		public String 	Control_Address = "localhost";
		public int 		Control_Port = 1337;
		
		public boolean StreamSwitcher_Enabled = true;
		public boolean StreamSwitcher_autoRecord = true;
		
		public String StreamSwitcher_liveStream = "live";
		public String StreamSwitcher_liveTarget = "";
		public String StreamSwitcher_previewStream = "preview";
		public String StreamSwitcher_previewTarget = "";
		public String StreamSwitcher_fallbackStream = "mp4:downtime.mp4";
		
		public String pushPublish_Host = null;
		public int    pushPublish_Port = 1935;
		public String pushPublish_Profile = null;
		public String pushPublish_Key = null;
		public String pushPublish_Application = null;
	}
	
	public final String applicationName;
	public Settings settings;
	
	private boolean exists = true;
	private String fileName;
	private static HashMap<String,Config> applicationConfig = new HashMap<String, Config>();
	private ObjectMapper mapper = new ObjectMapper();
	
	private Logger log;
	
	public static Config getConfig(String applicationName, Logger log)  {
		if(applicationConfig.containsKey(applicationName)) {
			return applicationConfig.get(applicationName);
		} else {
			Config c = new Config(applicationName, log);
			applicationConfig.put(applicationName, c);
			return c;
		}
	}
	
	/**
	 * 
	 * @param configPath Path to the applications config directory (without trailing slash)
	 */
	private Config(String applicationName, Logger log) {
		this.log = log;
		this.applicationName = applicationName;
		fileName = Bootstrap.getServerHome(Bootstrap.CONFIGHOME) + "/conf/" + applicationName + "/nitroxy.conf";
		load();
		save();
	}
	
	public boolean exists() {
		return exists;
	}
	
	public void save() {
		if(!exists) return;
		
		try {
			mapper.enable(SerializationFeature.INDENT_OUTPUT);
			mapper.writeValue(new File(fileName), settings);
		} catch (Exception e) {
			log.error("Failed to save config: "+ e.getMessage());
		}
	}
	
	private void load() {
		try {
			mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
			settings = mapper.readValue(new File(fileName), Settings.class);
		} catch (JsonParseException | JsonMappingException e) {
			log.error("Internal error when reflecting on Settings: "+e.getMessage());
			e.printStackTrace();
			exists = false;
		} catch (IOException e) {
			log.error("Loading defaults");
			settings = new Settings();
		}
	}
	
}
