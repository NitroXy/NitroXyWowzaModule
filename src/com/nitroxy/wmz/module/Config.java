package com.nitroxy.wmz.module;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.wowza.wms.bootstrap.Bootstrap;

public class Config {
	@Target(value = ElementType.FIELD)
	@Retention(value = RetentionPolicy.RUNTIME)
	private static @interface Optional {
	}
	
	public static class Settings {

		@Optional
		public String 	Control_Address = "localhost";
		@Optional
		public int 		Control_Port = 1337;
		
		@Optional
		public boolean StreamSwitcher_Enabled = false;
		public String StreamSwitcher_liveStream = null;
		public String StreamSwitcher_previewStream = null;
		@Optional
		public String StreamSwitcher_liveTarget = null;
		@Optional
		public String StreamSwitcher_previewTarget = null;
		@Optional
		public String StreamSwitcher_fallbackStream = null;

		public String pushPublish_Host = null;
		@Optional
		public int pushPublish_Port = 1935;
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
			settings = mapper.readValue(new File(fileName), Settings.class);
		} catch (Exception e) {
			log.error("Internal error when reflecting on Settings: "+e.getMessage());
			exists = false;
		}
	}
	
}
