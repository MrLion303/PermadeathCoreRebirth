# PermadeathCoreRebirth

Plugin de dificultad progresiva para Permadeath / NegativeStudios.

Esta documentación recoge los cambios de dificultad implementados actualmente en el código: mejoras de mobs, transformaciones, daño, objetos, drops, restricciones, comportamiento hostil y escaladas de estadísticas.

# Cambios por día

## Día 1
- Inicio de la progresión.
- No se aplican todavía los escalones especiales posteriores.

## Día 5
### Mobs
- Warden: Velocidad III y Resistencia II.
- Piglin Brute: hacha de diamante con Filo de Fuego II; no se puede obtener como drop.
- Piglin: Velocidad II, Fuerza II y Resistencia al Fuego I.
### Incursiones
- Con Bad Omen suficiente se añade un Illusioner adicional.

## Día 10
- Rana: Fuerza L permanente.
- Warden: Invisibilidad I permanente.
- Se habilita el escalado de capacidad de mobs cuando está activa la opción Doble-Mob-Cap.

## Día 15
### Mobs
- Allay: espada de Netherite con Filo V; no se puede obtener.
- Camel: Velocidad III y Resistencia II.
- Bat: Fuerza IV, Resistencia III y 20 de daño de ataque base.
- Illusioner: Velocidad I, Fuerza I y Resistencia I.
- Spider: al golpear aplica Lentitud extrema durante 3 segundos y coloca una telaraña bajo el jugador cuando hay espacio.
- El daño de ataque de los Bat se fuerza a 20.
### Receta
- Se habilita una receta de 8 botellas de experiencia usando 1 diamante y 4 lapislázuli.

## Día 20
### Hostilidad
- Las criaturas que normalmente no son hostiles reciben comportamiento hostil.
- La conversión también se aplica a entidades que ya existían y a entidades al cargar chunks.
- Las entidades convertidas reciben atributo de ataque si no lo tenían.
### Phantoms
- Vida máxima duplicada.
- Tamaño inicial 9.
### Drops
- Se eliminan drops normales de Iron Golem, Piglin/Zombified Piglin, Ghast, Guardian, Enderman, Witch, Wither Skeleton, Evoker, Phantom, Slime, Drowned y Blaze.
### Skeletons
- Comienza el sistema de clases especiales de Skeleton.

## Día 25
### Jugador
- Las herramientas vanilla de madera, piedra, hierro, oro y diamante provocan daño.
- La armadura de Netherite vanilla provoca daño.
- Las piezas especiales de Netherite del plugin quedan excluidas.
### Ravager
- Antes del día 40: Velocidad II y Fuerza I.
### Skeleton/Zombie
- Se eliminan piezas de armadura de cuero vanilla no especiales.
### Slime
- GIGA Slime.
- Tamaño 15.
- Vida x2.
### Magma Cube
- GIGA MagmaCube.
- Tamaño 16.
- Máximo 10 GIGA MagmaCubes.
### Ghast
- Ghast Demoníaco con aproximadamente 40–60 de vida antes del día 40.
### Netherite
- Entre los días 25 y 29 existe el sistema de drops especiales de armadura de Netherite según las probabilidades configuradas.

## Día 30
### Mobs
- Silverfish y Endermite reciben efectos de dificultad extremos.
- Enderman: Fuerza II.
- Iron Golem: Velocidad IV.
- Pillager: Invisibilidad permanente y ballesta con Quick Charge IV; el arma no se obtiene.
### Skeletons especiales
- Aumenta su vida.
- Empiezan a utilizar Protección IV y armas con Power, Punch, Sharpness y Fire Aspect mucho más altos.
- Las clases de Skeleton pasan a usar estadísticas superiores.
### Tótem
- Usar un tótem aplica Mining Fatigue durante 2 minutos.
- Se activan transformaciones adicionales asociadas al uso del tótem.

## Día 32
- Se bloquea la Invisibilidad obtenida mediante pociones:
  - Poción ingerida.
  - Splash Potion.
  - Area Effect Cloud.

## Día 35
### Jugador
- Stonecutter causa daño al permanecer sobre él.
- Durante la noche existe una probabilidad aleatoria de recibir Darkness durante 10 segundos.
### Ender Dragon
- El Permadeath Demon aumenta su vida máxima un 50 % respecto a la vida configurada.
- En The End, el daño del Ender Dragon aumenta 25 %.
### Sniffer
- Al morir puede soltar 2–4 diamantes.
- 4–10 esmeraldas.
- 1 Golden Apple.
- 25 % de Netherite Scrap.
- 5 % de Enchanted Golden Apple.

## Día 40
### PvP
- El daño entre jugadores queda habilitado.
### Phantom
- Puede llevar un Skeleton como pasajero.
### Piglin/Zombified Piglin
- Se habilitan variantes especiales.
- Probabilidad base de transformación: 5 %.
- Variantes: Ultra Ravager, Piglin con Bee, Piglin con Ghast, Piglin con Magma Cube y Piglin con Pig.
### Enderman
- En Nether puede transformarse en Ender Creeper.
- En Overworld puede generar un módulo de muerte.
### Iron Golem
- Fuerza I.
### Creeper
- Velocidad II y Resistencia II.
### Guardian
- Velocidad III y Resistencia II.
### Spider
- Se sustituye por Cave Spider.
### Zombie
- Puede sustituirse por Vindicator con Fuerza I y vida máxima duplicada.
### Wolf
- Se sustituye por Cat.
### Cats
- Antes del día 50 se convierten en Gato Supernova y pueden explotar.
### Animales
- Cow, Sheep, Pig y Mushroom Cow se sustituyen por Ravagers en el mundo principal.
### Chicken
- Se sustituye por Ravager antes del día 50.
### Witch
- Velocidad II, vida máxima x2 y nombre Bruja Imposible.

## Día 45
### Jugador
- Se entrega Saturación infinita una sola vez por jugador.
### Lava
- El daño de lava al jugador pasa a 2048, funcionando prácticamente como muerte instantánea.

## Día 50
### Phantom
- Tamaño 18.
- Aumenta fuertemente la probabilidad de generar Ghasts adicionales.
- Se añaden efectos adicionales.
### Piglin/Zombified Piglin
- La probabilidad de variantes especiales sube de 5 % a 20 %.
### Creeper
- Entre 50 y 59 puede ser Ender Creeper o Quantum Creeper.
- En el día 60 pasa a Ender Quantum Creeper.
### Pillager
- Puede sustituirse por Evoker.
### Vindicator
- Hacha de diamante con Sharpness V.
### Vex
- Resistencia III.
### Blaze
- Vida 200.
### Cod
- Bacalao de la Muerte.
- Espada de madera con Sharpness 50 y Knockback 100.
### Drowned
- Aparece con Trident.
### Salmon
- Se sustituye por Pufferfish.
### Pufferfish
- Invulnerable.
### Wither Skeleton
- Puede convertirse en Wither Skeleton Emperador.
- Vida 80.
- Armadura dorada.
- Arco Power 100 y Punch V.
### Zombie
- Puede convertirse en Giant mediante una probabilidad especial.
### Ravager
- En el mundo principal pasa a Ultra Ravager.
- Ultra Ravager: 500 de vida, Velocidad II y Fuerza II.
### Animales
- Cow, Sheep, Pig y Mushroom Cow se convierten directamente en Ultra Ravager.
### Chicken
- Se sustituye por Silverfish.
### Ahogamiento
- El daño por ahogamiento del jugador pasa a 5 entre los días 50–59.
### Llama
- Su proyectil aplica Veneno III durante 30 segundos, Náusea durante 10 segundos y aumenta el retroceso.
### Drops
- Se habilitan recompensas especiales de Giants y del Wither Skeleton Emperador.
- Las variantes especiales de Ultra Ravager tienen drops especiales.

## Día 55
### Interacciones letales
Interactuar con estos bloques mata al jugador:
- Puertas.
- Trampillas.
- Botones.
- Vallas.
- Puertas de valla.
### Cofres
- Chest.
- Trapped Chest.
- Ender Chest.
- Cualquier Shulker Box.
- Todos provocan daño equivalente a Instant Damage II.

## Día 57
### Suelo
- Grass Block causa daño.
- Todos los Slabs causan daño.

## Día 60 — Dificultad máxima
El sistema alcanza su máximo nivel.

### Herramientas
- Pico de Netherite vanilla: Instant Damage II.
- Hacha de Netherite vanilla: Instant Damage II.
- Pala de Netherite vanilla: Instant Damage II.
- Azada de Netherite vanilla: Instant Damage II.
- Las herramientas especiales del plugin quedan excluidas.

### Agua
- Tocar agua aplica Wither II continuamente.

### Elytra
Cualquier Elytra equipada aplica:
- Darkness I.
- Wither II.
Esto también afecta a Elytras especiales.

### Warden
- Después del intervalo correspondiente, cada jugador tiene una probabilidad de 1 % de generar un Warden en su posición.
- Se conservan las mejoras anteriores del Warden.

### Phantom
- Mantiene tamaño 18 y sus mejoras.
- La probabilidad de generar grupos de Ghasts aumenta considerablemente.

### Enderman
- Fuerza X.

### Iron Golem
- Fuerza IV.
- Resistencia IV.

### Creeper
- Ender Quantum Creeper.
- Mecha reducida a la mitad.

### Guardian
- Se bloquea el spawn normal de Guardian.
- Se sustituye por Elder Guardian.
- Máximo local de 5 Elder Guardians en la transformación.

### Vex
- Fuerza III.
- Resistencia III.
- 7 de daño base.

### Villager
- Los aldeanos dejan de generarse normalmente.
- Se sustituyen aleatoriamente por Vindicator o Vex.
- Los aldeanos existentes también pueden transformarse mediante las transformaciones globales.

### Vindicator
- Puede sustituirse por Evoker.
- Evoker: vida máxima x2 y Resistencia III.

### Blaze
- 200 de vida.

### Wither Skeleton Emperador
- Aumenta su frecuencia de aparición.
- Mantiene 80 de vida, armadura dorada y arco Power 100 / Punch V.

### Skeletons especiales
- Protección V donde corresponde.
- Aumentan Power, Punch, Sharpness, Fire Aspect y demás encantamientos.
- Aumentan sus valores de vida y velocidad.
- Se reducen drops de armadura en las variantes superiores.
### Ultra Esqueleto Definitivo
- Probabilidad extremadamente baja.
- 400 de vida.
- Velocidad II.
- Arco Power 32765.
- No suelta el arma.

### Slime
- GIGA Slime tamaño 16.
- Vida x4 respecto a la referencia anterior.

### Magma Cube
- GIGA MagmaCube tamaño 17.
- Vida aumentada nuevamente.
- Máximo 10 GIGA MagmaCubes.

### Drowned
- El daño infligido al jugador se multiplica x3.

### Ahogamiento
- 10 de daño por ahogamiento.

### Bloques interactuables
- Cualquier bloque interactuable que no sea una de las categorías especiales anteriores causa Instant Damage I al interactuar.

### Tótem
Después de usar un tótem:
- Mining Fatigue durante 2 minutos.
- Teletransporte al mob más cercano.

### Nether
- Se mantienen las restricciones y transformaciones de las etapas anteriores.
- Se bloquean determinados spawns normales para forzar las variantes de mayor dificultad.

# Cambios progresivos de estadísticas

Además de los eventos puntuales, los mobs especiales escalan sus estadísticas en varios escalones:

| Etapa | Escalado |
|---|---|
| Día 20 | Comportamiento hostil y primeras clases especiales |
| Día 25 | GIGA Slime/MagmaCube y herramientas peligrosas |
| Día 30 | Skeletons y mobs especiales reciben estadísticas mayores |
| Día 40 | Transformaciones masivas y variantes hostiles |
| Día 50 | Ultra Ravagers, Giants, Creepers especiales y mobs reforzados |
| Día 60 | Valores máximos, reemplazos de mobs y daño extremo |

# Reglas de acumulación

Los cambios se acumulan conforme avanza el día.

- Un cambio desbloqueado en el día 20 sigue activo en el día 60 salvo que una regla posterior lo sustituya explícitamente.
- Las transformaciones globales también afectan a mobs que ya existían cuando se alcanzó el nuevo día.
- Los mobs reciben las reglas correspondientes al cargar chunks posteriormente.
- Las reglas de estadísticas que tienen varios escalones utilizan el valor correspondiente al día actual.

# Progresión resumida

| Día | Cambio principal |
|---:|---|
| 1 | Inicio |
| 5 | Warden, Piglin y Piglin Brute mejorados |
| 10 | Rana/Warden potenciados + capacidad de mobs |
| 15 | Allay, Camel, Bat, Illusioner y receta XP |
| 20 | Entidades neutrales hostiles + Phantom mejorado |
| 25 | Herramientas peligrosas + GIGA Slime/MagmaCube |
| 30 | Gran escalada de mobs y Skeletons |
| 32 | Invisibilidad bloqueada |
| 35 | Stonecutter, Darkness, Ender Dragon y Sniffer |
| 40 | Transformaciones masivas + PvP |
| 45 | Saturación infinita + lava letal |
| 50 | Ultra Ravagers, Creepers especiales, Giants y mobs reforzados |
| 55 | Puertas/botones/vallas letales; cofres dañinos |
| 57 | Grass Block y Slabs dañinos |
| 60 | Dificultad máxima: agua, Elytra, Warden, herramientas, mobs y bloques |

# Límite

El sistema limita la progresión entre el **día 1 y el día 60**.

El **día 60** es actualmente el nivel máximo de dificultad implementado.
