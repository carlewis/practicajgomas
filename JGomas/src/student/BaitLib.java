package student;

import student.PathFinding.PathFindingSolver;
import es.upv.dsic.gti_ia.jgomas.Vector3D;
import java.util.Vector;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

public class BaitLib {
	// Mapa
	// Posicion de la bandera
	private static Vector3D cGoal;
	// Distancia entre la bandera y los puntos de ataque
	private static final double BAIT_RADIOUS = 40.0;
	// Puntos de ataque que estan opuestos
	private static Vector<Vector3D> cFirstQualityPoints = new Vector<Vector3D>();
	// Puntos de ataque que estan separados
	private static Vector<Vector3D> cSecondQualityPoints = new Vector<Vector3D>();
	/**
	 * Genera los puntos claves dentro de la estrategia de ataque del señuelo
	 * Utilizado por el Lider del grupo
	 * @param cGoal: Posicion de la bandera, cBase: Posicion de la base del atacante
	 */
	public static void GenerateBaitPoints(Vector3D cGoal, Vector3D cBase) {
		BaitLib.cGoal = cGoal;
		// TODO Decidir los puntos de control de la estrategia
		System.out.println("puntos");
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
			if (bValidPoints[i] && bValidPoints[7-i])
				cFirstQualityPoints.add(i, cAttackPoints[i]);
		}
		GetSecondQualityPoints(bValidPoints, cAttackPoints);
		// Buscamos las rutas entre la base y los puntos seleccionados
		// TODO Comprobar el codigo
		for (int i = 0; i < 8; i++) {
			if (bValidPoints[i]) {
				if ((cFirstQualityPoints.get(i) != null) && 
					// Ruta entre la base y el punto de ataque
					(PathFindingSolver.FindBaitPath(cBase.x, cBase.z, cFirstQualityPoints.get(i).x, 
						cFirstQualityPoints.get(i).z) == null)) {
					cFirstQualityPoints.remove(i);
				}
				// El vector cSecondQualityPoints tiene parejas de puntos, hay que comprobarlos uno a 
				// uno y eliminarlos de dos en dos
				/*if ((cSecondQualityPoints.get(i) != null) &&
					(PathFindingSolver.FindBaitPath(cBase.x, cBase.z, cSecondQualityPoints.get(i).x,
							cSecondQualityPoints.get(i).z) == null)) {
					cSecondQualityPoints.remove(i);
				}*/ 
			}
		}
		for (int i = 0; i < 8; i++) {
			if (!bValidPoints[i] && (cFirstQualityPoints.get(i) != null)) {
				cFirstQualityPoints.remove(i);
			}
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
	
	private static void CheckBaitPathToAttack(Vector3D[] cPath) {
		
		
	}
	
	public static boolean IsBattlePoint(double dPointX, double dPointZ) {
		
		return true;
	}
}
