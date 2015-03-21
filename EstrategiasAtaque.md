# Introducción #

El equipo base tiene comportamientos completamente reactivos y no organizados. Tras las primeras pruebas de ataque uno se da cuenta que el equipo enemigo completo acude tras una amenaza asi que la primera estrategia que se me ocurre es el uso de un señuelo.

## Estrategia del Señuelo ##

Consiste en enviar un agente para atraer la atención del equipo contrario hacia posiciones alejadas de la bandera, mientras el resto del equipo atacante se mantiene a la espera y finalmente ataca el objetivo.

### Valoración de la Estrategia ###

En primer lugar hay que generar los puntos clave de la estrategia:

  * Punto de Ataque del Señuelo
  * Punto Distante del Señuelo
  * Punto de Ataque Principal

Despues hay que comprobar que existen caminos seguros entre estos puntos:

  * Base - Punto de Ataque del Señuelo
  * Base - Punto Distante del Señuelo
  * Punto de Ataque del Señuelo - Punto Distante del Señuelo
  * Base - Punto de Ataque Principal

Si todos los caminos son seguros se pasa a la ejecución del ataque.

### Ejecución del Ataque ###

Tiene que existir un agente que determine la validez de la estrategia con lo que se crean al menos los roles de **lider**, **señuelo**, y **resto del equipo**.

En primer lugar se envia al señuelo a su Punto de Ataque para que atraiga la atención del equipo contrario. Se envia un segundo soldado, un médico y un operador de campo (FieldOps) al punto Distante del Señuelo. Se envia al resto de atacantes al Punto de Ataque Principal.

El señuelo se acerca al equipo contrario y dispara hasta que los agentes contrarios le persiguen hasta el punto Distante, donde el médico tiene preparados uno o varios MedicPacks y el operador de campo tiene municiones, el segundo agente cubre la retirada.

El resto del equipo comienza una ofensiva hacia la bandera que si funciona estará menos defendida de lo habitual.

![http://practicajgomas.googlecode.com/files/se%C3%B1uelo.png](http://practicajgomas.googlecode.com/files/se%C3%B1uelo.png)

### Implementacion ###

Lo primero que hay que hacer es definir los [roles de los agentes](NegociacionRoles.md). Una vez definido el agente lider, este se encarga de transmitir el resto de roles. Cada agente tiene que mantener una variable interna con su rol ya que este nos permitirá darle diferentes comportamientos a cada rol.

Además es necesario poder transmitir identidades a través de mensajes, ya que es necesario que el señuelo conozca quienes son sus apoyos, para solicitar los medicamentos y municiones.

La variable de cada agente que define su rol es la siguiente:

```
public enum BaitRole { BAIT, BAIT_MEDIC, BAIT_FIELDOP, BAIT_SOLDIER, TEAM_SOLDIER};
...
protected MyComponents.BaitRole m_nAgentRole;
```

Por otro lado, para poder enviar el AID de un agente en un mensaje, se puede hacer uso de la comparacion de las cadenas que describen a los agentes. La función que resulta es la siguiente:


```
private AID ContentToAgent(String s) {
	StringTokenizer tokens = new StringTokenizer(s);
	tokens.nextToken(); // Quita el (
	String sContentAID = tokens.nextToken();
	sContentAID = sContentAID + tokens.nextToken("))") + "))";
	for (AgentInfo ai: m_TeamAgents)
		if (ai.aid.toString().equals(sContentAID))
			return ai.aid;
	return getAID();
}
```

Una vez determinados los roles de cada uno de los agentes el líder decide la estrategia a seguir. En primer lugar, para la estrategia del señuelo es necesario definir los puntos de ataque mecionados antes. Se profundiza más en la siguiente sección: [Definición de Puntos](DefinicionPuntos.md)