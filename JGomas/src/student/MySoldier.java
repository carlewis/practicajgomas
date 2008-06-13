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
 * @version 1.0
 * @author carlos
 */
// TODO estrategias de defensa, cancelar estrategia de ataque...
public class MySoldier extends CSoldier {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
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
	/** Puja para ser el lider */
	protected double m_fLeaderBid;
	/**
	 * Tiene conocimiento de un lider
	 */
	protected boolean m_bExistsLeader = false;
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
		MOVE_TEAM, SYNCHRONIZE_TEAM, GOAL_ATTACK};
	/** Estado actual del lider */
	protected LeaderState m_nLeaderState = LeaderState.NO_STATE;
	/** Estados del señuelo */
	protected enum BaitState { NO_STATE, WAIT, MOVING, ATTACK, WITHDRAW };
	/** Estado actual del señuelo */
	protected BaitState m_nBaitState = BaitState.NO_STATE;
	/** */
	protected boolean m_bBaitPrepared = false;
	protected boolean m_bBaitFieldOpPrepared = false;
	protected int m_iTeamAgentPrepared;
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
			// Busca los agentes con servicio Comunications
			DFAgentDescription dfd = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType(m_sCommunicationsService);
			dfd.addServices(sd);
			DFAgentDescription[] result = DFService.search(this, dfd);
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
		msgBait.setContent(" ( " + MyComponents.BaitRole.BAIT + " ) ");
		
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
				continue;
			}
			if (!bBaitMedic && (ai.type == AgentType.MEDIC)) {
				msgBMedic.addReceiver(ai.aid);
				bBaitMedic = true;
				continue;
			}
			if (!bBaitFieldOp && (ai.type == AgentType.FIELDOPS)) {
				msgBFieldOp.addReceiver(ai.aid);
				bBaitFieldOp = true;
				continue;
			}
			if (!bBaitSoldier && (ai.type == AgentType.MEDIC)) {
				msgBSoldier.addReceiver(ai.aid);
				bBaitSoldier = true;
				continue;
			}
			// Resto
			msgOther.addReceiver(ai.aid);
		}

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
		if (m_nAgentRole == BaitRole.BAIT)
			m_Threshold.SetAmmo(10);
		//m_Threshold.SetAmmo
	}
	
	/**
	 * Comprueba la calidad del camino desde el punto del señuelo al de retirada
	 * En caso necesario modifica el punto de retirada
	 */
	protected void CheckBaitPathToWithdraw(Vector3D path) {
		
	}
	/**
	 * Envia al señuelo y su equipos sus coordenadas
	 */
	protected void SendBaitCommandMovement() {
		// TODO 
		ACLMessage msgBaitPoint = new ACLMessage(ACLMessage.INFORM);
		ACLMessage msgTeamPoint = new ACLMessage(ACLMessage.INFORM);
		// Selecciona al señuelo
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
		Vector3D cBaitPoint = BaitLib.getBaitAttackPoint();
		msgBaitPoint.setContent(" ( GOTO , " + cBaitPoint.x + " , 0.0 , " + cBaitPoint.z + " ) ");
		// TODO indicar al equipo del señuelo un punto diferente
		msgTeamPoint.setContent(" ( GOTO , " + cBaitPoint.x + " , 0.0 , " + cBaitPoint.z + " ) ");
		send(msgBaitPoint);
		send(msgTeamPoint);
	}
	/**
	 * Envia al resto de agentes el comando de movimiento
	 */
	protected void SendCommandMovement() {
		// TODO 
		ACLMessage msgTeamPoint = new ACLMessage(ACLMessage.INFORM);
		//ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
		// Selecciona al señuelo
		for (int i = 0; i < m_TeamAgents.size(); i++) {
			if (m_TeamAgents.get(i).role == BaitRole.TEAM_SOLDIER)
				msgTeamPoint.addReceiver(m_TeamAgents.get(i).aid);
			// else if ...
		}
		msgTeamPoint.setConversationId("COMMAND");
		Vector3D cTeamPoint = BaitLib.getAttackPoint();
		msgTeamPoint.setContent(" ( GOTO , " + cTeamPoint.x + " , 0.0 , " + cTeamPoint.z + " ) ");
		send(msgTeamPoint);
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
	public void AddTaskGoto(Vector3D point) {
		System.out.println("añadiendo tarea ir a ( " + point.x + " , " + point.z + " )");
		// TODO
		m_AStarPath = PathFindingSolver.FindBaitPath(m_Movement.getPosition().x, m_Movement.getPosition().z,
				point.x, point.z);
		String startPos = " ( " + m_AStarPath[0].x + " , 0.0 , " + m_AStarPath[0].z + " ) ";
		m_iAStarPathIndex = 0;
		System.out.print(" ( " + m_AStarPath[0].x + " , " + m_AStarPath[0].z + " )->"); 
		System.out.println(" ( " + m_AStarPath[m_AStarPath.length - 1].x + " , " + m_AStarPath[m_AStarPath.length - 1].z + " )"); 
		AddTask(CTask.TASK_WALKING_PATH, getAID(), startPos, m_CurrentTask.getPriority() + 1);
		// Cambia de estado
		if (m_nAgentRole == BaitRole.BAIT)
			m_nBaitState = BaitState.MOVING;
	}
	/**
	 * 
	 */
	public void SetAgentPrepared(AID _aid) {
		for (int i = 0; i < m_TeamAgents.size(); i++) {
			if (m_TeamAgents.get(i).aid.equals(_aid)) {
				if (m_TeamAgents.get(i).role == BaitRole.BAIT)
					m_bBaitPrepared = true;
				else if (m_TeamAgents.get(i).role == BaitRole.BAIT_FIELDOP)
					m_bBaitFieldOpPrepared = true;
				else if (m_TeamAgents.get(i).role == BaitRole.TEAM_SOLDIER)
					m_iTeamAgentPrepared--;
			}
		}
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
		//System.out.println("UpdateTargets");
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
		if (m_CurrentTask.getType() == CTask.TASK_GET_OBJECTIVE)
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
	} // Should we do anything when we take the objective pack? 
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
	 * This method is called before the agent creates a <tt> TASK_GOTO_POSITION</tt> task. It will try (for 5 attempts) to generate a
	 * valid random point in a radius of 20 units. If it doesn't generate a valid position in this cycle, it will try it in next cycle. 
	 * Once a position is calculated, agent updates its destination to the new position, and automatically calculates the new direction.
	 *   
	 * <em> It's very useful to overload this method. </em>
	 *   
	 * @return <tt> TRUE</tt>: valid position generated / <tt> FALSE</tt> cannot generate a valid position
	 * 
	 */
	protected boolean GeneratePath() {
		/*if ((m_AStarPath = PathFindingSolver.FindPathToTarget(m_Map, m_Movement)) != null) { 
			String startPos;
			startPos = " ( " + m_AStarPath[0].x + " , 0.0 , " + m_AStarPath[0].z + " ) ";
			AddTask(CTask.TASK_WALKING_PATH, getAID(), startPos, m_CurrentTask.getPriority() + 1);
			return true;
		}*/
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
		System.out.println("Ah me disparan!");
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
		
		GenerateEscapePosition();
		String sNewPosition = " ( " + m_Movement.getDestination().x + " , " + m_Movement.getDestination().y + " , " + m_Movement.getDestination().z + " ) "; 
		AddTask(CTask.TASK_RUN_AWAY, getAID(), sNewPosition, m_CurrentTask.getPriority() + 1);
		
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
			
			if (m_nAgentRole != BaitRole.BAIT)
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
		if (m_bIsLeader) {
			//System.out.println("Look");
			// TODO
			switch (m_nLeaderState) {
			case DEFINE_POINTS:
				// Genera los puntos clave
				PathFindingSolver.setMap(m_Map);
				BaitLib.GenerateBaitPoints(m_Movement.getDestination(), m_Movement.getPosition());
				m_nLeaderState = LeaderState.MOVE_BAIT;
				break;
			case MOVE_BAIT:
				// Enviar orden de movimiento al señuelo
				SendBaitCommandMovement();
				m_nLeaderState = LeaderState.SYNCHRONIZE_BAIT;
				break;
			case SYNCHRONIZE_BAIT: 
				// Esperar a recibir el mensaje del señuelo
				// Mensaje del señuelo
				if (m_bBaitPrepared) { 
					System.out.println("El señuelo ha llegado, vamos!");
					m_nLeaderState = LeaderState.SYNCHRONIZE_TEAM;
					// Ordena el movimiento del resto del equipo (el señuelo ya esta atacando)
					m_iTeamAgentPrepared = m_iTeamSize - 4; 
					System.out.println("Tiene que haber " + m_iTeamAgentPrepared + " agentes ");
					SendCommandMovement();
					// movimiento para mi mismo
					Vector3D cTeamPoint = BaitLib.getAttackPoint();
					m_AStarPath = PathFindingSolver.FindBaitPath(m_Movement.getPosition().x, m_Movement.getPosition().z,
							cTeamPoint.x, cTeamPoint.z);
					String startPos = " ( " + m_AStarPath[0].x + " , 0.0 , " + m_AStarPath[0].z + " ) ";
					m_iAStarPathIndex = 0;
					AddTask(CTask.TASK_WALKING_PATH, getAID(), startPos, m_CurrentTask.getPriority() + 1);
				}
				break;
			case SYNCHRONIZE_TEAM:
				// Espera a que llegue el equipo completo
				if (m_iTeamAgentPrepared == 0) {
					System.out.println("ya estamos todos, al ataque!");
					m_nLeaderState = LeaderState.GOAL_ATTACK;
					// Enviar orden de ataque al equipo principal
					// SendCommandAttack();
				}
				break;
			case GOAL_ATTACK:
				// El equipo esta atacando
				
				break;
			}
		}
		
		if (m_nAgentRole == BaitRole.BAIT) {
			switch (m_nBaitState) {
			case WAIT:
				// Se cambia cuando llega el mensaje del lider
				break;
			case MOVING:
				// Cambia cuando llega al destino
				break;
			case ATTACK:
				// Ataca hasta que se superan los umbrales de armas o salud
				break;
			case WITHDRAW:
				// Retirada
				break;
			}
			if (m_AStarPath != null) {
				System.out.println("bait ->" + m_CurrentTask.getType() + " " + m_CurrentTask.getPriority() + "    " + 
					m_Movement.getPosition().x + " " + m_Movement.getPosition().z + "     "
					+ m_AStarPath[m_iAStarPathIndex].x + " " + m_AStarPath[m_iAStarPathIndex].z);
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
			System.out.println("targetreached TASK_WALKING_PATH " + m_iAStarPathIndex + "/" + (m_AStarPath.length-1) );
			if (m_iAStarPathIndex < m_AStarPath.length - 1) {
				m_iAStarPathIndex++;
				String startPos = " ( " + m_AStarPath[m_iAStarPathIndex].x + " , 0.0 , " + m_AStarPath[m_iAStarPathIndex].z + " ) ";
				AddTask(CTask.TASK_WALKING_PATH, getAID(), startPos, _CurrentTask.getPriority());
			} // ya llegamos al final del camino
			else {
				// envio un mensaje de sincronizacion al lider
				if (!m_bIsLeader) {
					SendReadyMsgToLeader();
					if (m_nAgentRole == BaitRole.BAIT) {
						String pos = " ( " + m_Movement.getDestination().x + " , 0.0 , " + 
							m_Movement.getDestination().z + " ) ";
						AddTask(CTask.TASK_GET_OBJECTIVE, getAID(), pos, _CurrentTask.getPriority() + 1);
					}
				}
			}
			//super.PerformTargetReached(_CurrentTask);
			break;
		default:
			super.PerformTargetReached(_CurrentTask);
			break;
		}
	}
	/////////////////////////////////////////////////////////////////////////////////////////////////////

	// End of Methods to overload inhereted from CMedic class
	/////////////////////////////////////////////////////////////////////////////////////////////////////


}



