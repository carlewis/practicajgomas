package student;

import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.StringTokenizer;

import student.MyComponents.AgentType;
import student.MyComponents.BaitRole;
import student.MyComponents.LeaderMessage;

@SuppressWarnings("serial")
public class SoldierComm extends Communication {
	/** Puntero al agente */
	private MySoldier m_cSoldier = null;
	/** Puja para ser el lider */
	private double m_fLeaderBid;
	
	public SoldierComm(MySoldier cSoldier, double fBid) {
		m_cSoldier = cSoldier;
		m_fLeaderBid = fBid;
	}
	
	private LeaderMessage GetLeaderMessageType(String s) {
		StringTokenizer tokens = new StringTokenizer(s);
		tokens.nextToken(); // Quita (
		return MyComponents.parseLeaderMessage(tokens.nextToken());
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
		System.out.println("ejecutar comando " + s);
		// MySoldier.this
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
					//m_bExistsLeader = true;
					//m_TeamLeader = msgLO.getSender();
					//m_bWaitAnswer = false;
				}
				else if (nType == LeaderMessage.YOU_WIN) {
					// No existe un lider que no sea yo -> Sigue comunicando
					m_cSoldier.setExistsLeader(false);
					m_cSoldier.setTeamLeader(m_cSoldier.getAID());
					m_cSoldier.setWaitAnswer(false);
					//m_bExistsLeader = false;
					//m_TeamLeader = getAID();
					//m_bWaitAnswer = false;
				}
				else if (nType == LeaderMessage.ALREADY_EXISTS) {
					// Sigue buscando
					m_cSoldier.setWaitAnswer(false);
					//m_bWaitAnswer = false;
				}
				else if (nType == LeaderMessage.FINAL_LEADER) {
					m_cSoldier.setTeamLeader(msgLO.getSender());
				}
			}
			else if (msgLO.getConversationId() == "ROLE_PROTOCOL") {
				// Hay que ver el papel que nos ha dado el lider
				BaitRole role = ContentsToBaitRole(msgLO.getContent());
				m_cSoldier.setAgentRole(role);
				if (role == BaitRole.BAIT)
					System.out.println(m_cSoldier.getName() + " yo soy el puteado");
				else if (role == BaitRole.BAIT_SOLDIER)
					System.out.println(m_cSoldier.getName() + " yo soy el que respalda al puteado");
				// Una vez sabemos el papel que jugamos modificamos los umbrales
				m_cSoldier.SetThresholdValues();
			}
			else if (msgLO.getConversationId() == "INFORM") {
				//System.out.println("El medico es " + ContentToAgent(msgLO.getContent()));
			}
			else if (msgLO.getConversationId() == "COMMAND") {
				ExecuteCommand(msgLO.getContent());
			}
		}

	}
}


