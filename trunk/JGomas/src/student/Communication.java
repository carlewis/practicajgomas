package student;

import java.util.ArrayList;
import java.util.StringTokenizer;

import student.MyComponents.AgentType;
import student.MyComponents.BaitCommand;
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
}
