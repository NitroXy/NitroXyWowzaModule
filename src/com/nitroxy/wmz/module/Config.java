package com.nitroxy.wmz.module;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.json.JSONException;
import org.json.JSONObject;

import com.torandi.net.Logger;
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
	
	private static class SettingMeta {
		public final String name;
		public final Field field;
		public final boolean optional;
		
		public SettingMeta(String name, Field field) {
			this.name = name;
			this.field = field;
			optional = (field.getAnnotation(Optional.class) != null);
		}
	}
	
	public final String applicationName;
	public Settings settings;
	
	private boolean exists = true;
	private String fileName;
	private static HashMap<String,Config> applicationConfig = new HashMap<String, Config>();
	private static HashMap<String, ArrayList<SettingMeta>> settingsReflection = null;
	
	private Logger log;
	
	public static Config getConfig(String applicationName, Logger log)  {
		if(settingsReflection == null){
			settingsReflection = new HashMap<String, ArrayList<SettingMeta>>();

			for(Field field : Settings.class.getFields()) {
				log.info(field.getName());
				String[] split = field.getName().split("_");
				if(split.length != 2) {
					log.error("Invalid config variable "+field.getName());
					return null;
				} else {
					if(!settingsReflection.containsKey(split[0])) {
						settingsReflection.put(split[0], new ArrayList<SettingMeta>());
					}
					settingsReflection.get(split[0]).add(new SettingMeta(split[1],field));
				}
			}
		}
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
	 * @param configPath Path to the applications config dir (without trailing slash)
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
			JSONObject json = new JSONObject();
			for(Entry<String, ArrayList<SettingMeta>> entry : settingsReflection.entrySet()) {
				JSONObject jsonSet = new JSONObject();
				for(SettingMeta setting : entry.getValue()) {
					Object data = setting.field.get(settings);
					if(data == null) data = "";
					jsonSet.put(setting.name, data);
				}
				json.put(entry.getKey(), jsonSet);
			}
			Utils.writeFile(fileName, json.toString(2)+"\n");
		} catch (Exception e) {
			log.error("Failed to save config: "+ e.getMessage());
		}
	}
	
	private void load() {
		try {
			settings = new Settings();
			String fileData = Utils.readFile(fileName);
			if(fileData == null) {
				exists = false;
				return;
			} else {
				exists = true;
			}
			JSONObject json = new JSONObject(fileData);
			for(Entry<String, ArrayList<SettingMeta>> entry : settingsReflection.entrySet()) {
				try {
					JSONObject jsonSet = json.getJSONObject(entry.getKey());
					for(SettingMeta setting : entry.getValue()) {
						try {
							if(setting.optional && !jsonSet.has(setting.name)) {
								continue;
							}
							setting.field.set(settings, jsonSet.get(setting.name));
							
						} catch (JSONException e) {
							log.error("Required field not found: "+entry.getKey() + "."+setting.name);
						}
					}
				} catch (JSONException e) {
					log.error("Config group not found: "+entry.getKey());
				}
			}
		} catch (Exception e) {
			log.error("Internal error when reflecting on Settings: "+e.getMessage());
			exists = false;
		}
	}
	
}
