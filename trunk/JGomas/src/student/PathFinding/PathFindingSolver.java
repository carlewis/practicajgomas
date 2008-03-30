package student.PathFinding;

import es.upv.dsic.gti_ia.jgomas.CTerrainMap;
import es.upv.dsic.gti_ia.jgomas.Vector3D;
import es.upv.dsic.gti_ia.jgomas.CMobile;

public class PathFindingSolver {
	
	
	/**
	 * Implementa la busqueda de una ruta hasta la bandera mediante un algoritmo A*
	 */
	public static Vector3D[] FindPathToTarget(CTerrainMap map, CMobile movement) {
		Node.setMap(map);
		Node.setTarget(movement.getDestination().x / 8, movement.getDestination().z / 8);
			
		System.out.println("El nodo de partida es el actual: " + Math.floor(movement.getPosition().x / 8) + 
				" " + Math.floor(movement.getPosition().z / 8));
		System.out.println("El objetivo esta en: " + (movement.getDestination().x / 8) +
				" " + (movement.getDestination().z / 8));
		
		// Generamos el nodo de partida
		Node start = new Node();
		start.setPadre(null);
		start.setPosX((int)(Math.floor(movement.getPosition().x / 8)));
		start.setPosZ((int)(Math.floor(movement.getPosition().z / 8)));
		start.calcCost();
		// Generamos las lista abierta y cerrada
		AStarList OpenList = new AStarList(start);
		AStarList ClosedList = new AStarList();
		boolean objReached = false;
		//int counter = 0;
		Node act = null;
		// Mientras la lista abierta no este vacio y no se encuentre el objetivo
		while (!OpenList.empty() && !objReached) {
			// Extraemos el nodo
			act = OpenList.getFirst();
			
			// Si el nodo es el objetivo hemos terminado
			if (act.isObjective()) {
				objReached = true;
				break;
			}
			
			// Lo insertamos en la lista cerrada, eliminando la de mayor coste 
			// si ya existe una similar
			ClosedList.insert(act);
			
			// Calculamos sus trayectorias descendientes
			Node trajectories[] = act.getChildren();
			
			// Insertamos la trayectorias descendientes en la lista abierta de
			// forma ordenada
			for (Node n : trajectories)
				if (n != null)
					OpenList.insertOrder(n);
			
			// Eliminamos de la lista abierta las trayectorias comunes a las 
			// que ya existen en la cerrada
			OpenList.checkList(ClosedList);

		}
		
		if (!objReached) 
			return null;
		// Recorremos la lista de nodos en orden inverso, generando el recorrido
		System.out.println("ENCONTRADO CAMINO HASTA EL OBJETIVO!");
		Node it = act;
		int count = 0;
		while (it != null) {
			count++;
			it = it.getPadre();
		}
		it = act;
		Vector3D[] Path = new Vector3D[count];
		for (int i = 0; i < count; i++) {
			Path[count - i - 1] = new Vector3D(8 * it.getPosX(), 0.0, 8 * it.getPosZ());
			it = it.getPadre();
		}
		return Path;
	}
	/**
	 * Implementa la busqueda de una ruta desde una localización hasta un destino
	 */
	public static Vector3D[] FindPath(CTerrainMap map, 
			double startX, double startZ, double targetX, double targetZ) {
		Node.setMap(map);
		Node.setTarget(targetX / 8, targetZ / 8);
		
		// Generamos el nodo de partida
		Node start = new Node();
		start.setPadre(null);
		start.setPosX((int)(Math.floor(startX / 8)));
		start.setPosZ((int)(Math.floor(startZ / 8)));
		start.calcCost();
		// Generamos las lista abierta y cerrada
		AStarList OpenList = new AStarList(start);
		AStarList ClosedList = new AStarList();
		boolean objReached = false;
		//int counter = 0;
		Node act = null;
		// Mientras la lista abierta no este vacio y no se encuentre el objetivo
		while (!OpenList.empty() && !objReached) {
			// Extraemos el nodo
			act = OpenList.getFirst();
			
			// Si el nodo es el objetivo hemos terminado
			if (act.isObjective()) {
				objReached = true;
				break;
			}
			
			// Lo insertamos en la lista cerrada, eliminando la de mayor coste 
			// si ya existe una similar
			ClosedList.insert(act);
			
			// Calculamos sus trayectorias descendientes
			Node trajectories[] = act.getChildren();
			
			// Insertamos la trayectorias descendientes en la lista abierta de
			// forma ordenada
			for (Node n : trajectories)
				if (n != null)
					OpenList.insertOrder(n);
			
			// Eliminamos de la lista abierta las trayectorias comunes a las 
			// que ya existen en la cerrada
			OpenList.checkList(ClosedList);

		}
		
		if (!objReached) 
			return null;
		// Recorremos la lista de nodos en orden inverso, generando el recorrido
		Node it = act;
		int count = 0;
		while (it != null) {
			count++;
			it = it.getPadre();
		}
		it = act;
		Vector3D[] Path = new Vector3D[count];
		for (int i = 0; i < count; i++) {
			Path[count - i - 1] = new Vector3D(8 * it.getPosX(), 0.0, 8 * it.getPosZ());
			it = it.getPadre();
		}
		return Path;
	}

}
