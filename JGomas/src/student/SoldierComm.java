package student;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.StringTokenizer;

import es.upv.dsic.gti_ia.jgomas.Vector3D;

import student.MyComponents.AgentType;
import student.MyComponents.BaitCommand;
import student.MyComponents.BaitMessage;
import student.MyComponents.BaitRole;
import student.MyComponents.LeaderMessage;

@SuppressWarnings("serial")
public class SoldierComm extends Communication {
	/**
	 *  Puntero al agente 
	 */
	private MySoldier m_cSoldier = null;
	/**
	 * Puja para ser el lider 
	 */
	private double m_fLeaderBid;
	
	public SoldierComm(MySoldier cSoldier, double fBid) {
		m_cSoldier = cSoldier;
		m_fLeaderBid = fBid;
	}
	
	// Devuelve true si yo soy el lider, falso en otro caso
	private boolean ContentsToLeader(String s) {
		StringTokenizer tokens = new StringTokenizer(s);
		tokens.nextToken(); // Quita (
		tokens.nextToken(); // Quita el tipo de mensaje
		tokens.nextToken(); // Quita ,
		double fBid = Double.parseDouble(tokens.nextToken());
		if (fBid > m_fLeaderBid)
			return false;
		else 
			return true;
	}
	
	private void ExecuteCommand(String s) {
		// Se separa el comando y los contenidos
		System.out.println("ejecutar comando " + s);
		if (ContentsToCommand(s) == BaitCommand.GOTO) {
			// Sacamos la direccion del mensaje
			Vector3D point = ContentsToCommandPoint(s);
			// Se llama al metodo que sea del objeto m_cSoldier
			m_cSoldier.AddTaskGoto(point);
			// TODO El metodo lanza una tarea AddTask para ir al sitio. 	
		}
		else if (ContentsToCommand(s) == BaitCommand.WAIT) {
			m_cSoldier.WaitForCommand();
		}
		else if (ContentsToCommand(s) == BaitCommand.WITHDRAWPOINT) {
			//
			m_cSoldier.SetWithdrawPoint(ContentsToCommandPoint(s));
		}
		else if (ContentsToCommand(s) == BaitCommand.ATTACK) {
			m_cSoldier.AttackCommand(ContentsToCommandPoint(s), ContentsToCommand2Point(s));
		}
		//m_cSoldier.
	}
	/** 
	 * Metodo principal del comportamiento 
	 */
	public void action() {
		MessageTemplate template = MessageTemplate.MatchAll();
		// recibir un mensaje
		ACLMessage msgLO = m_cSoldier.receive(template);
		if (msgLO != null) {
			// Mensaje para enlazarse con los otros agentes
			if (msgLO.getConversationId() == "COMM_SUBSCRIPTION") {
				// 
				AID cSender = msgLO.getSender();
				AgentType at = ContentsToAgentType(msgLO.getContent());
				m_cSoldier.AddAgent(new AgentInfo(at, cSender));
			}
			else if (msgLO.getConversationId() == "LEADER_PROTOCOL") {
				// Recepcion mensajes lider
				LeaderMessage nType = GetLeaderMessageType(msgLO.getContent());
				if (nType == LeaderMessage.REQUEST) {
					ACLMessage msg = msgLO.createReply();
					// Si no conoce al lider
					if (!m_cSoldier.ExistsLeader()) {
						if (ContentsToLeader(msgLO.getContent())) {
							// respondemos I_WIN
							msg.setContent(" ( " + MyComponents.LeaderMessage.I_WIN + " ) ");
							m_cSoldier.setTeamLeader(m_cSoldier.getAID());
							m_cSoldier.setExistsLeader(false);
						}
						else {
							// respondemos YOU_WIN
							msg.setContent(" ( " + MyComponents.LeaderMessage.YOU_WIN + " ) ");
							m_cSoldier.setTeamLeader(msgLO.getSender());
							m_cSoldier.setExistsLeader(true);
						}
					}
					else {
						// Responde con la identificacion del lider
						msg.setContent(" ( " + MyComponents.LeaderMessage.ALREADY_EXISTS + " , " + 
								m_cSoldier.getTeamLeader() + " ) ");
					}
					msg.setPerformative(ACLMessage.AGREE);
					m_cSoldier.send(msg);
				}
				else if (nType == LeaderMessage.I_WIN) {
					// El otro agente es el lider
					m_cSoldier.setExistsLeader(true);
					m_cSoldier.setTeamLeader(msgLO.getSender());
					m_cSoldier.setWaitAnswer(false);
				}
				else if (nType == LeaderMessage.YOU_WIN) {
					// No existe un lider que no sea yo -> Sigue comunicando
					m_cSoldier.setExistsLeader(false);
					m_cSoldier.setTeamLeader(m_cSoldier.getAID());
					m_cSoldier.setWaitAnswer(false);
				}
				else if (nType == LeaderMessage.ALREADY_EXISTS) {
					// Sigue buscando
					m_cSoldier.setWaitAnswer(false);
				}
				else if (nType == LeaderMessage.FINAL_LEADER) {
					m_cSoldier.setTeamLeader(msgLO.getSender());
				}
			}
			else if (msgLO.getConversationId() == "ROLE_PROTOCOL") {
				// Hay que ver el papel que nos ha dado el lider
				BaitRole role = ContentsToBaitRole(msgLO.getContent());
				m_cSoldier.setAgentRole(role);
				if (role == BaitRole.BAIT) {
					System.out.println(m_cSoldier.getName() + " yo soy el puteado");
					m_cSoldier.SetAgentTeamNames(msgLO.getContent());
				}
				else if (role == BaitRole.BAIT_SOLDIER) {
					System.out.println(m_cSoldier.getName() + " yo soy el que respalda al puteado");
					// Una vez sabemos el papel que jugamos modificamos los umbrales
					m_cSoldier.setAgentRole(BaitRole.BAIT_SOLDIER);
				}
				m_cSoldier.SetThresholdValues();
			}
			else if (msgLO.getConversationId() == "INFORM") {
				// indica al lider que el emisor esta preparado (si es un mensaje ready)
				if (ContentsToMessage(msgLO.getContent()) == BaitMessage.READY) 
					m_cSoldier.SetAgentPrepared(msgLO.getSender());
				else if (ContentsToMessage(msgLO.getContent()) == BaitMessage.GOAL_TAKEN)
					m_cSoldier.AgentTakeGoalPack(msgLO.getSender());
			}
			else if (msgLO.getConversationId() == "COMMAND") {
				ExecuteCommand(msgLO.getContent());
			}
		}

	}
}


