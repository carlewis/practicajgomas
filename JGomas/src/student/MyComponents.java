package student;
//import es.upv.dsic.gti_ia.jgomas.CTroop;

public class MyComponents {
	//private CTroop thisTroop;

	/**
	 * Tipos de Agentes
	 * TODO: Mover a la clase MyComponents 
	 */
	public enum AgentType { SOLDIER, MEDIC, FIELDOPS };
	public static AgentType parseAgentType(String t) {
		if (t.equals("SOLDIER")) return AgentType.SOLDIER;
		if (t.equals("MEDIC")) return AgentType.MEDIC;
		if (t.equals("FIELDOPS")) return AgentType.FIELDOPS;
		return AgentType.SOLDIER;
	}
	
	
	//public MyComponents(CTroop agent) {
	//	thisTroop = agent;
	//}
	
}
