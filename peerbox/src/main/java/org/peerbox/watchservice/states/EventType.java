package org.peerbox.watchservice.states;

/** Utility enumeration, mainly used for logging **/
enum EventType {
	LOCAL_CREATE("LocalCreate"),
	LOCAL_DELETE("LocalDelete"),
	LOCAL_UPDATE("LocalUpdate"),
	LOCAL_MOVE("LocalMove"),
	LOCAL_HARD_DELETE("LocalHardDelete"),

	REMOTE_CREATE("RemoteCreate"),
	REMOTE_DELETE("RemoteDelete"),
	REMOTE_UPDATE("RemoteUpdate"),
	REMOTE_MOVE("RemoteMove");


	private final String type;

	private EventType(String type) {
		this.type = type;
	}

	public String getString() {
		return type;
	}

}
