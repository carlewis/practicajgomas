# Introducción #

En cada estrategia agentes del mismo tipo pueden jugar papeles (roles) diferentes dentro del equipo, siendo necesario una estrategia para determinar qué roles hay, y como asignarselos a cada agente.

# Planteamiento #

La negociación de los roles se hará mediante una contienda por ser el lider, es decir, todos los agentes solicitarán el puesto hasta que quede definido. El resto de roles los asigna el lider:

  1. Cada usuario genera un numero aleatorio entre 0 y 10 que será su puja.
  1. Cada usuario envia un mensaje "QUIERO SER EL LIDER" y su PUJA al resto, uno por uno.
  1. Si el agente que lo recibe no conoce un lider o es él el lider compara las pujas.
    * Si su puja es menor envia "TU ERES EL LIDER"
    * Si es mayor envia "YO SOY EL LIDER"
  1. Los dos agentes pasan a conocer al nuevo lider.
  1. Si el agente receptor ya conoce un lider, y no es él mismo, responde "YA HAY LIDER" y la identificacion del lider. El emisor contactará con el lider si no es él mismo.
  1. Vuelve al paso 3.

Finalmente un solo agente será el lider y será el encargado de informar al resto de agentes. Despues de informar al resto de agentes se encarga de repartir los roles del resto de agentes del grupo. De esta forma se mejora el rendimiento en el aspecto del número de mensajes intercambiados.

# Implementacion #

En primer lugar se definen los tipos de mensajes:
```
public enum LeaderMessage { REQUEST, VALUE, ALREADY_EXISTS, I_WIN, YOU_WIN };
```

Despues hay que definir **cuando comienza el protocolo**. Esto no puede suceder inmediatamente despues al lanzamiento del comportamiento de las comunicaciones, ya que es
probable que los agentes no hayan entrado en contacto todos con todos. Solución: lanzamos un comportamiento simple en el que esperamos a que todos los agentes estén comunicados, cuando esto ocurra se comienza la negociacion de lider.

**Problema**

Puesto que el algoritmo se basa en que hay que interactuar con los agentes de uno en uno, es necesario tener una estructura de datos que nos indique con qué agentes ya hemos dialogado, además de una variable de estado que nos indique si estamos a la espera de la respuesta de uno de los agentes. El segundo problema se resuelve con una variable tipo boolean que nos indica si el agente se encuentra esperando una respuesta. Para el primer problema extendemos la estructura de datos de comunicación de los agentes, que pasa a ser una clase con los siguientes campos:
```
// Espera por una respuesta
protected boolean m_bWaitAnswer;
...
public class AgentInfo {
	public AgentType type;
	public AID aid;
	public boolean checked;
	public AgentInfo(AgentType t, AID a) {
		type = t;
		aid = a;
		checked = false;
	}
}
```

**Problema**

Otro problema que surge es que si solo hay un soldado con vida, este se tiene que proponer como lider del grupo. Para esto es necesario saber el numero de agentes de tipo soldado que tiene el equipo así que declaramos una variable que mantiene la cuenta de soldados del equipo (sin incluirle a él mismo):
```
protected int m_iTeamSoldiersCount;
```