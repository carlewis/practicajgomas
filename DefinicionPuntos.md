# Definicion de Puntos #

Para definir los puntos de la estrategia hay que tener en cuenta varios factores. Primero la distancia a la que queremos que se acerquen los agentes antes de comenzar el ataque, despues hay que tener en cuenta desde donde queremos que sea el ataque ya que es probable que no todas las direcciones. Para esto se buscan las direcciones en linea recta desde las que no hay obstáculos.

Primero definimos un cuadrado en base a un radio. Se trazan todas las trayectorias de los centros de los lados y las esquinas del cuadrado hasta el centro del mismo y se verifican cuáles son viables. Son viables las trayectorias que en linea recta no tienen ningún obstáculo.

Se seleccionan dos tipos de puntos: los puntos de Primera Categoría, que son los puntos que están opuestos en el cuadrado; y los puntos de Segunda Categoría, que son los puntos que no están opuestos pero tienen al menos un punto intermedio en el cuadrado.

Después se buscan las trayectorias que, desde la base alcanzan los puntos del cuadrado viables y que no atraviesen el cuadrado. Si alguno de los puntos no es alcanzable sin atravesar el cuadrado tampoco es viable. Si existen varias posibilidades de elección se eligen los que minimicen la longitud de las trayectorias, para minimizar el tiempo de la partida, aunque siempre predominarán los puntos de Primera Categoría frente a los de Segunda.

El punto de retirada del señuelo se selecciona trazando una línea recta entre la bandera y el punto de ataque del señuelo, a una distancia determinada.