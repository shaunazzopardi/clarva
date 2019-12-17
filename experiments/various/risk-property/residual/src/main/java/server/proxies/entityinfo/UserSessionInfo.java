package main.java.server.proxies.entityinfo;

public class UserSessionInfo {
	protected Integer sid;
	protected String log;
	protected Integer owner;
	protected Boolean opened;
	
	public UserSessionInfo(Integer uid, Integer sid, String log, Boolean opened) {
		this.sid = sid;
		owner = uid;
		this.log = log;
		this.opened = opened;
	}

	public Integer getId() { return sid; }
	public Integer getOwner() { return owner; }
	public String getLog() { return log; }
	
	public void setOpened(boolean opened){
		this.opened = opened;
	}
}
