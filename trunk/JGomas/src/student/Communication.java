package student;

import java.util.ArrayList;
import java.util.StringTokenizer;

import es.upv.dsic.gti_ia.jgomas.Vector3D;

import student.MyComponents.AgentType;
import student.MyComponents.BaitCommand;
import student.MyComponents.BaitMessage;
import student.MyComponents.BaitRole;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;

@SuppressWarnings("serial")
public abstract class Communication extends CyclicBehaviour {
	
	protected AgentType ContentsToAgentType(String s) {
		StringTokenizer tokens = new StringTokenizer(s);
		tokens.nextToken(); // Quita (
		return MyComponents.parseAgentType(tokens.nextToken());
	}
	
	protected BaitRole ContentsToBaitRole(String s) {
		StringTokenizer tokens = new StringTokenizer(s);
		tokens.nextToken(); // Quita (
		return MyComponents.parseBaitRole(tokens.nextToken());
	}
	
	protected AID ContentToAgent(ArrayList<AgentInfo> cTeamAgents, String s) {
		StringTokenizer tokens = new StringTokenizer(s);
		tokens.nextToken(); // Quita el (
		String sContentAID = tokens.nextToken();
		sContentAID = sContentAID + tokens.nextToken("))") + "))";
		for (AgentInfo ai: cTeamAgents)
			if (ai.aid.toString().equals(sContentAID))
				return ai.aid;
		return null;
	}
	
	protected BaitCommand ContentsToCommand(String s) {
		StringTokenizer tokens = new StringTokenizer(s);
		tokens.nextToken(); // Quita el (
		String sContentCommand = tokens.nextToken();
		return MyComponents.parseBaitCommand(sContentCommand);
	}
	
	protected Vector3D ContentsToCommandPoint(String s) {
		StringTokenizer tokens = new StringTokenizer(s);
		tokens.nextToken(); // Quita el (
		tokens.nextToken(); // Quita el comando
		tokens.nextToken(); // Quita la ,
		double x, y, z;
		x = Double.parseDouble(tokens.nextToken());
		tokens.nextToken();
		y = Double.parseDouble(tokens.nextToken());
		tokens.nextToken();
		z = Double.parseDouble(tokens.nextToken());
		return new Vector3D(x, y, z);
	}
	
	protected BaitMessage ContentsToMessage(String s) {
		StringTokenizer tokens = new StringTokenizer(s);
		tokens.nextToken(); // Quita el (
		String sContentMessage = tokens.nextToken();
		BaitMessage bm = MyComponents.parseBaitMessage(sContentMessage);
		return MyComponents.parseBaitMessage(sContentMessage);
	}
}
