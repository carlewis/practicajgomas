package student;

import jade.core.AID;
import student.MyComponents.AgentType;
import student.MyComponents.BaitRole;

public class AgentInfo {
	public AgentType type;
	public AID aid;
	public boolean checked;
	public BaitRole role;
	public AgentInfo(AgentType t, AID a) {
		type = t;
		aid = a;
		checked = false;
		role = BaitRole.TEAM_SOLDIER;
	}
}
