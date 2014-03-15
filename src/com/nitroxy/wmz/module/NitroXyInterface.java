package com.nitroxy.wmz.module;

import com.torandi.net.command.*;

public class NitroXyInterface {
	private NitroXyModule control = null;
	private JSONCommand<NitroXyInterface> command = null;

	
	public NitroXyInterface(NitroXyModule control) {
		this.control = control;
		command = new JSONCommand<NitroXyInterface>(this.control, this, this.control.bind_address, this.control.bind_port);
	}
	
	void shutdown() {
		command.shutdown();
	}
	
	@Exposed
	public String testMethod() {
		return "hai";
	}
	
	@Exposed
	public int stringParse(String str) {
		return Integer.parseInt(str);
	}
}
