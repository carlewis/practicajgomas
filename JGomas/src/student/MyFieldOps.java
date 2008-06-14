package student;


import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import java.util.Iterator;

import student.MyComponents.BaitRole;
import student.PathFinding.PathFindingSolver;

import es.upv.dsic.gti_ia.jgomas.*;

/**
 * 
 * @version 1.0
 * @author carlos
 */
public class MyFieldOps extends CFieldOps {
	
	private final boolean DEBUG_FIELDOP = false;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected PathFindingSolver m_cSolver;
	protected BaitLib m_cBaitLib;
	/**
	 * Nombre del servicio de comunicaciones. Depende del equipo del agente
	 */
	protected String m_sCommunicationsService;
	/**
	 * Tipo del agente AgentType.SOLDIER
	 */
	protected MyComponents.AgentType m_nAgentType;
	/**
	 * Rol dentro de la estrategia del señuelo
	 */
	protected MyComponents.BaitRole m_nAgentRole;
	/**
	 * AID del lider del grupo
	 */
	protected AID m_TeamLeader = null;
	/**
	 * Posicion para esperar en el estado WAIT 
	 */
	protected String m_sWaitPosition;
	/** Estados del fieldops */
	protected enum FieldOpState { NO_STATE, MOVING, WAIT, WORK };
	/** Estado actual del fieldops */
	protected FieldOpState m_nFieldOpState = FieldOpState.NO_STATE;
	
	protected void setup() {
	
		AddServiceType("Communications");
		super.setup();
		// Definimos el tipo de Agente
		m_nAgentType = MyComponents.AgentType.FIELDOPS;
		// Definimos el nombre de los servicios
		if (m_eTeam == TEAM_AXIS) {
			m_sCommunicationsService = "Communications_Axis";
		}
		else {
			m_sCommunicationsService = "Communications_Allied";
		}
		SetUpPriorities();
		// Comienza la comunicacion con el resto de agentes
		StartAgentCommunications();
		m_cSolver = new PathFindingSolver();
		m_cSolver.setMap(m_Map);
		m_cBaitLib = new BaitLib();
		m_cBaitLib.setPathFindingSolver(m_cSolver);
		m_cBaitLib.setGoal(m_Movement.getDestination());
	}
	/**
	 * Comienza la comunicacion entre el agente y el resto del equipo
	 */
	protected void StartAgentCommunications() {
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
				System.out.println("Existen agentes");
				ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
				for ( int i = 0; i < result.length; i++ ) {
					AID agent = result[i].getName();
					// No nos lo enviamos a nosotros mismos
					if (!agent.equals(getName())) {
						System.out.println(getName() + " a " + agent.getName());
						msg.addReceiver(agent);
					}
				}
				msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
				msg.setConversationId("COMM_SUBSCRIPTION");
				msg.setContent(" ( " + m_nAgentType + " ) ");
				send(msg);
			}
			else {
				System.out.println("No hay ningun agente");
			}
		} catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
	}
	
	private void LaunchCommunicationsBehaviour() {
		addBehaviour(new FieldOpComm(this));
	}		
		/*new CyclicBehaviour() {
			private static final long serialVersionUID = 1L;

			private MyComponents.AgentType ContentsToAgentType(String s) {
				StringTokenizer tokens = new StringTokenizer(s);
				tokens.nextToken(); // Quita (
				//AgentType retValue = (AgentType) Integer.parseInt(tokens.nextToken());
				return MyComponents.parseAgentType(tokens.nextToken());
			}
			private BaitRole ContentsToBaitRole(String s) {
				StringTokenizer tokens = new StringTokenizer(s);
				tokens.nextToken(); // Quita (
				return MyComponents.parseBaitRole(tokens.nextToken());
			}
			public void action() {
				MessageTemplate template = MessageTemplate.MatchAll();
				ACLMessage msgLO = receive(template);
				if (msgLO != null) {
					if (msgLO.getConversationId() == "COMM_SUBSCRIPTION") {
						// 
						AID cSender = msgLO.getSender();
						System.out.println("Leida suscripcion de agente tipo " + 
								ContentsToAgentType(msgLO.getContent()));
						cSender.getName();
						//AgentType nType = ContentsToAgentType(msgLO.getContents());
					}
					else if (msgLO.getConversationId() == "ROLE_PROTOCOL") {
						m_nAgentRole = ContentsToBaitRole(msgLO.getContent());
						if (ContentsToBaitRole(msgLO.getContent()) == BaitRole.BAIT_FIELDOP)
							System.out.println(getName() + " yo soy el que lleva municion al puteado");	
					}
					// else if (msgLO.getConversationId() == "LO QUE SEA") {}
				}
	
			}
		});
		
	}*/
	/**
	 * @param aid
	 */
	public void setTeamLeader(AID aid) {
		m_TeamLeader = aid;
	}
	/**
	 * Genera una tarea goto position. Cuando llega espera al siguiente comando
	 */
	public void WaitForCommand() {
		System.out.println("fieldops: añadiendo tarea esperar " + CTask.TASK_GOTO_POSITION + " " + 
				(m_CurrentTask.getPriority() + 1));
		m_sWaitPosition = " ( " + m_Movement.getPosition().x + " , 0.0 , " + m_Movement.getPosition().z + " ) ";
		m_nFieldOpState = FieldOpState.WAIT;
		AddTask(CTask.TASK_GOTO_POSITION, getAID(), m_sWaitPosition, m_CurrentTask.getPriority() + 1);
		System.out.println("fieldops: " + m_CurrentTask.getType() + " " + m_CurrentTask.getPriority());
	}
	/**
	 * Añade una tarea TASK_WALKING_PATH para ir a un punto
	 */
	public void AddTaskGoto(Vector3D point) {
		System.out.println("añadiendo tarea ir a ( " + point.x + " , " + point.z + " )");
		// TODO
		System.out.println("punto " + point.x + " " + point.z);
		/*if (m_cSolver == null) {
			m_cSolver = new PathFindingSolver();
			m_cSolver.setMap(m_Map);
		}*/
		m_AStarPath = m_cSolver.FindBaitPath(m_Movement.getPosition().x, m_Movement.getPosition().z,
				point.x, point.z);
		if (m_AStarPath == null)
			System.out.println("fieldops: la ruta es null");
		String startPos = " ( " + m_AStarPath[0].x + " , 0.0 , " + m_AStarPath[0].z + " ) ";
		m_iAStarPathIndex = 0;
		System.out.print(" ( " + m_AStarPath[0].x + " , " + m_AStarPath[0].z + " )->"); 
		System.out.println(" ( " + m_AStarPath[m_AStarPath.length - 1].x + " , " + m_AStarPath[m_AStarPath.length - 1].z + " )"); 
		AddTask(CTask.TASK_WALKING_PATH, getAID(), startPos, m_CurrentTask.getPriority() + 1);
		m_nFieldOpState = FieldOpState.MOVING;
	}	
	/**
	 * 
	 * @param ai
	 */
	public void AddAgent(AgentInfo ai) {
	/*	if (ai.type == AgentType.SOLDIER)
			m_iTeamSoldiersCount++;
		m_TeamAgents.add(ai);*/
	}
	/**
	 * 
	 * @param role
	 */
	public void setAgentRole(BaitRole role) {
		m_nAgentRole = role;
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
	public void GiveAmmoPacks() {
		CreateAmmoPack();
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
	protected void UpdateTargets() {} 
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
	protected void ObjectivePackTaken() {} // Should we do anything when we take the objective pack? 
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
	protected void PerformNoAmmoAction() {}
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
			if (m_nAgentRole == BaitRole.BAIT_FIELDOP) {
					// TODO Hay que devolver true o false dependiendo de si estan esperando
					// al señuelo o ya están atacando
					return false;
			}
			System.out.println("veo un tio");
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
if (DEBUG_FIELDOP) {
if (m_nAgentRole == BaitRole.BAIT_FIELDOP)
	System.out.println("Fieldop: " + m_CurrentTask.getType() + " " + 
	m_CurrentTask.getPriority() + " (" + m_Movement.getPosition().x + "," + 
	m_Movement.getPosition().z + ")" + m_Movement.getDestination().x + "-" + 
	m_Movement.getDestination().z + "   " + m_iAStarPathIndex);
}
		if ((m_nFieldOpState == FieldOpState.WAIT) && (m_CurrentTask.getType() == CTask.TASK_GET_OBJECTIVE))
			AddTask(CTask.TASK_GOTO_POSITION, getAID(), m_sWaitPosition, m_CurrentTask.getPriority() + 1);
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
if (DEBUG_FIELDOP) {
	System.out.println("fieldops targetreached TASK_WALKING_PATH " + m_iAStarPathIndex + "/" + (m_AStarPath.length-1));
}
			if (m_iAStarPathIndex < m_AStarPath.length - 1) {
				m_iAStarPathIndex++;
				String startPos = " ( " + m_AStarPath[m_iAStarPathIndex].x + " , 0.0 , " + 
					m_AStarPath[m_iAStarPathIndex].z + " ) ";
				AddTask(CTask.TASK_WALKING_PATH, getAID(), startPos, _CurrentTask.getPriority());
			} // ya llegamos al final del camino
			else {
				// envio un mensaje de sincronizacion al lider.
				if (m_nFieldOpState == FieldOpState.MOVING) {
					SendReadyMsgToLeader();
					m_nFieldOpState = FieldOpState.WAIT;
					m_sWaitPosition = " ( " + m_Movement.getPosition().x + " , 0.0 , " + m_Movement.getPosition().z + " ) ";
				}
				super.PerformTargetReached(_CurrentTask);
			}	
			break;
		case CTask.TASK_GOTO_POSITION:
			break;
			
		default:
			System.out.println("otro target reached");
			super.PerformTargetReached(_CurrentTask);
			break;
		}
	}
	/////////////////////////////////////////////////////////////////////////////////////////////////////

	// End of Methods to overload inhereted from CMedic class
	/////////////////////////////////////////////////////////////////////////////////////////////////////


}



