package student;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import es.upv.dsic.gti_ia.jgomas.Vector3D;

import student.MyComponents.AgentType;
import student.MyComponents.BaitCommand;
import student.MyComponents.BaitRole;
import student.MyComponents.LeaderMessage;

@SuppressWarnings("serial")
public class FieldOpComm extends Communication {
	/** Puntero al agente */
	private MyFieldOps m_cFieldOp = null;
	
	public FieldOpComm(MyFieldOps cFieldOp) {
		m_cFieldOp = cFieldOp;
	}
	
	private void ExecuteCommand(String s) {
		// Se separa el comando y los contenidos
		System.out.println("fielops: ejecutar comando " + s);
		if (ContentsToCommand(s) == BaitCommand.GOTO) {
			// Sacamos la direccion del mensaje
			Vector3D point = ContentsToCommandPoint(s);
			// Se llama al metodo que sea del objeto m_cMedic
			m_cFieldOp.AddTaskGoto(point);	
		}
		if (ContentsToCommand(s) == BaitCommand.WAIT) {
			m_cFieldOp.WaitForCommand();
		}
		if (ContentsToCommand(s) == BaitCommand.GIVE_PACKS) {
			m_cFieldOp.GiveAmmoPacks();
		}
	}
	/** 
	 * Metodo principal del comportamiento 
	 */
	public void action() {
		MessageTemplate template = MessageTemplate.MatchAll();
		// recibir un mensaje
		ACLMessage msgLO = m_cFieldOp.receive(template);
		if (msgLO != null) {
			// Mensaje para enlazarse con los otros agentes
			if (msgLO.getConversationId() == "COMM_SUBSCRIPTION") {
				// 
				AID cSender = msgLO.getSender();
				AgentType at = ContentsToAgentType(msgLO.getContent());
				m_cFieldOp.AddAgent(new AgentInfo(at, cSender));
			}
			else if (msgLO.getConversationId() == "LEADER_PROTOCOL") {
				// Recepcion mensajes lider
				LeaderMessage nType = GetLeaderMessageType(msgLO.getContent());
				if (nType == LeaderMessage.FINAL_LEADER) {
					m_cFieldOp.setTeamLeader(msgLO.getSender());
				}
			}
			else if (msgLO.getConversationId() == "ROLE_PROTOCOL") {
				// Hay que ver el papel que nos ha dado el lider
				BaitRole role = ContentsToBaitRole(msgLO.getContent());
				m_cFieldOp.setAgentRole(role);
				if (ContentsToBaitRole(msgLO.getContent()) == BaitRole.BAIT_MEDIC) {
					System.out.println(m_cFieldOp.getName() + " yo soy el fieldop del señuelo");
				}
				// Una vez sabemos el papel que jugamos modificamos los umbrales
				m_cFieldOp.SetThresholdValues();
			}
			else if (msgLO.getConversationId() == "INFORM") {
				
			}
			else if (msgLO.getConversationId() == "COMMAND") {
				ExecuteCommand(msgLO.getContent());
			}
		}

	}
}


