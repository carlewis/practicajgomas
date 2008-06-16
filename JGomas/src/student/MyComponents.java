package student;

public class MyComponents {
	//private CTroop thisTroop;

	/**
	 * Tipos de Agentes
	 * (SOLDIER, MEDIC, FIELDOPS)
	 */
	public enum AgentType { SOLDIER, MEDIC, FIELDOPS };
	/**
	 * 
	 * @author carlos
	 * 
	 * Tipos de mensajes dentro del protocolo de eleccion de lider del grupo
	 */
	public enum LeaderMessage { REQUEST, ALREADY_EXISTS, I_WIN, YOU_WIN, FINAL_LEADER};
	/**
	 * Roles posibles dentro de la estrategia del Señuelo
	 * <br>
	 * BAIT: Señuelo<br>
	 * BAIT_MEDIC: Medico que acompaña al señuelo<br>
	 * BAIT_FIELDOP: FieldOp que acompaña al señuelo<br>
	 * BAIT_SOLDIER: Soldado que cubre al señuelo en la retirada<br>
	 * TEAM_SOLDIER: Resto de miembros del equipo<br>
	 */
	public enum BaitRole { UNCERTAIN, BAIT, BAIT_MEDIC, BAIT_FIELDOP, BAIT_SOLDIER, TEAM_SOLDIER};
	/**
	 * 
	 */
	public enum BaitMessage { READY, MEDIC, GOAL_TAKEN};
	/**
	 * 
	 */
	public enum BaitCommand { GOTO, WAIT, WITHDRAWPOINT, GIVE_PACKS, ATTACK };
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
		if (t.equals("FINAL_LEADER")) return LeaderMessage.FINAL_LEADER;
		return LeaderMessage.REQUEST;
	}
	
	public static BaitRole parseBaitRole(String t) {
		if (t.equals("BAIT")) return BaitRole.BAIT;
		if (t.equals("BAIT_MEDIC")) return BaitRole.BAIT_MEDIC;
		if (t.equals("BAIT_FIELDOP")) return BaitRole.BAIT_FIELDOP;
		if (t.equals("BAIT_SOLDIER")) return BaitRole.BAIT_SOLDIER;
		if (t.equals("TEAM_SOLDIER")) return BaitRole.TEAM_SOLDIER;
		return BaitRole.UNCERTAIN;
	}
	
	public static BaitCommand parseBaitCommand(String t) {
		if (t.equals("GOTO")) return BaitCommand.GOTO;
		if (t.equals("WAIT")) return BaitCommand.WAIT;
		if (t.equals("WITHDRAWPOINT")) return BaitCommand.WITHDRAWPOINT;
		if (t.equals("GIVE_PACKS")) return BaitCommand.GIVE_PACKS;
		if (t.equals("ATTACK")) return BaitCommand.ATTACK;
		return BaitCommand.WAIT;
	}
	
	public static BaitMessage parseBaitMessage(String t) {
		if (t.equals("READY")) return BaitMessage.READY;
		if (t.equals("MEDIC")) return BaitMessage.MEDIC;
		if (t.equals("GOAL_TAKEN")) return BaitMessage.GOAL_TAKEN;
		return BaitMessage.READY;
	}
	
}


