package student.PathFinding;

import java.util.ArrayList;
/**
 * Implementacion de las listas abierta y cerrada utilizadas en el algoritmo A*
 * para la resolución del problema de búsqueda. La <b>optimalidad</b> y 
 * <b>admisibilidad</b> de la estrategia vienen garantizados por la función de 
 * evaluación heurística implementada en la clase Node, y que tiene que ser 
 * subestimada. 
 */
public class AStarList {
	
	private ArrayList<Node> list;
	
	public AStarList() {
		list = new ArrayList<Node>();
	}
	
	public AStarList(Node n) {
		list = new ArrayList<Node>();
		list.add(n);
	}
	
	public boolean empty() {
		return list.isEmpty();
	}
	
	public int size() {
		return list.size();
	}
	
	public Node get(int i) {
		return list.get(i);
	}
	
	public Node getFirst() {
		Node aux = list.get(0);
		list.remove(0);
		return aux;
	}
	/**
	 * Inserta el nodo en la lista, si el mismo nodo ya existe elimina el que 
	 * tenga mayor coste
	 * @param n
	 */
	public void insert(Node n) {
		boolean exists = false;
		// Optimizable
		for (int i = 0; i < list.size(); i++) {
			if ((list.get(i).getPosX() == n.getPosX()) &&
				(list.get(i).getPosZ() == n.getPosZ())) {
				if (list.get(i).getCost() > n.getCost()) 
					list.remove(i);
				else
					exists = true;
			}
		}
		if (!exists)
			list.add(n);
	}
	
	public void insertOrder(Node n) {
		// Busca la posicion que le corresponde
		
		if (list.size() == 0)
			list.add(n);
		else {
			// Optimizable
			boolean inserted = false;
			for (int i = 0; i < list.size(); i++)  
				if (n.getCost() < list.get(i).getCost()) {
					list.add(i, n);
					inserted = true;
					break;
				}
			if (!inserted)
				list.add(list.size(), n);
		}
		
	}
	/**
	 * Realiza el chequeo de nodos duplicados entre las listas abierta y cerrada
	 */
	// 1. Elimina de la lista abierta las trayectorias comunes excepto la de 
	//    menor coste
	// 2. Elimina las trayectorias de la lista abierta con mayor coste que las
	//    de la lista cerrada
	// Las trayectorias eliminadas de la lista abierta se insertan en la lista cerrada
	public void checkList(AStarList c) {
		// 1
		for (int i = 0; i < list.size(); i++) {
			for (int j = i + 1; j < list.size(); j++) {
				if ((list.get(i).getPosX() == list.get(j).getPosX()) &&
					(list.get(i).getPosZ() == list.get(j).getPosZ())) {
					if (list.get(i).getCost() < list.get(j).getCost()) {
						c.insert(list.get(i));
						list.remove(j);
						j--;
					}
					else {
						c.insert(list.get(j));
						list.remove(i);
						i--;
						break;
					}
				}
			}
		}
		// 2
		for (int i = 0; i < c.size(); i++) {
			Node act = c.get(i);
			for (int j = 0; j < list.size(); j++) {
				if ((act.getPosX() == list.get(j).getPosX()) &&
					(act.getPosZ() == list.get(j).getPosZ())) {
					if (act.getCost() < list.get(j).getCost()) {
						// Eliminamos el nodo porque ya hay uno en la lista cerrada 
						// con menos coste
						list.remove(j);
						// En la siguiente iteracion tenemos que comprobar el mismo indice
						j--;
					}
					else
						c.insert(list.get(j));
				}
			}
		}
	}
	
	public void show() {
		System.out.print("Size = " + list.size() + "->");
		for (int i = 0; i < list.size(); i++) {
			System.out.print(((Node)(list.get(i))).toString() + "  ");
		}
		System.out.println("<-");
	}
}
