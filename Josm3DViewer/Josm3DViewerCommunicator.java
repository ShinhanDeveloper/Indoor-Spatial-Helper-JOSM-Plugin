package org.openstreetmap.josm.plugins.indoorSpatialHelper.Josm3DViewer;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

public class Josm3DViewerCommunicator {
    public enum Cmds
    {
        launch, sync_request, sync_response
    }
    
	private String cmd;
	private Map<String, String> parameter = new HashMap<>();
	private transient Gson gson = new Gson();
	
	public Josm3DViewerCommunicator()
	{
	}
	
	public Josm3DViewerCommunicator setParameter(String key, String value)
	{
		parameter.put(key, value);
		return this;
	}
	
	public Josm3DViewerCommunicator newCommand(Cmds cmd)
	{
		this.cmd = cmd.toString();
		parameter.clear();
		return this;
	}
	
	public String end()
	{
		String jsonString = gson.toJson(this);
		return jsonString;
	}
	
	public Cmds getCommand()
	{
		return Cmds.valueOf(cmd);
	}
	
	public String paramValue(String key)
	{
		return parameter.get(key);
	}

	static public Josm3DViewerCommunicator fromJson(String jsonString)
	{
		Gson gson = new Gson();
		return gson.fromJson(jsonString, Josm3DViewerCommunicator.class);
	}
	
	static public Josm3DViewerCommunicator createCommand(Cmds cmd)
	{
		return new Josm3DViewerCommunicator().newCommand(cmd);
	}
	
}
