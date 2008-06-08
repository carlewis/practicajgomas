package student.PathFinding;

import student.BaitLib;
import es.upv.dsic.gti_ia.jgomas.CTerrainMap;

public class Node {
	private static CTerrainMap m_Map;
	private static final float LIN_INC_COST = 1;
	//private static final int DIAG_INC_COST = 14;
	private static double m_fTargetX;
	private static double m_fTargetZ;
	private int m_iPosX;
	private int m_iPosZ;
	private Node m_Hijos[];
	private Node m_Padre;
	private float m_fFCost;
	private float m_fHCost;
	private float m_fTotalCost;
	
	public static void setMap(CTerrainMap m) {
		m_Map = m;
	}
	public static void setTarget(double px, double pz) {
		m_fTargetX = px;
		m_fTargetZ = pz;
	}
	
	public String toString() {
		return "" + m_iPosX + "," + m_iPosZ + " (" + m_fTotalCost + ")";
	}
	
	public int getPosX() {
		return m_iPosX;
	}
	public int getPosZ() {
		return m_iPosZ;
	}
	public float getCost() {
		return m_fTotalCost;
	}
	public float getFCost() {
		return m_fFCost;
	}
	public float getHCost() {
		return m_fHCost;
	}
	public Node getPadre() {
		return m_Padre;
	}
	public void setPosX(int px) {
		m_iPosX = px;
	}
	public void setPosZ(int pz) {
		m_iPosZ = pz;
	}
	public void setCost(float cost) {
		m_fTotalCost = cost;
	}
	public void setFCost(float cost) {
		m_fFCost = cost;
	}
	public void setHCost(float cost) {
		m_fHCost = cost;
	}
	public void setPadre(Node p) {
		m_Padre = p;
	}
	public float calcHCost(int posX, int posZ) {
		int iObjX = (int) Math.round(m_fTargetX);
		int iObjZ = (int) Math.round(m_fTargetZ);
		//return (float)Math.sqrt((posX - iObjX) * (posX - iObjX) + (posZ - iObjZ) * (posZ - iObjZ));
		// Distancia manhatan
		return Math.abs(posX - iObjX) + Math.abs(posZ - iObjZ);
/*		if (Math.abs(posX - iObjX) > Math.abs(posZ - iObjZ)) 
			return Math.abs(posX - iObjX);
		else
			return Math.abs(posZ - iObjZ);*/
	}
	
	// Calcula el coste del nodo de partida según la evaluación de la función heurística
	public void calcCost() {
		m_fFCost = 0;
		m_fHCost = calcHCost(m_iPosX, m_iPosZ);
		m_fTotalCost = m_fHCost;
		System.out.println("El coste del nodo inicial es " + m_fTotalCost);
	}
	
	public Node[] getChildren() {
		int incPosX[] = {+1, 0, -1, 0};
		int incPosZ[] = {0, +1, 0, -1};
		m_Hijos = new Node[4];
		for (int i = 0; i < 4; i++) {
			if (m_Map.CanWalk(getPosX() + incPosX[i], getPosZ() + incPosZ[i])) {
				m_Hijos[i] = new Node();
				m_Hijos[i].setPosX(getPosX() + incPosX[i]);
				m_Hijos[i].setPosZ(getPosZ() + incPosZ[i]);
				m_Hijos[i].setFCost(getFCost() + LIN_INC_COST);
				m_Hijos[i].setHCost(calcHCost(m_Hijos[i].getPosX(), m_Hijos[i].getPosZ()));
				m_Hijos[i].setCost(m_Hijos[i].getFCost() + m_Hijos[i].getHCost());
				m_Hijos[i].setPadre(this);
			}
		}
		return m_Hijos;
	}
	
	public Node[] getBaitChildren() {
		int incPosX[] = {+1, 0, -1, 0};
		int incPosZ[] = {0, +1, 0, -1};
		m_Hijos = new Node[4];
		for (int i = 0; i < 4; i++) {
			if (m_Map.CanWalk(getPosX() + incPosX[i], getPosZ() + incPosZ[i]) &&
				!BaitLib.IsBattlePoint(
						(double) ((getPosX() + incPosX[i]) * 8), 
						(double) ((getPosZ() + incPosZ[i]) * 8))) {
				m_Hijos[i] = new Node();
				m_Hijos[i].setPosX(getPosX() + incPosX[i]);
				m_Hijos[i].setPosZ(getPosZ() + incPosZ[i]);
				m_Hijos[i].setFCost(getFCost() + LIN_INC_COST);
				m_Hijos[i].setHCost(calcHCost(m_Hijos[i].getPosX(), m_Hijos[i].getPosZ()));
				m_Hijos[i].setCost(m_Hijos[i].getFCost() + m_Hijos[i].getHCost());
				m_Hijos[i].setPadre(this);
			}
		}
		return m_Hijos;
	}
	
	public boolean isObjective() {
		int iObjX = (int) Math.round(m_fTargetX);
		int iObjZ = (int) Math.round(m_fTargetZ);
		return ((iObjX == m_iPosX) && (iObjZ == m_iPosZ)); 
	}
}
