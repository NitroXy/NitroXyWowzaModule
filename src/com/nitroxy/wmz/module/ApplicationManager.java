package com.nitroxy.wmz.module;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.livestreamrecord.manager.IStreamRecorderConstants;
import com.wowza.wms.livestreamrecord.manager.StreamRecorderParameters;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.vhost.IVHost;

public class ApplicationManager {
	private Config config;
	private StreamSwitcher streamSwitcher = null;
	private NitroXyModule main;
	private IApplicationInstance appInstance;
	private IVHost vhost;
	private Map<String,Method> routing = new HashMap<String, Method>();
	private Date recordStart = null;

	public ApplicationManager(NitroXyModule main, IApplicationInstance appInstance) {
		this.appInstance = appInstance;
		this.main = main;
		this.vhost = appInstance.getVHost();

		loadRouting();

		config = Config.getConfig(appInstance.getApplication().getName(), main);
		if(config.exists()) {
			if(config.settings.StreamSwitcher_Enabled) {
				streamSwitcher = new StreamSwitcher(main, appInstance, config);
			}

			if ( config.settings.StreamSwitcher_autoRecord ) {
				startRecording();
			}
		}
	}

	protected void loadRouting(){
		for (Method method : this.getClass().getMethods()) {
			final Exposed exposed = method.getAnnotation(Exposed.class);
			if ( exposed == null) continue;

			final String url = exposed.url().isEmpty() ? method.getName() : exposed.url();
			final String entry = exposed.method() + ":/" + url;

			if (routing.put(entry, method) != null) {
				main.error("Duplicate route " + entry + " in class " + this.getClass().getName());
			}
		}
	}

	public Object routeRequest(String http_method, String url, Map<String,String> args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		/* find matching method */
		final String entry = http_method + ":" + url;
		Method method = routing.get(entry);
		if ( method == null ){
			throw new IllegalAccessException("No such route exposed");
		}

		/* map named arguments to positional */
		Parameter parameters[] = method.getParameters();
		Object mapped_args[] = new Object[parameters.length];
		for ( int i=0; i < parameters.length; ++i ) {
			final String argname = parameters[i].getName();
			if ( !args.containsKey(argname) ){
				throw new IllegalArgumentException("Missing argument " + argname);
			}
			mapped_args[i] = args.get(argname);
		}

		/* invoke method with arguments */
		return method.invoke(this, mapped_args);
	}

	public void stop() {
		main.info("Stopping ApplicationManager");
		if ( streamSwitcher != null ){
			streamSwitcher.close();
		}

		main.info("ApplicationManager stoppped");
	}

	protected void startRecording(){
		main.info("Starting stream recorder");
		StreamRecorderParameters record = new StreamRecorderParameters(this.appInstance);
		record.segmentationType = IStreamRecorderConstants.SEGMENT_NONE;
		record.fileVersionDelegate = new StreamRecorderFile();
		vhost.getLiveStreamRecordManager().startRecording(appInstance, record);
		this.recordStart = new Date();
	}

	protected void stopRecording(){
		main.info("Stopping stream recorder");
		vhost.getLiveStreamRecordManager().stopRecording(appInstance);
		this.recordStart = null;
		main.info("Stream recorder stopped");
	}

	protected String liveStreamName(){
		return config.settings.StreamSwitcher_liveStream;
	}

	protected String previewStreamName(){
		return config.settings.StreamSwitcher_previewStream;
	}

	protected boolean enabled() {
		return config.exists();
	}


	public void onStreamCreate(IMediaStream stream) {
		if(streamSwitcher != null) streamSwitcher.onStreamCreate(stream);
	}

	protected boolean isPublished(){
		if ( streamSwitcher != null ){
			return streamSwitcher.isPublished();
		} else {
			return false;
		}
	}

	protected String currentLive() {
		return config.settings.StreamSwitcher_liveTarget;
	}

	protected String currentPreview() {
		return config.settings.StreamSwitcher_previewTarget;
	}


	protected String currentFallback() {
		return config.settings.StreamSwitcher_fallbackStream;
	}

	/**
	 * Change source on preview channel.
	 *
	 * @param stream Source name
	 * @return
	 */
	@Exposed(method="POST", url="stream/switch")
	public boolean switchStream(String name)  {
		if(streamSwitcher != null) {
			streamSwitcher.switchStream(name);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Takes the stream currently on the preview channel and publishes to the
	 * live channel.
	 *
	 * @return true if successful.
	 */
	@Exposed(method="POST", url="stream/push")
	public boolean publishStream() {
		if(streamSwitcher != null) {
			streamSwitcher.publishStream();
			return true;
		} else
			return false;
	}

	/**
	 * Control if live stream should be republished to external stream
	 * (e.g. twitch)
	 */
	@Exposed(method="POST", url="publish")
	public boolean publishExternal(boolean state){
		if ( streamSwitcher == null ) return false;

		if ( state ){
			streamSwitcher.startPushPublish();
			return true;
		} else {
			streamSwitcher.stopPushPublish();
			return false;
		}
	}

	@Exposed(method="GET", url="streams")
	public ArrayList<String> getStreams() {
		Pattern p = Pattern.compile("^\\[.+\\].+");

		ArrayList<String> streams = new ArrayList<String>();
		for(IMediaStream s : appInstance.getStreams().getStreams()) {
			String name = s.getName();
			Matcher m = p.matcher(name);

			if ( name.equalsIgnoreCase(liveStreamName()) ) continue;
			if ( name.equalsIgnoreCase(previewStreamName()) ) continue;
			if ( m.matches() ) continue;

			streams.add(name);
		}

		/* list content */
		File contentDir = new File(appInstance.getStreamStorageDir());
		for(final File fileEntry : contentDir.listFiles()) {
			if(!fileEntry.isDirectory()) {
				String ext = Utils.fileExtension(fileEntry.getName());
				if(ext.equalsIgnoreCase("mp4")
						|| ext.equalsIgnoreCase("mov")
						|| ext.equalsIgnoreCase("3gp")
						|| ext.equalsIgnoreCase("m4v")) {
					streams.add("mp4:"+fileEntry.getName());
				} else if(ext.equalsIgnoreCase("flv")) {
					streams.add("flv:"+fileEntry.getName());
				} else if(ext.equalsIgnoreCase("mp3")) {
					streams.add("mp3:"+fileEntry.getName());
				}
			}
		}

		return streams;
	}

	@Exposed(method="GET", url="status")
	public Map<String,Object> fullStatus(){
		Map<String,Object> status = new Hashtable<String,Object>();

		if ( !(config.exists() && config.settings.StreamSwitcher_Enabled) ){
			status.put("enabled", false);
			return status;
		}

		status.put("enabled", true);
		status.put("auto_recording", config.settings.StreamSwitcher_autoRecord);
		status.put("live_target", currentLive());
		status.put("preview_target", currentPreview());
		status.put("fallback_target", currentFallback());
		status.put("is_published", isPublished());

		if (this.config.settings.StreamSwitcher_autoRecord) {
			final Map<String, Object> segment = new Hashtable<String, Object>();
			final Date now = new Date();
			final long duration = (now.getTime() - this.recordStart.getTime()) / 1000;
			segment.put("started_at", this.recordStart.getTime() / 1000);
			segment.put("duration", duration);
			status.put("segment", segment);
		}

		return status;
	}

	@Exposed(method="POST", url="stream/fallback")
	public void setFallback(String name) {
		/* disable if an empty string is passed */
		if ( name.isEmpty() ){
			name = null;
		}

		config.settings.StreamSwitcher_fallbackStream = name;
		config.save();
	}

	@Exposed(method="POST", url="stream/restart")
	public boolean restartBroadcast() {
		if(streamSwitcher != null) {
			streamSwitcher.republish();
			return true;
		} else
			return false;
	}

	@Exposed(method="POST", url="stream/stop")
	public void stopBroadcast() {
		if(streamSwitcher != null) {
			streamSwitcher.stopBroadcast();
		}
	}

	@Exposed(method="POST", url="recording")
	public void toggleRecording(boolean state){
		/* idempotence */
		if (this.config.settings.StreamSwitcher_autoRecord == state) {
			return;
		}

		config.settings.StreamSwitcher_autoRecord = state;
		config.save();

		if ( state ){
			startRecording();
		} else {
			stopRecording();
		}
	}

	@Exposed(method="POST", url="recording/segment")
	public boolean segment(){
		if (!config.settings.StreamSwitcher_autoRecord) {
			throw new RuntimeException("Cannot segment recording when recording is inactive");
		}
		main.info("Segmenting live stream recording - " + currentLive());
		vhost.getLiveStreamRecordManager().splitRecording(appInstance, currentLive());
		this.recordStart = new Date();
		return true;
	}
}
