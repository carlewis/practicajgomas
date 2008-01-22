package student;

import jade.core.AID;
import student.MyComponents.AgentType;

public class AgentInfo {
	public AgentType type;
	public AID aid;
	public boolean checked;
	public AgentInfo(AgentType t, AID a) {
		type = t;
		aid = a;
		checked = false;
	}
}
