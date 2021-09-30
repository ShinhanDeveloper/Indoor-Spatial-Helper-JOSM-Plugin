package org.openstreetmap.josm.plugins.indoorSpatialHelper.Josm3DViewer;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.openstreetmap.josm.gui.MainApplication;

public class Josm3DViewerLauncher implements Runnable  {
    final String JOSM_PATH ="C:\\Program Files\\Josm3DViewer\\Josm3DViewer.exe";
	
    public interface CommunicationListener {
    	public void onCommunication(Josm3DViewerCommunicator communicator);
    }

	private Process process = null;
	private Scanner receiver = null;
	private PrintWriter sender = null;
	private ProcessBuilder processBuilder = null; 
	private boolean terminated = true;
    private	Gson gson = new Gson();
    private String launchString = "";
    private Josm3DViewerCommunicator communicator = new Josm3DViewerCommunicator();
    private CommunicationListener communicationListener = null;
    

    private Josm3DViewerLauncher() {

    }
 
    private static class LazyHolder {
        public static final Josm3DViewerLauncher INSTANCE = new Josm3DViewerLauncher();
    }
 
    public static Josm3DViewerLauncher getInstance() {
        return LazyHolder.INSTANCE;
    }

    public boolean startJosm3DViewer(String workspaceDir, String buildingID, String targetFloor, String height, CommunicationListener listener)
    {
    	communicationListener = listener;
    	launchString = communicator.newCommand(Josm3DViewerCommunicator.Cmds.launch)
    			.setParameter("workspace", workspaceDir)
    			.setParameter("buildingID", buildingID)
    			.setParameter("targetFloor", targetFloor)
    			.setParameter("height", height)
    			.end();
    	
    	if(!terminated)
    	{
    		sendMessage(launchString);
    		return false;
    	}
    	
    	launchString = launchString.replace("\"", "\\\"");
    	new Thread(this).start();
    	
		return true;
    }
    
    public void close()
    {
    	if(processBuilder != null)
    	{
    		processBuilder = null;
    	}
    	
    	if(process != null)
    	{
    		try {
				process.destroy();
				process.waitFor();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		process = null;
    	}
    	
    	receiver.close();
    	receiver = null;
    	sender.close();
    	sender = null;
    }
    
    public void run()
    {
    	terminated = false;
    	Runtime rt = Runtime.getRuntime();
		processBuilder = new ProcessBuilder(new String[] {JOSM_PATH, launchString});
		try {
			process = processBuilder.start();
			// child process destroy when parent process destroyed
			 rt.addShutdownHook(
				new Thread() {
					public void run() {
						if(process != null)
							process.destroy();
				}
			} );

			// Start reading from the program
			receiver = new Scanner(process.getInputStream());
			// Write a few commands to the program.
			sender = new PrintWriter(process.getOutputStream());
			// Message loop
			new Thread() {
				public void run() {	
					while (receiver != null && receiver.hasNextLine()) {
						String jsonString = receiver.nextLine();
						System.out.println(jsonString);
						try {
							communicationListener.onCommunication(Josm3DViewerCommunicator.fromJson(jsonString));
						} catch (Throwable ex){
							ex.printStackTrace();
						}
						
					}
				}
			}.start();

			process.waitFor();
			close();
		} catch (Exception e) {
			process.destroy();
			e.printStackTrace();
		}
		terminated = true;
    }
    
    public void sendMessage(String msg)
    {
    	if(sender == null)
    		return;
    	
    	sender.println(msg);
    	sender.flush();
    }
    
    public void sendMessage(Josm3DViewerCommunicator cmd)
    {
    	if(sender == null)
    		return;
    	
    	String msg = cmd.end();
    	sender.println(msg);
    	sender.flush();
    }
    
}
