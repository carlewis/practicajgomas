package student;

import student.PathFinding.PathFindingSolver;
import es.upv.dsic.gti_ia.jgomas.CTerrainMap;
import es.upv.dsic.gti_ia.jgomas.Vector3D;
import java.util.Vector;


public class BaitLib {
	private PathFindingSolver m_cSolver;
	/** Posicion de la bandera */
	private Vector3D cGoal;
	/** Distancia entre la bandera y los puntos de ataque */
	private double BAIT_RADIOUS = 48.0;
	/** Puntos de ataque que estan opuestos */
	private Vector<Vector3D> cFirstQualityPoints = new Vector<Vector3D>();
	/** Puntos de ataque que estan separados */
	private Vector<Vector3D> cSecondQualityPoints = new Vector<Vector3D>();
	/** Punto de ataque del señuelo */
	private Vector3D m_cBaitAttackPoint;
	/** Punto de ataque del resto del equipo */
	private Vector3D m_cAttackPoint;
	public void setPathFindingSolver(PathFindingSolver cSolver) {
		m_cSolver = cSolver;
		m_cSolver.setBaitLib(this);
	}
	public void setGoal(Vector3D cGoal) {
		this.cGoal = cGoal;
	}
	/**
	 * Genera los puntos claves dentro de la estrategia de ataque del señuelo
	 * Utilizado por el Lider del grupo
	 * @param cGoal: Posicion de la bandera
	 * @param cBase: Posicion de la base del atacante
	 */
	public void GenerateBaitPoints(CTerrainMap cMap, Vector3D cGoal, Vector3D cBase) {
		this.cGoal = cGoal;

		// posicion del objetivo		
		//System.out.println("la bandera esta en " + cGoal.x + ", " + cGoal.z);
		// posicion de la base?
		//System.out.println("la base en " + cBase.x + ", " + cBase.z);
		// punto de ataque del señuelo: se encuentra en la recta entre la base y 
		// la bandera

		// Generamos los puntos entre los que podemos elegir los de ataque
		Vector3D cAttackPoints[] = new Vector3D[8];
		boolean m_bPointNotFound = true;
		do {
			double dGoalXIncs[] = {-BAIT_RADIOUS, -BAIT_RADIOUS, -BAIT_RADIOUS, 0, 
					0, BAIT_RADIOUS, BAIT_RADIOUS,  BAIT_RADIOUS };		
			double dGoalZIncs[] = {-BAIT_RADIOUS, 0, BAIT_RADIOUS, -BAIT_RADIOUS, 
					BAIT_RADIOUS, -BAIT_RADIOUS, 0, BAIT_RADIOUS};
			for (int i = 0; i < 8; i++) {
				cAttackPoints[i] = new Vector3D();
				cAttackPoints[i].x = cGoal.x + dGoalXIncs[i];
				cAttackPoints[i].z = cGoal.z + dGoalZIncs[i];
			}

			m_cSolver.setMap(cMap);
			// Comprobamos si se puede alcanzar el objetivo en linea recta
			boolean bValidPoints[] = new boolean[8];
			for (int i = 0; i < 8; i++) 
				bValidPoints[i] = m_cSolver.CheckDirectPath(cAttackPoints[i], cGoal);
			// Seleccionamos los puntos de primera y segunda categoría
			for (int i = 0; i < 4; i++) {
				// Los dos puntos opuestos son validos
				if (bValidPoints[i] && bValidPoints[7-i]) {
					cFirstQualityPoints.add(cAttackPoints[i]);
					cFirstQualityPoints.add(cAttackPoints[7 - i]);
				}
			}
			GetSecondQualityPoints(bValidPoints, cAttackPoints);
			//ShowPoints();
			//System.out.println("Todos los puntos:");
			//for (int i = 0; i < 8; i++)
			//	System.out.print("(" + cAttackPoints[i].x + "," + cAttackPoints[i].z + ")  ");
			//System.out.println("");
			// Buscamos las rutas entre la base y los puntos seleccionados en los 
			// puntos de primera categoria
			for (int i = 0; i < cFirstQualityPoints.size(); i += 2) {
				if ((m_cSolver.FindBaitPath(cBase.x, cBase.z,
						cFirstQualityPoints.get(i).x, cFirstQualityPoints.get(i).z) == null) ||
						(m_cSolver.FindBaitPath(cBase.x, cBase.z,
								cFirstQualityPoints.get(i+1).x, cFirstQualityPoints.get(i+1).z) == null)) {
					// Se eliminan los dos puntos
					cFirstQualityPoints.remove(i);
					cFirstQualityPoints.remove(i);
					//System.out.println("eliminando " + i);
					i -= 2;
				}
			}
			// Buscamos las rutas entre la base y los puntos seleccionados en los 
			// puntos de segunda categoría
			for (int i = 0; i < cSecondQualityPoints.size(); i += 2) {
				if ((m_cSolver.FindBaitPath(cBase.x, cBase.z,
						cSecondQualityPoints.get(i).x, cSecondQualityPoints.get(i).z) == null) ||
						(m_cSolver.FindBaitPath(cBase.x, cBase.z,
								cSecondQualityPoints.get(i+1).x, cSecondQualityPoints.get(i+1).z) == null)) {
					// Se eliminan los dos puntos
					cSecondQualityPoints.remove(i);
					cSecondQualityPoints.remove(i);
					//System.out.println("eliminando " + i);
					i -= 2;
				}
			}
			//ShowPoints();
			// TODO Mejorar la implementacion para elegir el mejor punto
			// TODO falta poner el punto de retirada del señuelo
			if (cFirstQualityPoints.size() != 0) {
				// Elegimos entre todas las opciones de primera calidad
				m_cBaitAttackPoint = cFirstQualityPoints.get(0);
				m_cAttackPoint = cFirstQualityPoints.get(1);
				m_bPointNotFound = false;
			}
			else if (cSecondQualityPoints.size() != 0){
				// Elegimos entre todos los puntos de segunda calidad
				m_cBaitAttackPoint = cSecondQualityPoints.get(0);
				m_cAttackPoint = cSecondQualityPoints.get(1);
				m_bPointNotFound = false;
			}
			else {
				m_cBaitAttackPoint = null;
				m_cAttackPoint = null;
				//System.out.println("NO HAY PUNTOS FACTIBLES!!RETURN FALSE");
				BAIT_RADIOUS -= 8;
			}
		} while (m_bPointNotFound);
	}
	/**
	 * Imprime por consola los puntos seleccionados
	 */
	protected void ShowPoints() {
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
	private void GetSecondQualityPoints(boolean bValidPoints[], Vector3D cAttackPoints[]) {
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
	public boolean IsBattlePoint(double dPointX, double dPointZ) {
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
	public Vector3D getBaitAttackPoint() {
		return m_cBaitAttackPoint;
	}
	/**
	 * 
	 * @return
	 */
	public Vector3D getAttackPoint() {
		return m_cAttackPoint;
	}
}
