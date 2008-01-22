package student;

public class MyComponents {
	//private CTroop thisTroop;

	/**
	 * Tipos de Agentes
	 */
	public enum AgentType { SOLDIER, MEDIC, FIELDOPS };
	/**
	 * 
	 * @author carlos
	 *
	 */
	public enum LeaderMessage { REQUEST, ALREADY_EXISTS, I_WIN, YOU_WIN };
	/**
	 * 
	 * @param t: string
	 * @return AgentType
	 */
	public static AgentType parseAgentType(String t) {
		if (t.equals("SOLDIER")) return AgentType.SOLDIER;
		if (t.equals("MEDIC")) return AgentType.MEDIC;
		if (t.equals("FIELDOPS")) return AgentType.FIELDOPS;
		return AgentType.SOLDIER;
	}
	public static LeaderMessage parseLeaderMessage(String t) {
		if (t.equals("REQUEST")) return LeaderMessage.REQUEST;
		if (t.equals("I_WIN")) return LeaderMessage.I_WIN;
		if (t.equals("YOU_WIN")) return LeaderMessage.YOU_WIN;
		if (t.equals("ALREADY_EXISTS")) return LeaderMessage.ALREADY_EXISTS;
		return LeaderMessage.REQUEST;
	}
	
}


