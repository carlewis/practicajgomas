package student;

import student.PathFinding.PathFindingSolver;
import es.upv.dsic.gti_ia.jgomas.Vector3D;
import java.util.Vector;


public class BaitLib {
	// Mapa
	/** Posicion de la bandera */
	private static Vector3D cGoal;
	/** Distancia entre la bandera y los puntos de ataque */
	private static final double BAIT_RADIOUS = 40.0;
	/** Puntos de ataque que estan opuestos */
	private static Vector<Vector3D> cFirstQualityPoints = new Vector<Vector3D>();
	/** Puntos de ataque que estan separados */
	private static Vector<Vector3D> cSecondQualityPoints = new Vector<Vector3D>();
	/** Punto de ataque del señuelo */
	private static Vector3D m_cBaitAttackPoint;
	/** Punto de ataque del resto del equipo */
	private static Vector3D m_cAttackPoint;
	/**
	 * Genera los puntos claves dentro de la estrategia de ataque del señuelo
	 * Utilizado por el Lider del grupo
	 * @param cGoal: Posicion de la bandera
	 * @param cBase: Posicion de la base del atacante
	 */
	public static void GenerateBaitPoints(Vector3D cGoal, Vector3D cBase) {
		BaitLib.cGoal = cGoal;
		// posicion del objetivo		
		System.out.println("la bandera esta en " + cGoal.x + ", " + cGoal.z);
		// posicion de la base?
		System.out.println("la base en " + cBase.x + ", " + cBase.z);
		// punto de ataque del señuelo: se encuentra en la recta entre la base y 
		// la bandera
		
		// Generamos los puntos entre los que podemos elegir los de ataque
		Vector3D cAttackPoints[] = new Vector3D[8];
		double dGoalXIncs[] = {-BAIT_RADIOUS, -BAIT_RADIOUS, -BAIT_RADIOUS, 0, 
				0, BAIT_RADIOUS, BAIT_RADIOUS,  BAIT_RADIOUS };		
		double dGoalZIncs[] = {-BAIT_RADIOUS, 0, BAIT_RADIOUS, -BAIT_RADIOUS, 
				BAIT_RADIOUS, -BAIT_RADIOUS, 0, BAIT_RADIOUS};
		for (int i = 0; i < 8; i++) {
			cAttackPoints[i] = new Vector3D();
			cAttackPoints[i].x = cGoal.x + dGoalXIncs[i];
			cAttackPoints[i].z = cGoal.z + dGoalZIncs[i];
		}
		// Comprobamos si se puede alcanzar el objetivo en linea recta
		boolean bValidPoints[] = new boolean[8];
		for (int i = 0; i < 8; i++) 
			bValidPoints[i] = PathFindingSolver.CheckDirectPath(cAttackPoints[i], cGoal);
		// Seleccionamos los puntos de primera y segunda categoría
		for (int i = 0; i < 4; i++) {
			// Los dos puntos opuestos son validos
			if (bValidPoints[i] && bValidPoints[7-i]) {
				cFirstQualityPoints.add(cAttackPoints[i]);
				cFirstQualityPoints.add(cAttackPoints[7 - i]);
			}
		}
		GetSecondQualityPoints(bValidPoints, cAttackPoints);
		ShowPoints();
		System.out.println("Todos los puntos:");
		for (int i = 0; i < 8; i++)
			System.out.print("(" + cAttackPoints[i].x + "," + cAttackPoints[i].z + ")  ");
		System.out.println("");
		
		// Buscamos las rutas entre la base y los puntos seleccionados en los 
		// puntos de primera categoria
		for (int i = 0; i < cFirstQualityPoints.size(); i += 2) {
			if ((PathFindingSolver.FindBaitPath(cBase.x, cBase.z,
						cFirstQualityPoints.get(i).x, cFirstQualityPoints.get(i).z) == null) ||
				(PathFindingSolver.FindBaitPath(cBase.x, cBase.z,
						cFirstQualityPoints.get(i+1).x, cFirstQualityPoints.get(i+1).z) == null)) {
				// Se eliminan los dos puntos
				cFirstQualityPoints.remove(i);
				cFirstQualityPoints.remove(i);
				System.out.println("eliminando " + i);
				i -= 2;
			}
		}
		// Buscamos las rutas entre la base y los puntos seleccionados en los 
		// puntos de segunda categoría
		for (int i = 0; i < cSecondQualityPoints.size(); i += 2) {
			if ((PathFindingSolver.FindBaitPath(cBase.x, cBase.z,
						cSecondQualityPoints.get(i).x, cSecondQualityPoints.get(i).z) == null) ||
				(PathFindingSolver.FindBaitPath(cBase.x, cBase.z,
						cSecondQualityPoints.get(i+1).x, cSecondQualityPoints.get(i+1).z) == null)) {
				// Se eliminan los dos puntos
				cSecondQualityPoints.remove(i);
				cSecondQualityPoints.remove(i);
				System.out.println("eliminando " + i);
				i -= 2;
			}
		}
		ShowPoints();
		// TODO Mejorar la implementacion para elegir el mejor punto
		// TODO falta poner el punto de retirada del señuelo
		if (cFirstQualityPoints.size() != 0) {
			// Elegimos entre todas las opciones de primera calidad
			m_cBaitAttackPoint = cFirstQualityPoints.get(0);
			m_cAttackPoint = cFirstQualityPoints.get(1);
		}
		else if (cSecondQualityPoints.size() != 0){
			// Elegimos entre todos los puntos de segunda calidad
			m_cBaitAttackPoint = cSecondQualityPoints.get(0);
			m_cAttackPoint = cSecondQualityPoints.get(1);
		}
		else {
			m_cBaitAttackPoint = null;
			m_cAttackPoint = null;
			System.out.println("NO HAY PUNTOS FACTIBLES!!RETURN FALSE");
		}
		
		
		// TODO Poner este codigo en el señuelo
		/*Vector3D[] path = PathFindingSolver.FindPath(m_Map, x1, z1, x, z);
		System.out.println("El path tiene longitud " + path.length);
		CheckBaitPathToAttack(path);
		// Generar un punto de ataque principal
		
		// Ejecucion de la tarea
		String startPos;
		startPos = " ( " + path[0].x + " , 0.0 , " + path[0].z + " ) ";
		AddTask(CTask.TASK_WALKING_PATH, getAID(), startPos, m_CurrentTask.getPriority() + 1);*/
		
		
	}
	/**
	 * Imprime por consola los puntos seleccionados
	 */
	private static void ShowPoints() {
		System.out.print("Puntos de primera categoria: ");
		for (int i = 0; i < cFirstQualityPoints.size(); i += 2)
			System.out.print("(" + cFirstQualityPoints.get(i).x + "," + cFirstQualityPoints.get(i).z + ")-(" + 
					cFirstQualityPoints.get(i+1).x + "," + cFirstQualityPoints.get(i+1).z + ")  ");
		System.out.println("");
		System.out.print("Puntos de segunda categoria: ");
		for (int i = 0; i < cSecondQualityPoints.size(); i += 2) {
			System.out.print("(" + cSecondQualityPoints.get(i).x + ","+ cSecondQualityPoints.get(i).z + ")-(" +
					cSecondQualityPoints.get(i+1).x + "," + cSecondQualityPoints.get(i+1).z + ")  ");
		}
		System.out.println("");
	}
	/**
	 * 
	 */
	private static void GetSecondQualityPoints(boolean bValidPoints[], Vector3D cAttackPoints[]) {
		if (bValidPoints[0] && bValidPoints[4]) {
			cSecondQualityPoints.add(cAttackPoints[0]);
			cSecondQualityPoints.add(cAttackPoints[4]);
		}
		if (bValidPoints[0] && bValidPoints[6]) {
			cSecondQualityPoints.add(cAttackPoints[0]);
			cSecondQualityPoints.add(cAttackPoints[6]);
		}
		if (bValidPoints[2] && bValidPoints[3]) {
			cSecondQualityPoints.add(cAttackPoints[2]);
			cSecondQualityPoints.add(cAttackPoints[3]);
		}
		if (bValidPoints[2] && bValidPoints[6]) {
			cSecondQualityPoints.add(cAttackPoints[2]);
			cSecondQualityPoints.add(cAttackPoints[6]);
		}
		if (bValidPoints[7] && bValidPoints[1]) {
			cSecondQualityPoints.add(cAttackPoints[7]);
			cSecondQualityPoints.add(cAttackPoints[1]);
		}
		if (bValidPoints[7] && bValidPoints[3]) {
			cSecondQualityPoints.add(cAttackPoints[7]);
			cSecondQualityPoints.add(cAttackPoints[3]);
		}
		if (bValidPoints[5] && bValidPoints[1]) {
			cSecondQualityPoints.add(cAttackPoints[5]);
			cSecondQualityPoints.add(cAttackPoints[1]);
		}
		if (bValidPoints[5] && bValidPoints[4]) {
			cSecondQualityPoints.add(cAttackPoints[5]);
			cSecondQualityPoints.add(cAttackPoints[4]);
		}
	}
	
	/*private static void CheckBaitPathToAttack(Vector3D[] cPath) {
		
		
	}*/
	/**
	 * Indica si el punto seleccionado como parámetro se encuentra dentro del cuadrado
	 * que rodea la bandera
	 * @param dPointX: coordenada X
	 * @param dPointZ: coordenada Z
	 * @return true si se encuentra dentro del cuadrado, false en otro caso
	 */
	public static boolean IsBattlePoint(double dPointX, double dPointZ) {
		if ((dPointX > cGoal.x - BAIT_RADIOUS) && 
			(dPointX < cGoal.x + BAIT_RADIOUS) &&
			(dPointZ > cGoal.z - BAIT_RADIOUS) &&
			(dPointZ < cGoal.z + BAIT_RADIOUS)) {
			return true;
		}
		return false;
	}
	/**
	 * 
	 * @return
	 */
	public static Vector3D getBaitAttackPoint() {
		return m_cBaitAttackPoint;
	}
	/**
	 * 
	 * @return
	 */
	public static Vector3D getAttackPoint() {
		return m_cAttackPoint;
	}
}
