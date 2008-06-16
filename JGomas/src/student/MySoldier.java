package student;


import jade.core.AID;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

import student.MyComponents.BaitCommand;
import student.MyComponents.BaitRole;
import student.MyComponents.LeaderMessage;
import student.MyComponents.AgentType;
import student.PathFinding.PathFindingSolver;
import es.upv.dsic.gti_ia.jgomas.CSoldier;
import es.upv.dsic.gti_ia.jgomas.CPack;
import es.upv.dsic.gti_ia.jgomas.CSight;
import es.upv.dsic.gti_ia.jgomas.CTask;
import es.upv.dsic.gti_ia.jgomas.Vector3D;

/**
 * 
 * @version 1.1
 * @author carlos
 */
public class MySoldier extends CSoldier {
	
	private final boolean DEBUG_BAIT = true;
	private final boolean DEBUG_LEADER = true;
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected PathFindingSolver m_cSolver;
	protected BaitLib m_cBaitLib;
	protected final int NUM_OF_AGENTS = 10;
	/**
	 * Nombre del servicio de comunicaciones. Depende del equipo del agente
	 */
	protected String m_sCommunicationsService;
	/**
	 * Tipo del agente AgentType.SOLDIER
	 */
	protected MyComponents.AgentType m_nAgentType;
	/**
	 * Enlaces al resto de agentes para la comunicacion
	 */
	protected ArrayList<AgentInfo> m_TeamAgents;
	/**
	 * Numero de agentes que tiene el equipo
	 */
	protected int m_iTeamSize = Integer.MAX_VALUE;
	/**
	 * Numero de soldados que tiene el equipo, excluyendo al propio
	 */
	protected int m_iTeamSoldiersCount = 0;
	/** 
	 * Puja para ser el lider
	 */
	protected double m_fLeaderBid;
	/**
	 * Tiene conocimiento de un lider
	 */
	protected boolean m_bExistsLeader = false;
	///////////////////////////////////////////////////////////////////////////
	/**
	 * AID del lider del grupo
	 */
	protected AID m_TeamLeader = null;
	protected boolean m_bIsLeader;
	protected boolean m_bWaitAnswer;
	/**
	 * Rol dentro de la estrategia del señuelo
	 */
	protected MyComponents.BaitRole m_nAgentRole;
	/** Estados del lider para la estrategia del señuelo */
	protected enum LeaderState { NO_STATE, DEFINE_POINTS, MOVE_BAIT, SYNCHRONIZE_BAIT, 
		MOVE_TEAM, SYNCHRONIZE_TEAM, GOAL_ATTACK, GOAL_TAKEN, RETURN_BASE};
	/** Estado actual del lider */
	protected LeaderState m_nLeaderState = LeaderState.NO_STATE;
	protected String m_sBaitTeamPoint;
	protected String m_sTeamPoint;
	/**
	 * 
	 */
	protected Vector3D m_cGoalPoint;
	///////////////////////////////////////////////////////////////////////////
	protected SoldierState m_nTeamSoldierState = SoldierState.NO_STATE;
	///////////////////////////////////////////////////////////////////////////
	/** 
	 * Estados del señuelo 
	 */
	protected enum BaitState { NO_STATE, WAIT, MOVING, ATTACK, WITHDRAW, HOLD, GOAL_TAKEN};
	/** 
	 * Estado actual del señuelo 
	 */
	protected BaitState m_nBaitState = BaitState.NO_STATE;
	/** 
	 * Numero de disparos que ha recibido el señuelo 
	 */
	protected int m_iBaitShots = 0;
	/**
	 * Numero de disparos antes de retirarse
	 */
	protected final int SHOTS_TO_WITHDRAW = 3;
	/**
	 * Punto de retirada del señuelo, donde le espera su equipo
	 */
	protected Vector3D m_cWithdrawPoint;
	/**
	 * Indice del camino para la tarea TASK_RUN_AWAY
	 */
	protected int m_iRunAwayIndex;
	/**
	 * Identificador del medico del equipo del señuelo
	 */
	protected AID m_cMedicAid;
	/** 
	 * Identificador del fieldop del equipo del señuelo
	 */
	protected AID m_cFieldOpAid;
	/**
	 * 
	 */
	protected Vector3D m_cBasePoint;
	///////////////////////////////////////////////////////////////////////////
	/**
	 * Estados del respaldo del señuelo
	 */
	protected enum SoldierState { NO_STATE, WAIT, MOVING, MOVING_TO_ATTACK, ATTACK, GOAL_TAKEN };
	/**
	 * Estado actual del respaldo del señuelo
	 */
	protected SoldierState m_nBaitSoldierState = SoldierState.NO_STATE;
	/** */
	protected boolean m_bBaitPrepared = false;
	protected boolean m_bBaitFieldOpPrepared = false;
	protected int m_iTeamAgentPrepared;
	/**
	 * Posicion para esperar en el estado WAIT 
	 */
	protected String m_sWaitPosition;
	/**
	 * setup method
	 */
	protected void setup() {
	
		AddServiceType("Communications");
		super.setup();
		// Definimos el tipo de Agente
		m_nAgentType = MyComponents.AgentType.SOLDIER;
		// Definimos el nombre de los servicios
		if (m_eTeam == TEAM_AXIS) {
			m_sCommunicationsService = "Communications_Axis";
		}
		else {
			m_sCommunicationsService = "Communications_Allied";
		}
		SetUpPriorities();
		// Estructura de datos para las comunicaciones 
		m_TeamAgents = new ArrayList<AgentInfo>();
		// Comienza la comunicacion con el resto de agentes
		m_fLeaderBid = Math.random() * 10;
		m_iTeamSize = StartAgentCommunications();
		NegociateLeaderRole();
		m_cSolver = new PathFindingSolver();
		m_cSolver.setMap(m_Map);
		m_cBaitLib = new BaitLib();
		m_cBaitLib.setPathFindingSolver(m_cSolver);
		m_cBaitLib.setGoal(m_Movement.getDestination());
		m_cGoalPoint = m_Movement.getDestination();
		m_cBasePoint = m_Movement.getPosition();
	}
	
	public ArrayList<AgentInfo> getTeamAgents() {
		return m_TeamAgents;
	}
	public void AddAgent(AgentInfo ai) {
		if (ai.type == AgentType.SOLDIER)
			m_iTeamSoldiersCount++;
		m_TeamAgents.add(ai);
	}
	public boolean ExistsLeader() {
		return m_bExistsLeader;
	}
	public void setExistsLeader(boolean exists) {
		m_bExistsLeader = exists;
	}
	public void setTeamLeader(AID aid) {
		m_TeamLeader = aid;
	}
	public AID getTeamLeader() {
		return m_TeamLeader;
	}
	public void setWaitAnswer(boolean bWaitAnswer) {
		m_bWaitAnswer = bWaitAnswer;
	}
	public void setAgentRole(BaitRole role) {
		m_nAgentRole = role;
		if (role == BaitRole.BAIT)
			m_nBaitState = BaitState.WAIT;
	}
	public BaitRole getAgentRole() {
		return m_nAgentRole;
	}
	/**
	 * Comienza la comunicacion entre el agente y el resto del equipo
	 */
	protected int StartAgentCommunications() {
		// Comienza el comportamiento de comunicaciones
		LaunchCommunicationsBehaviour();
		try {
		wait(10);
		}
		catch (InterruptedException ex) {
			System.err.println("Ocurrio un error mientras se esperaba por los agentes");
		}
		catch (IllegalMonitorStateException ex) {
			// TODO: handle exception
		}
		try {
			// Busca los agentes con servicio Comunications
			DFAgentDescription dfd = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType(m_sCommunicationsService);
			dfd.addServices(sd);
			DFAgentDescription[] result = null;
			do {
				result = DFService.search(this, dfd);
			} while (result.length < 10);
			if (result.length > 0) {
				// Envia un mensaje de suscripcion a cada uno
				ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
				for ( int i = 0; i < result.length; i++ ) {
					AID agent = result[i].getName();
					// No nos lo enviamos a nosotros mismos
					if (!agent.equals(getName())) 
						msg.addReceiver(agent);
				}
				msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
				msg.setConversationId("COMM_SUBSCRIPTION");
				msg.setContent(" ( " + m_nAgentType + " ) ");
				send(msg);
				return result.length;
			}
			else
				System.out.println("No hay ningun agente");
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		return 0;
		
	}
	/**
	 * Lanza un comportamiento que se encarga de mantener la comunicacion con 
	 * el resto de agentes de su equipo
	 */
	private void LaunchCommunicationsBehaviour() {
		// igual habrá que pasarle el puntero this
		addBehaviour(new SoldierComm(this, m_fLeaderBid));
	}

	/**
	 * Implementa el protocolo para decidir el lider del equipo 
	 */
	private void NegociateLeaderRole() {
		addBehaviour(new SimpleBehaviour() {
			private static final long serialVersionUID = 1L;
			private boolean m_bDone = false;
			public void action() {
				// Esperamos a que el array de agentes contenga a todos los agentes del equipo
				if (m_TeamAgents.size() == m_iTeamSize - 1) {
					// Agente por agente hasta saber que no soy el lider o terminar con los agentes
					Iterator<AgentInfo> it = m_TeamAgents.iterator();
					while (!m_bWaitAnswer && !m_bExistsLeader && it.hasNext()) {
						AgentInfo ai = it.next();
						//if (ai.type == MyComponents.AgentType.SOLDIER)
						//	iSoliderNum++;
						// Elegimos un agente tipo soldado
						if ((ai.type == MyComponents.AgentType.SOLDIER) && !ai.checked) {
							// Enviamos un mensaje tipo REQUEST
							ACLMessage msg = new ACLMessage(ACLMessage.PROPOSE);
							msg.addReceiver(ai.aid);
							msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
							msg.setConversationId("LEADER_PROTOCOL");
							msg.setContent(" ( " + MyComponents.LeaderMessage.REQUEST + " , " + m_fLeaderBid + " ) ");
							send(msg);
							m_bWaitAnswer = true;
							ai.checked = true;
						}
					}
					if (!m_bWaitAnswer) {
						m_bDone = true;
					}
				}
			}
			public boolean done() {
				m_bIsLeader = false;
				if ((m_bDone && (m_TeamLeader == getAID())) || 
					(m_bDone && (m_iTeamSoldiersCount == 0))) {
					m_TeamLeader = getAID();
					m_bIsLeader = true;
					m_nLeaderState = LeaderState.DEFINE_POINTS;
					System.out.println(getName() + ": Yo soy el lider");
					// Avisamos a todos los agentes de quien es el lider final
					ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
					Iterator<AgentInfo> it = m_TeamAgents.iterator();
					while (it.hasNext()) {
						AgentInfo ai = it.next();
						msg.addReceiver(ai.aid);
					}
					msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
					msg.setConversationId("LEADER_PROTOCOL");
					msg.setContent(" ( " + LeaderMessage.FINAL_LEADER + " ) ");
					send(msg);
					// Una vez decidido el lider del grupo se asignan los roles
					AssignBaitRoles();
				}
				return m_bDone;
			}
		});
	}
	/**
	 * Asigna los roles de la estrategia de ataque del señuelo.
	 * Ejecuta el lider del equipo
	 */
	protected void AssignBaitRoles() {
		// Enviamos un mensaje a cada agente con su rol
		// Señuelo (SOLDIER), medico del señuelo (MEDIC), 
		// fieldop del señuelo (FIELDOP), agente defensa (SOLDIER)
		boolean bBait = false, bBaitMedic = false, bBaitFieldOp = false,
			bBaitSoldier = false;
		ACLMessage msgBait = new ACLMessage(ACLMessage.INFORM);
		msgBait.setConversationId("ROLE_PROTOCOL");
		
		ACLMessage msgBMedic = new ACLMessage(ACLMessage.INFORM);
		msgBMedic.setConversationId("ROLE_PROTOCOL");
		msgBMedic.setContent(" ( " + MyComponents.BaitRole.BAIT_MEDIC + " ) ");
		
		ACLMessage msgBFieldOp = new ACLMessage(ACLMessage.INFORM);
		msgBFieldOp.setConversationId("ROLE_PROTOCOL");
		msgBFieldOp.setContent(" ( " + MyComponents.BaitRole.BAIT_FIELDOP + " ) ");
		
		ACLMessage msgBSoldier = new ACLMessage(ACLMessage.INFORM);
		msgBSoldier.setConversationId("ROLE_PROTOCOL");
		msgBSoldier.setContent(" ( " + MyComponents.BaitRole.BAIT_SOLDIER + " ) ");
		
		ACLMessage msgOther = new ACLMessage(ACLMessage.INFORM);
		msgOther.setConversationId("ROLE_PROTOCOL");
		msgOther.setContent(" ( " + MyComponents.BaitRole.TEAM_SOLDIER + " ) ");
		// Ahora recorremos la lista de agentes
		Iterator<AgentInfo> it = m_TeamAgents.iterator();
		AID cBaitMedic = null, cBaitFieldop = null;
		while (it.hasNext()) {
			AgentInfo ai = it.next();
			// Todavia no hay un señuelo
			if (!bBait && (ai.type == AgentType.SOLDIER)) {
				msgBait.addReceiver(ai.aid);
				bBait = true;
				ai.role = BaitRole.BAIT;
				continue;
			}
			if (!bBaitSoldier && (ai.type == AgentType.SOLDIER)) {
				msgBSoldier.addReceiver(ai.aid);
				bBaitSoldier = true;
				ai.role = BaitRole.BAIT_SOLDIER;
				continue;
			}
			if (!bBaitMedic && (ai.type == AgentType.MEDIC)) {
				msgBMedic.addReceiver(ai.aid);
				bBaitMedic = true;
				ai.role = BaitRole.BAIT_MEDIC;
				cBaitMedic = ai.aid;
				continue;
			}
			if (!bBaitFieldOp && (ai.type == AgentType.FIELDOPS)) {
				msgBFieldOp.addReceiver(ai.aid);
				bBaitFieldOp = true;
				ai.role = BaitRole.BAIT_FIELDOP;
				cBaitFieldop = ai.aid;
				continue;
			}
			// Resto
			msgOther.addReceiver(ai.aid);
		}
		msgBait.setContent(" ( " + MyComponents.BaitRole.BAIT + " , " + cBaitMedic + 
				" , " + cBaitFieldop + " ) ");
		
		send(msgBait);
		send(msgBMedic);
		send(msgBFieldOp);
		send(msgBSoldier);
		send(msgOther);
	}
	/**
	 * Asigna los umbrales dependiendo del tipo de papel que juega dentro de la estrategia
	 */
	protected void SetThresholdValues() {
/*		if (m_nAgentRole == BaitRole.BAIT) {
			m_Threshold.SetAmmo(10);
			m_Threshold.SetHealth(10);
		}
*/		//m_Threshold.SetAmmo
	}
	
	/**
	 * Comprueba la calidad del camino desde el punto del señuelo al de retirada
	 * En caso necesario modifica el punto de retirada
	 */
	protected void CheckBaitPathToWithdraw(Vector3D path) {
		
	}
	protected void SendWaitCommandEverybody() {
		// TODO SendWaitCommandEverybody
		ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		for (int i = 0; i < m_TeamAgents.size(); i++) {
			if ((m_TeamAgents.get(i).role != BaitRole.BAIT) &&
					(m_TeamAgents.get(i).role != BaitRole.BAIT_FIELDOP) &&
					(m_TeamAgents.get(i).role != BaitRole.BAIT_MEDIC) &&
					(m_TeamAgents.get(i).role != BaitRole.BAIT_SOLDIER)) {
				msg.addReceiver(m_TeamAgents.get(i).aid);
				System.out.println("enviando wait a " + m_TeamAgents.get(i).aid);
			}
		}
		msg.setConversationId("COMMAND");
		msg.setContent(" ( WAIT ) ");
		send(msg);
	}
	/**
	 * Envia al señuelo y su equipos sus coordenadas
	 */
	protected void SendBaitCommandMovement() {
		// TODO SendBaitCommandMovement
		// Se envia un mensaje al señuelo con su punto de ataque y su punto de retirada
		ACLMessage msgBaitPoint = new ACLMessage(ACLMessage.INFORM);
		// Se envia un mensaje al equipo del señuelo con el punto de retirada
		ACLMessage msgTeamPoint = new ACLMessage(ACLMessage.INFORM);
		// Selecciona los receptores
		for (int i = 0; i < m_TeamAgents.size(); i++) {
			if (m_TeamAgents.get(i).role == BaitRole.BAIT)
				msgBaitPoint.addReceiver(m_TeamAgents.get(i).aid);
			else if ((m_TeamAgents.get(i).role == BaitRole.BAIT_FIELDOP) ||
					 (m_TeamAgents.get(i).role == BaitRole.BAIT_MEDIC) || 
					 (m_TeamAgents.get(i).role == BaitRole.BAIT_SOLDIER)) 
				msgTeamPoint.addReceiver(m_TeamAgents.get(i).aid);
			// else if ...
		}
		msgBaitPoint.setConversationId("COMMAND");
		msgTeamPoint.setConversationId("COMMAND");
		Vector3D cBaitPoint = m_cBaitLib.getBaitAttackPoint();
		msgBaitPoint.setContent(" ( GOTO , " + cBaitPoint.x + " , 0.0 , " + cBaitPoint.z + " ) ");
		// Se selecciona el punto de retirada
		Vector3D[] cPath = m_cSolver.FindBaitPath(m_Movement.getPosition().x,
				m_Movement.getPosition().z, cBaitPoint.x, cBaitPoint.z);
		if (cPath.length < 6)
			m_sBaitTeamPoint = cPath[0].x + " , 0.0 , " + cPath[0].z;
		else
			m_sBaitTeamPoint = cPath[cPath.length-6].x + " , 0.0 , " + cPath[cPath.length-6].z;
		msgTeamPoint.setContent(" ( GOTO , " + m_sBaitTeamPoint + " ) ");
		// Se envian los mensajes
		send(msgBaitPoint);
		send(msgTeamPoint);
		// Punto de retirada
		msgBaitPoint.setContent(" ( WITHDRAWPOINT , " + m_sBaitTeamPoint + " ) ");
		send(msgBaitPoint);
	}
	/**
	 * Envia al resto de agentes el comando de movimiento
	 */
	protected void SendCommandMovement() {
		// TODO 
		ACLMessage msgTeamPoint = new ACLMessage(ACLMessage.INFORM);
		for (int i = 0; i < m_TeamAgents.size(); i++) {
			if ((m_TeamAgents.get(i).role != BaitRole.BAIT) &&
					(m_TeamAgents.get(i).role != BaitRole.BAIT_FIELDOP) &&
					(m_TeamAgents.get(i).role != BaitRole.BAIT_MEDIC) &&
					(m_TeamAgents.get(i).role != BaitRole.BAIT_SOLDIER))
				msgTeamPoint.addReceiver(m_TeamAgents.get(i).aid);
			// else if ...
		}
		msgTeamPoint.setConversationId("COMMAND");
		// Punto final
		Vector3D cTeamPoint = m_cBaitLib.getAttackPoint();
		// Ruta hasta el punto
		m_AStarPath = m_cSolver.FindBaitPath(m_Movement.getPosition().x,
				m_Movement.getPosition().z, cTeamPoint.x, cTeamPoint.z);
		if (m_AStarPath.length < 6)
			m_sTeamPoint = m_AStarPath[0].x + " , 0.0 , " + m_AStarPath[0].z;
		else
			m_sTeamPoint = m_AStarPath[m_AStarPath.length-6].x + " , 0.0 , " + m_AStarPath[m_AStarPath.length-6].z;
		m_sWaitPosition = " ( " + m_sTeamPoint + " ) ";
		m_AStarPath = m_cSolver.FindBaitPath(
				m_Movement.getPosition().x,	m_Movement.getPosition().z, 
				m_AStarPath[m_AStarPath.length-6].x, m_AStarPath[m_AStarPath.length-6].z);
		msgTeamPoint.setContent(" ( GOTO , " + m_sTeamPoint + " ) ");
		send(msgTeamPoint);
		//
		String startPos = " ( " + m_AStarPath[0].x + " , 0.0 , " + m_AStarPath[0].z + " ) ";
		m_iAStarPathIndex = 0;
		AddTask(CTask.TASK_WALKING_PATH, getAID(), startPos, m_CurrentTask.getPriority() + 1);
	}
	/**
	 * Envia al equipo principal el comando de ataque. El medico y fieldOp se 
	 * desplazan al punto de ataque principal.
	 */
	protected void SendCommandAttack() {
		// TODO 
		ACLMessage msgTeamPoint = new ACLMessage(ACLMessage.INFORM);
		ACLMessage msgAttack = new ACLMessage(ACLMessage.INFORM);
		for (int i = 0; i < m_TeamAgents.size(); i++) {
			if ((m_TeamAgents.get(i).role != BaitRole.BAIT) &&
					(m_TeamAgents.get(i).role != BaitRole.BAIT_FIELDOP) &&
					(m_TeamAgents.get(i).role != BaitRole.BAIT_MEDIC) &&
					(m_TeamAgents.get(i).role != BaitRole.BAIT_SOLDIER)) {
				if (m_TeamAgents.get(i).type == AgentType.SOLDIER) 
					msgAttack.addReceiver(m_TeamAgents.get(i).aid);					
				else 
					msgTeamPoint.addReceiver(m_TeamAgents.get(i).aid);
			}
		}
		msgTeamPoint.setConversationId("COMMAND");
		msgAttack.setConversationId("COMMAND");
		// Punto final
		Vector3D cTeamPoint = m_cBaitLib.getAttackPoint();
		m_sTeamPoint = cTeamPoint.x + " , 0.0 , " + cTeamPoint.z;
		msgTeamPoint.setContent(" ( GOTO , " + m_sTeamPoint + " ) ");
		msgAttack.setContent(" ( ATTACK , " + m_sTeamPoint + " , " +
				m_cGoalPoint.x + " , 0.0 , " + m_cGoalPoint.z + " ) ");
		send(msgTeamPoint);
		send(msgAttack);
		// El lider es un soldado asi que tiene que atacar
		m_AStarPath = m_cSolver.FindBaitPath(m_Movement.getPosition().x, m_Movement.getPosition().z,
				cTeamPoint.x, cTeamPoint.z);
		if (m_AStarPath == null)
			System.out.println("lider: la ruta es nula");
		String startPos = " ( " + m_AStarPath[0].x + " , 0.0 , " + m_AStarPath[0].z + " ) ";
		m_iAStarPathIndex = 0;
		AddTask(CTask.TASK_WALKING_PATH, getAID(), startPos, m_CurrentTask.getPriority() + 1);
		//String sPos = " ( " + m_cGoalPoint.x + " , 0.0 , " + m_cGoalPoint.z + " ) ";
		//AddTask(CTask.TASK_GET_OBJECTIVE, getAID(), sPos, m_CurrentTask.getPriority() + 1);
	}
	/**
	 * 
	 */
	protected void SendGoalTakenMsgToLeader() {
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.addReceiver(m_TeamLeader);
			msg.setConversationId("INFORM");
			msg.setContent(" ( GOAL_TAKEN ) ");
			send(msg);
	}
	/**
	 * Genera una tarea goto position. Cuando llega espera al siguiente comando
	 */
	public void WaitForCommand() {
		System.out.println("soldier: añadiendo tarea esperar");
		m_sWaitPosition = " ( " + m_Movement.getPosition().x + " , 0.0 , " + m_Movement.getPosition().z + " ) ";
		AddTask(CTask.TASK_GOTO_POSITION, getAID(), m_sWaitPosition, m_CurrentTask.getPriority() + 1);
		if (m_nAgentRole == BaitRole.BAIT_SOLDIER)
			m_nBaitSoldierState = SoldierState.WAIT;
	}
	/**
	 * 
	 */
	public void AddTaskGoto(Vector3D point) {
		System.out.println("soldier: añadiendo tarea ir a ( " + point.x + " , " + point.z + " )");
		// TODO
		m_AStarPath = m_cSolver.FindBaitPath(m_Movement.getPosition().x, m_Movement.getPosition().z,
				point.x, point.z);
		if (m_AStarPath == null)
			System.out.println("soldier: la ruta es nula");
		String startPos = " ( " + m_AStarPath[0].x + " , 0.0 , " + m_AStarPath[0].z + " ) ";
		m_iAStarPathIndex = 0;
		System.out.print(" ( " + m_AStarPath[0].x + " , " + m_AStarPath[0].z + " )->"); 
		System.out.println(" ( " + m_AStarPath[m_AStarPath.length - 1].x + " , " + m_AStarPath[m_AStarPath.length - 1].z + " )");

		AddTask(CTask.TASK_WALKING_PATH, getAID(), startPos, m_CurrentTask.getPriority() + 1);
		// Cambia de estado
		if (m_nAgentRole == BaitRole.BAIT)
			m_nBaitState = BaitState.MOVING;
		else if (m_nAgentRole == BaitRole.TEAM_SOLDIER) {
			//if (m_nTeamSoldierState == SoldierState.NO_STATE) {
			m_sWaitPosition = " ( " + point.x + " , 0.0 , " + point.z + " ) ";
			m_nTeamSoldierState = SoldierState.MOVING;
			//}
			if (m_nTeamSoldierState == SoldierState.WAIT) {
				m_nTeamSoldierState = SoldierState.MOVING_TO_ATTACK;
			}
		}
		else if (m_nAgentRole == BaitRole.BAIT_SOLDIER) 
			m_sWaitPosition = " ( " + point.x + " , 0.0 , " + point.z + " ) ";
	}
	/**
	 * 
	 */
	public void SetAgentPrepared(AID _aid) {
		for (int i = 0; i < m_TeamAgents.size(); i++) {
			if (m_TeamAgents.get(i).aid.equals(_aid)) {
				if (m_TeamAgents.get(i).role == BaitRole.BAIT) {
					if (m_bIsLeader && (m_nLeaderState == LeaderState.MOVE_BAIT))
						m_nLeaderState = LeaderState.SYNCHRONIZE_BAIT;
					m_bBaitPrepared = true;
				}
				else if (m_TeamAgents.get(i).role == BaitRole.BAIT_FIELDOP)
					m_bBaitFieldOpPrepared = true;
				else if (m_TeamAgents.get(i).role == BaitRole.BAIT_MEDIC) { 
					
				}
				else if (m_TeamAgents.get(i).role == BaitRole.TEAM_SOLDIER)
					m_iTeamAgentPrepared--;
			}
		}
	}
	/**
	 * 
	 * @param point
	 */
	public void SetWithdrawPoint(Vector3D point) {
		m_cWithdrawPoint = point;
	}
	/**
	 * 
	 */
	public void SetAgentTeamNames(String medic) {
		StringTokenizer tokens = new StringTokenizer(medic);
		tokens.nextToken(); // Quita el (
		tokens.nextToken(); // Quita el primer parametro
		tokens.nextToken(); // Quita la coma
		String sContentAID = tokens.nextToken();
		sContentAID = sContentAID + tokens.nextToken("))") + "))";
		for (AgentInfo ai: m_TeamAgents) {
			if (ai.aid.toString().equals(sContentAID)) {
				SetAgentMedic(ai.aid);
				break;
			}
		}
		tokens.nextToken(" "); // Quita los parentesis
		tokens.nextToken(); // Quita la segunda coma
		sContentAID = tokens.nextToken();
		System.out.println(sContentAID);
		sContentAID = sContentAID + tokens.nextToken("))") + "))"; 
				for (AgentInfo ai: m_TeamAgents) {
			if (ai.aid.toString().equals(sContentAID)) {
				SetAgentFieldOp(ai.aid);
				break;
			}
		}
	}
	/**
	 * 
	 * @param medic
	 */
	public void SetAgentMedic(AID medic) {
		m_cMedicAid = medic;
	}
	/**
	 * 
	 * @param medic
	 */
	public void SetAgentFieldOp(AID fieldop) {
		m_cFieldOpAid = fieldop;
	}
	/**
	 * 
	 */
	protected void SendReadyMsgToLeader() {
			ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
			msg.addReceiver(m_TeamLeader);
			msg.setConversationId("INFORM");
			msg.setContent(" ( READY ) ");
			send(msg);
	}
	/**
	 * 
	 */
	protected void AgentTakeGoalPack(AID agent) {
		for (int i = 0; i < m_TeamAgents.size(); i++) {
			if (m_TeamAgents.get(i).aid.equals(agent)) {
				if (m_TeamAgents.get(i).role == BaitRole.BAIT) {
					System.out.println("Plan B! el señuelo tiene el objetivo");
					// Envia al equipo completo a proteger al señuelo
					SendCommandAttack();
					m_nLeaderState = LeaderState.MOVE_TEAM;
					return;
				}
				else {
					m_nLeaderState = LeaderState.GOAL_TAKEN;
				}
			}
		}
	}
	/**
	 * 
	 */
	public void AttackCommand(Vector3D cPathPoint, Vector3D cGoalPoint) {
		// TODO attack command cambia a estado move_team
		System.out.println(getName() + " al ataque!");
		m_AStarPath = m_cSolver.FindBaitPath(m_Movement.getPosition().x, m_Movement.getPosition().z,
				cPathPoint.x, cPathPoint.z);
		if (m_AStarPath == null)
			System.out.println("soldier: la ruta es nula");
		String startPos = " ( " + m_AStarPath[0].x + " , 0.0 , " + m_AStarPath[0].z + " ) ";
		m_iAStarPathIndex = 0;
		AddTask(CTask.TASK_WALKING_PATH, getAID(), startPos, m_CurrentTask.getPriority() + 1);
		//String sPos = " ( " + m_cGoalPoint.x + " , 0.0 , " + m_cGoalPoint.z + " ) ";
		//AddTask(CTask.TASK_GET_OBJECTIVE, getAID(), sPos, m_CurrentTask.getPriority() + 1);
		m_nTeamSoldierState = SoldierState.MOVING_TO_ATTACK;
		m_cGoalPoint = cGoalPoint;
	}
	
	
	/**
	 * 
	 */
	protected void SendMsgReturnBase() {
		System.out.println("Envia Mensaje regreso" );
		// TODO 
		ACLMessage msgBasePoint = new ACLMessage(ACLMessage.INFORM);
		for (int i = 0; i < m_TeamAgents.size(); i++) {
			if (m_TeamAgents.get(i).role != BaitRole.BAIT) 
				msgBasePoint.addReceiver(m_TeamAgents.get(i).aid);					
		}
		msgBasePoint.setConversationId("COMMAND");
		msgBasePoint.setContent(" ( GOTO , " + m_cBasePoint.x + " , 0.0 , " + m_cBasePoint.z + " ) ");
		send(msgBasePoint);
		// El lider es un soldado asi que tiene que atacar
		m_AStarPath = m_cSolver.FindBaitPath(m_Movement.getPosition().x, m_Movement.getPosition().z,
				m_cBasePoint.x, m_cBasePoint.z);
		if (m_AStarPath == null)
			System.out.println("lider: la ruta es nula");
		String startPos = " ( " + m_AStarPath[0].x + " , 0.0 , " + m_AStarPath[0].z + " ) ";
		m_iAStarPathIndex = 0;
		AddTask(CTask.TASK_WALKING_PATH, getAID(), startPos, m_CurrentTask.getPriority() + 1);
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////
	// Methods to overload inhereted from CTroop class
	//

	/////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Request for medicine. 
	 * 
	 * This method sends a <b> FIPA REQUEST </b> message to all agents who offers the <tt> m_sMedicService </tt> service.
	 * 
	 * The content of message is: <tt> ( x , y , z ) ( health ) </tt>.
	 * 
	 * Variable <tt> m_iMedicsCount </tt> is updated.
	 * 
	 * <em> It's very useful to overload this method. </em>
	 * 
	 */

	protected void CallForMedic() {

		try {

			DFAgentDescription dfd = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType(m_sMedicService);
			dfd.addServices(sd);
			DFAgentDescription[] result = DFService.search(this, dfd);

			if ( result.length > 0 ) {

				m_iMedicsCount = result.length;

				// Fill the REQUEST message
				ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);

				for ( int i = 0; i < result.length; i++ ) {
					DFAgentDescription dfdMedic = result[i];
					AID Medic = dfdMedic.getName();
					if ( ! Medic.equals(getName()) )
						msg.addReceiver(dfdMedic.getName());
					else
						m_iMedicsCount--;
				}
				msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
				msg.setConversationId("CFM");
				msg.setContent(" ( " + m_Movement.getPosition().x + " , " + m_Movement.getPosition().y + " , " + m_Movement.getPosition().z + " ) ( " + GetHealth() + " ) ");
				send(msg);
				System.out.println(getLocalName()+ ": Need a Medic! (v21)");  			

			} else {
				m_iMedicsCount = 0;
			}
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}
	/////////////////////////////////////////////////////////////////////////////////////////////////////

	/////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Request for ammunition. 
	 * 
	 * This method sends a <b> FIPA REQUEST </b> message to all agents who offers the <tt> m_sAmmoService </tt> service.
	 * 
	 * The content of message is: <tt> ( x , y , z ) ( ammo ) </tt>.
	 * 
	 * Variable <tt> m_iFieldOpsCount </tt> is updated.
	 * 
	 * <em> It's very useful to overload this method. </em>
	 *    
	 */
	protected void CallForAmmo() {
		// aqui ya se cambia a estado withdraw
		if (m_nAgentRole == BaitRole.BAIT) {
			m_nBaitState = BaitState.WITHDRAW;
			m_iRunAwayIndex = m_AStarPath.length - 1;
		}
		System.out.println("call for ammo");
		super.CallForAmmo();

	}
	/////////////////////////////////////////////////////////////////////////////////////////////////////

	
	/////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Request for backup. 
	 * 
	 * This method sends a <b> FIPA REQUEST </b> message to all agents who offers the <tt> m_sBackupService</tt> service.
	 * 
	 * The content of message is: <tt> ( x , y , z ) ( SoldiersCount ) </tt>.
	 * 
	 * Variable <tt> m_iSoldiersCount </tt> is updated.
	 * 
	 * <em> It's very useful to overload this method. </em>
	 *    
	 */
	protected void CallForBackup() {
		
		System.out.println("call for backup");
		super.CallForBackup();

	}
	/////////////////////////////////////////////////////////////////////////////////////////////////////



	/////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Update priority of all 'prepared (to execute)' tasks. 
	 * 
	 * This method is invoked in the state <em>STANDING</em>, and it's used to re-calculate the priority of all tasks (targets) int the task list
	 * of the agent. The reason is because JGOMAS Kernel always execute the maximum priority task. 
	 * 
	 * <em> It's very useful to overload this method. </em>
	 *    
	 */
	protected void UpdateTargets() {
		/*if (m_nAgentRole == BaitRole.BAIT) {
			System.out.println("señuelo actualizando tareas ");
		}*/
	} 
	/////////////////////////////////////////////////////////////////////////////////////////////////////

	/////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Should we update now all 'prepared (to execute)' tasks? 
	 * 
	 * This method is a decision function invoked in the state <em>GOTO_TARGET</em>. A value of <tt> TRUE</tt> break out the inner loop, 
	 * making possible to JGOMAS Kernel extract a more priority task, or update some attributes of the current task.
	 * By default, the return value is <tt> FALSE</tt>, so we execute the current task until it finalizes.
	 *   
	 * <em> It's very useful to overload this method. </em>
	 *   
	 * @return <tt> FALSE</tt> 
	 * 
	 */
	protected boolean ShouldUpdateTargets() { 
		// TODO ShouldUpdateTargets
		if (m_bIsLeader) {
			if ((m_nLeaderState == LeaderState.NO_STATE) ||
					(m_nLeaderState == LeaderState.GOAL_ATTACK) || 
					(m_nLeaderState == LeaderState.GOAL_TAKEN))
				return true;
		}
		else if (m_nAgentRole == BaitRole.BAIT) { 
			if (m_nBaitState == BaitState.ATTACK) 
				return true;
			if (m_nBaitState == BaitState.WITHDRAW)
				return true;
		}
		else if (m_nAgentRole == BaitRole.TEAM_SOLDIER) {
			if ((m_nTeamSoldierState == SoldierState.WAIT) || 
					(m_nTeamSoldierState == SoldierState.ATTACK)) {
				if (m_nTeamSoldierState == SoldierState.ATTACK) 
					System.out.println(getName() + " estado attack devuelve true");
				return true;
			}
		}
		// TODO indicar a que agente corresponde este codigo
		if ((m_CurrentTask.getType() == CTask.TASK_GET_OBJECTIVE) ||
				(m_CurrentTask.getType() == CTask.TASK_GOTO_POSITION))
			return true;
		return false; 
	}  
	/////////////////////////////////////////////////////////////////////////////////////////////////////

	/////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * The agent has got the objective pack. 
	 * 
	 * This method is called when this agent walks on the objective pack, getting it.
	 *   
	 * <em> It's very useful to overload this method. </em>
	 *   
	 */
	protected void ObjectivePackTaken() {
		System.out.println(getName() + " objective pack taken");
			
	} 
	/////////////////////////////////////////////////////////////////////////////////////////////////////

	/////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Definition of priorities for each kind of task. 
	 * 
	 * This method can be implemented in CTroop's derived classes to define the task's priorities in agreement to
	 * the role of the new class. Priorities must be defined in the array <tt> m_TaskPriority</tt>. 
	 *   
	 * <em> It's very useful to overload this method. </em>
	 *   
	 */
	protected void SetUpPriorities() {
		
		m_TaskPriority[CTask.TASK_NONE] = 0;
		m_TaskPriority[CTask.TASK_GIVE_MEDICPAKS] = 2000;
		m_TaskPriority[CTask.TASK_GIVE_AMMOPACKS] = 0;
		m_TaskPriority[CTask.TASK_GIVE_BACKUP] = 0;
		m_TaskPriority[CTask.TASK_GET_OBJECTIVE] = 1000;
		m_TaskPriority[CTask.TASK_ATTACK] = 1000;
		m_TaskPriority[CTask.TASK_RUN_AWAY] = 1500;
		m_TaskPriority[CTask.TASK_GOTO_POSITION] = 750;
		m_TaskPriority[CTask.TASK_PATROLLING] = 500;
		m_TaskPriority[CTask.TASK_WALKING_PATH] = 750;

	}
	/////////////////////////////////////////////////////////////////////////////////////////////////////

	/////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Action to do if this agent cannot shoot. 
	 * 
	 * This method is called when the agent try to shoot, but has no ammo. The agent will spit enemies out. :-) 
	 *   
	 * <em> It's very useful to overload this method. </em>
	 *   
	 */
	protected void PerformNoAmmoAction() {
		System.out.println(getName() + " out of ammo");
	}
	/////////////////////////////////////////////////////////////////////////////////////////////////////

	/////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Calculates a new destiny position to escape. 
	 * 
	 * This method is called before the agent creates a task for escaping. It generates a valid random point in a radius of 50 units.
	 * Once position is calculated, agent updates its destiny to the new position, and automatically calculates the new direction.
	 *   
	 * <em> It's very useful to overload this method. </em>
	 *   
	 */
	protected void GenerateEscapePosition() {
	
		while (true) {
			m_Movement.CalculateNewDestination(50, 50);
			if ( CheckStaticPosition(m_Movement.getDestination().x, m_Movement.getDestination().z) == true ) {
				m_Movement.CalculateNewOrientation();
				return;
			}
		}
	}
	/////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Calculates a new destiny position to walk. 
	 * 
	 * This method is called before the agent creates a <tt> TASK_GOTO_POSITION</tt> task.
	 *   
	 * <em> It's very useful to overload this method. </em>
	 *   
	 * @return <tt> TRUE</tt>: valid position generated / <tt> FALSE</tt> cannot generate a valid position
	 * 
	 */
	protected boolean GeneratePath() {
		//System.out.println("gp ");
		if (m_bIsLeader) {
			if (m_nLeaderState == LeaderState.GOAL_TAKEN) {
				//System.out.print("lider ");
				m_AStarPath = m_cSolver.FindPathToTarget(m_Movement.getPosition().x, 
						m_Movement.getPosition().z, m_cGoalPoint.x, m_cGoalPoint.z);
				if (m_AStarPath == null) {
					System.out.println("ERROR: La ruta es null");
					return false;
				}
				String startPos = " ( " + m_AStarPath[0].x + " , 0.0 , " + m_AStarPath[0].z + " ) ";
				m_iAStarPathIndex = 0;
				AddTask(CTask.TASK_WALKING_PATH, getAID(), startPos, m_CurrentTask.getPriority() + 1);
				return true;
			}
		}
		else if (m_nAgentRole == BaitRole.TEAM_SOLDIER) {
			//System.out.print(getName() + " ");
			m_AStarPath = m_cSolver.FindPathToTarget(m_Movement.getPosition().x, 
					m_Movement.getPosition().z, m_Map.GetTargetX(), m_Map.GetTargetZ());
			if (m_AStarPath == null) {
				System.out.println("ERROR: La ruta es null");
				return false;
			}
			String startPos = " ( " + m_AStarPath[0].x + " , 0.0 , " + m_AStarPath[0].z + " ) ";
			m_iAStarPathIndex = 0;
			AddTask(CTask.TASK_WALKING_PATH, getAID(), startPos, m_CurrentTask.getPriority() + 1);
			return true;
		}
		else if (m_nAgentRole == BaitRole.BAIT_SOLDIER) {
			
		}
		return false;
	}
	/////////////////////////////////////////////////////////////////////////////////////////////////////

	/////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Calculates an array of positions for patrolling. 
	 * 
	 * When this method is called, it creates an array of <tt> n</tt> random positions. For medics and fieldops, the rank of <tt> n</tt> is 
	 * [1..1]. For soldiers, the rank of <tt> n</tt> is [5..10].
	 *   
	 * <em> It's very useful to overload this method. </em>
	 *   
	 */
	protected void CreateControlPoints() {

		int iMaxCP = 0;
		
		switch ( m_eClass ) {
		case CLASS_MEDIC:
		case CLASS_FIELDOPS:
			super.CreateControlPoints();
			break;
			
		case CLASS_SOLDIER:
			iMaxCP = (int) (Math.random() * 5) + 5;
			m_ControlPoints = new Vector3D [iMaxCP];
			for (int i = 0; i < iMaxCP; i++ ) {
				Vector3D ControlPoints = new Vector3D();
				while (true) {
			
					double x = m_Map.GetTargetX() + (25 - (Math.random() * 50));
					double z = m_Map.GetTargetZ() + (25 - (Math.random() * 50));

					if ( CheckStaticPosition(x, z) == true ) {
						ControlPoints.x = x;
						ControlPoints.z = z;
						m_ControlPoints[i] = ControlPoints;
						break;
					}
				}
			}
			break;
			
		case CLASS_ENGINEER:
		case CLASS_NONE:
		default:
			break;
		}
	}
	/////////////////////////////////////////////////////////////////////////////////////////////////////

	/////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Action to do when an agent is being shot. 
	 * 
	 * This method is called every time this agent receives a messager from agent Manager informing it is being shot.
	 *   
	 * <em> It's very useful to overload this method. </em>
	 *   
	 */
	protected void PerformInjuryAction() {
		if (m_nAgentRole == BaitRole.BAIT) {
			m_iBaitShots++;
			if (m_iBaitShots >= SHOTS_TO_WITHDRAW) {
				if (m_nBaitState != BaitState.WITHDRAW) {
					m_nBaitState = BaitState.WITHDRAW;
					m_iRunAwayIndex = m_AStarPath.length - 1;
					SendReadyMsgToLeader();
				}
				String sPosition = " ( " + m_AStarPath[m_iRunAwayIndex].x + " , 0.0 , " + 
				m_AStarPath[m_iRunAwayIndex].z + " ) "; 
				SendReadyMsgToLeader();
				AddTask(CTask.TASK_RUN_AWAY, getAID(), sPosition, m_CurrentTask.getPriority() + 1);	
				System.out.println("retirada!" + m_CurrentTask.getType() + " " + m_CurrentTask.getPriority());
				
			}
			System.out.println("Ah me disparan! " + GetHealth());
		}
		
	}
	/////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Action to do when ammo or health values exceed the threshold allowed. 
	 * 
	 * This method is called when current values of ammo and health exceed the threshold allowed. These values are checked 
	 * by <tt> Launch_MedicAmmo_RequestBehaviour</tt> behaviour, every ten seconds. Perhaps it is convenient to create a 
	 * <tt> TASK_RUN_AWAY</tt> task.  
	 *   
	 * <em> It's very useful to overload this method. </em>
	 *   
	 */
	protected void PerformThresholdAction() {
		System.out.println("threshold action");
		/*GenerateEscapePosition();
		String sNewPosition = " ( " + m_Movement.getDestination().x + " , " + m_Movement.getDestination().y + " , " + m_Movement.getDestination().z + " ) "; 
		AddTask(CTask.TASK_RUN_AWAY, getAID(), sNewPosition, m_CurrentTask.getPriority() + 1);*/
		if (m_iBaitShots == SHOTS_TO_WITHDRAW) {
			m_nBaitState = BaitState.WITHDRAW;
			m_iRunAwayIndex = m_AStarPath.length - 1;
			String sPosition = " ( " + m_AStarPath[m_iRunAwayIndex].x + " , 0.0 , " + 
			m_AStarPath[m_iRunAwayIndex].z + " ) "; 
			AddTask(CTask.TASK_RUN_AWAY, getAID(), sPosition, m_CurrentTask.getPriority() + 1);		
		}
		
	}
	/////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Calculates if there is an enemy at sight. 
	 * 
	 * This method scans the list <tt> m_FOVObjects</tt> (objects in the Field Of View of the agent) looking for an enemy.
	 * If an enemy agent is found, a value of <tt> TRUE</tt> is returned and variable <tt> m_AimedAgent</tt> is updated.
	 * Note that there is no criterion (proximity, etc.) for the enemy found.
	 * Otherwise, the return value is <tt> FALSE</tt>.
	 *   
	 * <em> It's very useful to overload this method. </em>
	 *   
	 * @return <tt> TRUE</tt>: enemy found / <tt> FALSE</tt> enemy not found
	 * 
	 */
	@SuppressWarnings("unchecked")
	protected boolean GetAgentToAim() {
		// TODO
		if ( m_FOVObjects.isEmpty() ) {
			m_AimedAgent = null;
			return false;
		}
		
		Iterator it = m_FOVObjects.iterator();
		while ( it.hasNext() ) {						
			CSight s = (CSight) it.next();
			if ( s.getType() >= CPack.PACK_NONE ) {
				continue;
			}
	
			int eTeam = s.getTeam();
			
			if ( m_eTeam == eTeam )
				continue;
			
			m_AimedAgent = s;
			
			if (m_bIsLeader) {
				if (m_nLeaderState == LeaderState.MOVE_TEAM)
					return false;
			}
			else if (m_nAgentRole == BaitRole.BAIT) {
				if (m_nBaitState == BaitState.ATTACK) {
					return true;
				}
				else {
					m_AimedAgent = null;
					return false;
				}
			}
			else if (m_nAgentRole == BaitRole.BAIT_SOLDIER) {
				// TODO Hay que devolver true o false dependiendo de si estan esperando
				// al señuelo o ya están atacando
				return false;
			}
			else if (m_nAgentRole == BaitRole.TEAM_SOLDIER) {
				if (m_nTeamSoldierState == SoldierState.WAIT) 
					return false;
			}
			return true; 
		}
		m_AimedAgent = null;
		return false;
	}
	/////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Action to do when the agent is looking at. 
	 * 
	 * This method is called just after Look method has ended. 
	 *   
	 * <em> It's very useful to overload this method. </em>
	 * 
	 */
	protected void PerformLookAction() {	
		
		// ACCIONES DEL LIDER
		if (m_bIsLeader) {
			PerformLookActionLeader();
		} // ACCIONES DEL SEÑUELO
		else if (m_nAgentRole == BaitRole.BAIT) {
			PerformLookActionBait();
		} // ACCIONES DEL RESPALDO DEL SEÑUELO
		else if (m_nAgentRole == BaitRole.BAIT_SOLDIER){
			if (m_nBaitSoldierState == SoldierState.WAIT) {
					//&& (m_CurrentTask.getType() == CTask.TASK_GET_OBJECTIVE)) {
				AddTask(CTask.TASK_GOTO_POSITION, getAID(), m_sWaitPosition, m_CurrentTask.getPriority() + 1);
			}
		} // ACCIONES DEL RESTO DE SOLDADOS
		else {
			switch (m_nTeamSoldierState) {
			case WAIT:
				AddTask(CTask.TASK_GOTO_POSITION, getAID(), m_sWaitPosition, m_CurrentTask.getPriority() + 1);
				break;
			case ATTACK: 
				if (m_CurrentTask.getType() != CTask.TASK_GET_OBJECTIVE) {
					String sPos = " ( " + m_cGoalPoint.x + " , 0.0 , " + m_cGoalPoint.z + " ) ";
					System.out.println("!!!ts " + getName() + " se supone que hemos llegado. ataque al punto " + sPos);
					System.out.println("   tarea " + m_CurrentTask.getType() + " " + m_CurrentTask.getPriority() + " " + m_Movement.getDestination().x + " " + m_Movement.getDestination().z);
					System.out.println("   nueva tarea " + CTask.TASK_GET_OBJECTIVE + " " + (m_CurrentTask.getPriority() + 2));
					System.out.println("   estado " + m_nTeamSoldierState);
					AddTask(CTask.TASK_GET_OBJECTIVE, getAID(), sPos, m_CurrentTask.getPriority() + 1);
				}
				break;
			}
		}
	}
	/////////////////////////////////////////////////////////////////////////////////////////////////////

	// End of Methods to overload inhereted from CTroop class
	/////////////////////////////////////////////////////////////////////////////////////////////////////

	/////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Action to do when this agent reaches the target of current task. 
	 * 
	 * This method is called when this agent goes to state <em>TARGET_REACHED</em>. If current task is <tt> TASK_GIVE_MEDICPAKS</tt>, 
	 * agent must give medic packs, but in other case, it calls to parent's method.
	 *   
	 * <em> It's very useful to overload this method. </em>
	 *   
	 * @param _CurrentTask
	 * 
	 */
	protected void PerformTargetReached(CTask _CurrentTask) {
		
		switch ( _CurrentTask.getType() ) {
		case CTask.TASK_NONE:
			break;
			
		case CTask.TASK_GIVE_MEDICPAKS:
			int iPacks = _CurrentTask.getPacksDelivered();
			super.PerformTargetReached(_CurrentTask);
			if ( iPacks != _CurrentTask.getPacksDelivered() )
				System.out.println(getLocalName()+ ": Medic has left " + (_CurrentTask.getPacksDelivered() - iPacks) + " Medic Packs");
			else
				System.out.println(getLocalName()+ ": Medic cannot leave Medic Packs");
			break;
		
		case CTask.TASK_WALKING_PATH:
if (DEBUG_BAIT) {
	if (m_nAgentRole == BaitRole.BAIT)
		System.out.println("targetreached TASK_WALKING_PATH " + m_iAStarPathIndex + "/" + (m_AStarPath.length-1) );
}
			System.out.println(getName() + " targetreached TASK_WALKING_PATH " + m_iAStarPathIndex + "/" + (m_AStarPath.length-1) );
			if (m_iAStarPathIndex < m_AStarPath.length - 1) {
				m_iAStarPathIndex++;
				String startPos = " ( " + m_AStarPath[m_iAStarPathIndex].x + " , 0.0 , " + m_AStarPath[m_iAStarPathIndex].z + " ) ";
				AddTask(CTask.TASK_WALKING_PATH, getAID(), startPos, _CurrentTask.getPriority());
			} // ya llegamos al final del camino
			else {
				m_iAStarPathIndex++;
				// envio un mensaje de sincronizacion al lider
				//SendReadyMsgToLeader();
				if (m_nAgentRole == BaitRole.BAIT) {
					m_nBaitState = BaitState.ATTACK;
					//AddTask(CTask.TASK_GET_OBJECTIVE, getAID(), pos, _CurrentTask.getPriority() + 1);
					super.PerformTargetReached(_CurrentTask);
				}
				else if (m_bIsLeader) {
					// Termina el camino entre la base y el punto en el que esperan al señuelo
					if (m_nLeaderState == LeaderState.MOVE_BAIT)
						m_nLeaderState = LeaderState.SYNCHRONIZE_BAIT;
					// Termina el camino entre el punto de espera y el punto de ataque
					else if (m_nLeaderState == LeaderState.MOVE_TEAM) {
						m_nLeaderState = LeaderState.GOAL_ATTACK;
						
					}
					super.PerformTargetReached(_CurrentTask);
				}
				else if (m_nAgentRole == BaitRole.TEAM_SOLDIER) {
					if (m_nTeamSoldierState == SoldierState.MOVING)
						m_nTeamSoldierState = SoldierState.WAIT;
					if (m_nTeamSoldierState == SoldierState.MOVING_TO_ATTACK) {
						m_nTeamSoldierState = SoldierState.ATTACK;
						//String sPos = " ( " + m_cGoalPoint.x + " , 0.0 , " + m_cGoalPoint.z + " ) ";
						//AddTask(CTask.TASK_GET_OBJECTIVE, getAID(), sPos, m_CurrentTask.getPriority() + 1);
					}
					super.PerformTargetReached(_CurrentTask);
				}
				else if (m_nAgentRole == BaitRole.BAIT_SOLDIER) {
					m_nBaitSoldierState = SoldierState.WAIT;
					super.PerformTargetReached(_CurrentTask);
				}
			}
			//
			break;
		case CTask.TASK_GOTO_POSITION:
			if (m_bIsLeader) {
				if ((m_nLeaderState == LeaderState.MOVE_TEAM) || 
						(m_nLeaderState == LeaderState.GOAL_ATTACK) ||
						(m_nLeaderState == LeaderState.GOAL_TAKEN))
					super.PerformTargetReached(_CurrentTask);
			}
			else if (m_nAgentRole == BaitRole.TEAM_SOLDIER) {
				if (m_nTeamSoldierState == SoldierState.ATTACK)
					super.PerformTargetReached(_CurrentTask);
			}
			else if (m_nAgentRole == BaitRole.BAIT_SOLDIER) {
				if (m_nBaitSoldierState == SoldierState.WAIT) {
					super.PerformTargetReached(_CurrentTask);
				}
			}
			
			//System.out.println("targetReached goto position");
			break;
		case CTask.TASK_RUN_AWAY:
			if (m_nAgentRole == BaitRole.BAIT){
				if (m_nBaitState == BaitState.GOAL_TAKEN) {
					if (m_iRunAwayIndex == m_AStarPath.length - 5) {
						// enviar un mensaje al medico y al fieldop para que dejen los packs
						ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
						msg.setContent(" ( " + BaitCommand.GIVE_PACKS + " ) ");
						msg.addReceiver(m_cMedicAid);
						msg.addReceiver(m_cFieldOpAid);
						msg.setConversationId("COMMAND");
						send(msg);
					}
					m_iRunAwayIndex--;
				}
				else { 
					if (m_iRunAwayIndex > m_AStarPath.length - 6) {
						m_iRunAwayIndex--;
						if (m_iRunAwayIndex == m_AStarPath.length - 6) {
							// enviar un mensaje al medico y al fieldop para que dejen los packs
							ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
							msg.setContent(" ( " + BaitCommand.GIVE_PACKS + " ) ");
							msg.addReceiver(m_cMedicAid);
							msg.addReceiver(m_cFieldOpAid);
							msg.setConversationId("COMMAND");
							send(msg);
						}
					}
					else
						m_nBaitState = BaitState.HOLD;
				}
			}
			if (m_bIsLeader){
				if (m_nLeaderState == LeaderState.GOAL_TAKEN) {
					if (m_iRunAwayIndex == m_AStarPath.length - 6) {
						// enviar un mensaje al medico y al fieldop para que dejen los packs
						ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
						msg.setContent(" ( " + BaitCommand.GIVE_PACKS + " ) ");
						// TODO Hay que seleccionar el medico y fieldop del equipo principal
						msg.addReceiver(m_cMedicAid);
						msg.addReceiver(m_cFieldOpAid);
						msg.setConversationId("COMMAND");
						//send(msg);
					}
					m_iRunAwayIndex--;
				}
			}
			break;
		case CTask.TASK_GET_OBJECTIVE:
			if (m_bIsLeader) {
				if (m_nLeaderState != LeaderState.GOAL_TAKEN) {
					System.out.println("!!!!!coge el objetivo");
					m_nLeaderState = LeaderState.GOAL_TAKEN;
					m_iRunAwayIndex = m_AStarPath.length - 1;
					String sPosition = " ( " + m_cBasePoint.x + " , 0.0 , " + 
					m_cBasePoint.z + " ) "; 
					AddTask(CTask.TASK_GET_OBJECTIVE, getAID(), sPosition, m_CurrentTask.getPriority());	
					sPosition = " ( " + m_AStarPath[m_iRunAwayIndex].x + " , 0.0 , " + 
						m_AStarPath[m_iRunAwayIndex].z + " ) "; 
					AddTask(CTask.TASK_RUN_AWAY, getAID(), sPosition, m_CurrentTask.getPriority() + 1);	
					System.out.println("(lider) retirada a la base!" + m_CurrentTask.getType() + " " + m_CurrentTask.getPriority());
				}
				else {
					// Alguien ya cogio la bandera, volvemos a la base
					m_nLeaderState = LeaderState.RETURN_BASE;
					if (m_CurrentTask.getType() != CTask.TASK_WALKING_PATH) {
						SendMsgReturnBase();
					}
				}
			}
			else if (m_nAgentRole == BaitRole.BAIT) {
				// Envia un mensaje al lider y se dirige a la base
				m_nBaitState = BaitState.GOAL_TAKEN;
				m_iRunAwayIndex = m_AStarPath.length - 1;
				String sPosition = " ( " + m_AStarPath[m_iRunAwayIndex].x + " , 0.0 , " + 
				m_AStarPath[m_iRunAwayIndex].z + " ) "; 
				AddTask(CTask.TASK_RUN_AWAY, getAID(), sPosition, m_CurrentTask.getPriority() + 1);	
				System.out.println("retirada a la base!" + m_CurrentTask.getType() + " " + m_CurrentTask.getPriority());
				SendGoalTakenMsgToLeader();
			}
			else if (m_nAgentRole == BaitRole.TEAM_SOLDIER) {
				if (m_nTeamSoldierState != SoldierState.GOAL_TAKEN) {
					SendGoalTakenMsgToLeader();
					m_nTeamSoldierState = SoldierState.GOAL_TAKEN;
					m_iRunAwayIndex = m_AStarPath.length - 1;
					String sPosition = " ( " + m_AStarPath[m_iRunAwayIndex].x + " , 0.0 , " + 
					m_AStarPath[m_iRunAwayIndex].z + " ) "; 
					AddTask(CTask.TASK_RUN_AWAY, getAID(), sPosition, m_CurrentTask.getPriority() + 1);	
					System.out.println("(soldado) retirada a la base!" + m_CurrentTask.getType() + " " + m_CurrentTask.getPriority());
				}
				else {
					// Alguien ya cogio la bandera, volvemos a la base
				}
			}
			break;
		default:
			super.PerformTargetReached(_CurrentTask);
		break;
		}
	}
	/////////////////////////////////////////////////////////////////////////////////////////////////////

	// End of Methods to overload inhereted from CMedic class
	/////////////////////////////////////////////////////////////////////////////////////////////////////
	protected void PerformLookActionLeader() {
		// TODO
		switch (m_nLeaderState) {
		case DEFINE_POINTS:
			//SendWaitCommandEverybody();
			// Genera los puntos clave
			m_cBaitLib.GenerateBaitPoints(m_Map, m_Movement.getDestination(), m_Movement.getPosition());
			m_nLeaderState = LeaderState.MOVE_BAIT;
			SendBaitCommandMovement();
			SendCommandMovement();
			break;
		case MOVE_BAIT:
			// En este estado el señuelo se esta moviendo
			break;
		case SYNCHRONIZE_BAIT: 
			// Esperar a recibir el mensaje del señuelo
			if (m_bBaitPrepared) { 
				System.out.println("El señuelo ha llegado, vamos!");
				m_nLeaderState = LeaderState.MOVE_TEAM;
				// Ordena el movimiento del resto del equipo (el señuelo ya esta atacando)
				SendCommandAttack();
			}
			else {
				// Mantenerse en el punto
				//System.out.println("esperaaaa...." + m_CurrentTask.getType() + " " + m_CurrentTask.getPriority());
				AddTask(CTask.TASK_GOTO_POSITION, getAID(), m_sWaitPosition, m_CurrentTask.getPriority() + 1);
			}
			break;
		case MOVE_TEAM:
if (DEBUG_LEADER) {
System.out.println("Lider (MOVE_TEAM) " + m_CurrentTask.getType() + " " + m_CurrentTask.getPriority());
}
			//if (m_iTeamAgentPrepared == 0) {
				//System.out.println("ya estamos todos, al ataque!");
				//m_nLeaderState = LeaderState.GOAL_ATTACK;
				// Enviar orden de ataque al equipo principal
				// SendCommandAttack();
			//}
			break;
		case GOAL_ATTACK:
if (DEBUG_LEADER) {
System.out.println("Lider (GOAL_ATTACK) " + m_CurrentTask.getType() + " " + m_CurrentTask.getPriority());
}
			// El equipo esta atacando
			if (m_CurrentTask.getType() != CTask.TASK_GET_OBJECTIVE) {
				String sPos = " ( " + m_cGoalPoint.x + " , 0.0 , " + m_cGoalPoint.z + " ) ";
				System.out.println("!!!" + getName() + " se supone que hemos llegado. ataque al punto " + sPos);
				System.out.println("   tarea" + m_CurrentTask.getType() + " " + m_CurrentTask.getPriority() + " " + m_Movement.getDestination().x + " " + m_Movement.getDestination().z);
				System.out.println("   nueva tarea " + CTask.TASK_GET_OBJECTIVE + " " + (m_CurrentTask.getPriority() + 2));
				System.out.println("   estado " + m_nTeamSoldierState);
				AddTask(CTask.TASK_GET_OBJECTIVE, getAID(), sPos, m_CurrentTask.getPriority() + 2);
			}
			break;
		case GOAL_TAKEN:
if (DEBUG_LEADER) {
System.out.println("Lider (GOAL_TAKEN) " + m_CurrentTask.getType() + " " + m_CurrentTask.getPriority());
}
			break;
		case RETURN_BASE:
			
			break;
		}
	
	}
	
	protected void PerformLookActionBait() {
		String sPosition;
		switch (m_nBaitState) {
		case WAIT:
			// Se cambia cuando llega el mensaje del lider
			break;
		case MOVING:
if (DEBUG_BAIT) {
System.out.println("Bait (moving): " + m_CurrentTask.getType() + " " + m_CurrentTask.getPriority());
}
			break;
		case ATTACK:
			// Ataca hasta que se superan los umbrales de armas o salud
if (DEBUG_BAIT) {
System.out.println("Bait (attack): " + m_CurrentTask.getType() + " " + m_CurrentTask.getPriority());
}
			if ((m_CurrentTask.getType() == CTask.TASK_WALKING_PATH) || 
					(m_CurrentTask.getType() == CTask.TASK_GOTO_POSITION)) {
				String pos = " ( " + m_Movement.getPosition().x + " , 0.0 , " + 
				m_Movement.getPosition().z + " ) ";
				AddTask(CTask.TASK_GOTO_POSITION, getAID(), pos, m_CurrentTask.getPriority() + 1);
			}
			break;
		case WITHDRAW:
			// Retirada
if (DEBUG_BAIT) {
System.out.println("Retirada con tarea " + m_CurrentTask.getType() + " " + m_CurrentTask.getPriority() + 
	"[" + m_iRunAwayIndex + "]");
}
			sPosition = " ( " + m_AStarPath[m_iRunAwayIndex].x + " , 0.0 , " + 
				m_AStarPath[m_iRunAwayIndex].z + " ) "; 
			if (m_CurrentTask.getType() == CTask.TASK_RUN_AWAY)
				AddTask(CTask.TASK_RUN_AWAY, getAID(), sPosition, m_CurrentTask.getPriority());
			else
				AddTask(CTask.TASK_RUN_AWAY, getAID(), sPosition, m_CurrentTask.getPriority() + 1);
			break;
		case HOLD:
if (DEBUG_BAIT) {
System.out.println("Bait (hold) " + m_CurrentTask.getType() + " " + m_CurrentTask.getPriority() + 
	"[" + m_iRunAwayIndex + "]");
}
			break;
		case GOAL_TAKEN:
if (DEBUG_BAIT) {
System.out.println("Bait (goal_taken): " + m_CurrentTask.getType() + " " + m_CurrentTask.getPriority());
}
			sPosition = " ( " + m_AStarPath[m_iRunAwayIndex].x + " , 0.0 , " + 
				m_AStarPath[m_iRunAwayIndex].z + " ) "; 
			if (m_CurrentTask.getType() == CTask.TASK_RUN_AWAY)
				AddTask(CTask.TASK_RUN_AWAY, getAID(), sPosition, m_CurrentTask.getPriority());
			else
				AddTask(CTask.TASK_RUN_AWAY, getAID(), sPosition, m_CurrentTask.getPriority() + 1);
			break;
		}
	
	}

}



