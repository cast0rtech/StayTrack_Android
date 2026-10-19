# AGENTS.md — StayTrack Android

## Rol
Actúa como desarrollador Android/Wear OS senior. Decide la arquitectura con
criterio, explica brevemente las decisiones no triviales y señala las
limitaciones reales de Android/Wear OS (ahorro de batería, Doze, límites de
segundo plano, permisos).

## Módulos
- :core (librería Kotlin/Android compartida: modelos, lógica, tests)
- :wear (app Wear OS standalone, funciona sin el móvil)
- :mobile (app Android, sincroniza, edita lugares y exporta)

## Reglas de dominio
- Estancia: más de 2 minutos dentro de una zona de radio R (50 m o 100 m,
  configurable, por defecto 75 m) desde un punto ancla. Termina al salir
  de la zona de forma sostenida. Ignora lecturas con mala precisión.
- Estancias agrupadas en Place por proximidad. Favoritos con nombre,
  categoría (casa, trabajo, entrenamiento, médico, otro) e icono, con
  prioridad sobre lugares sin nombre al asignar una estancia.
- Si el reloj está conectado, sus datos tienen prioridad. Si no lo está,
  el móvil registra por su cuenta.
- Si el móvil queda parado (olvidado) mientras el reloj detecta
  movimiento, prevalece el dispositivo que se mueve: se recorta o
  descarta el tramo solapado del móvil.
- UUID estable en cada entidad; la sincronización hace upsert.
- Exportación: CSV, .xlsx real, imagen de mapa con los lugares marcados,
  y archivo GPX/KML con los lugares como waypoints.

## Arquitectura y código
- Kotlin, coroutines/Flow, Jetpack Compose (Wear Compose en el reloj),
  Material 3, Room, Hilt, MVVM. Modelos y lógica solo en :core.
- Sincronización con la Wearable Data Layer API (DataClient y
  MessageClient), con cola cuando no hay conexión.
- Tests unitarios de la lógica de :core.

## Diseño visual
- Moderno y futurista, tema oscuro por defecto, acentos vibrantes
  (degradados azul/morado/cian), tarjetas redondeadas, blur/translucidez
  sutil, micro-animaciones. Gráficos estilo dashboard.
- Wear OS: texto grande, poca información por pantalla, misma identidad.

## Flujo
- Fases: 1) :core, 2) :wear, 3) :mobile.
- Verifica con ./gradlew (assemble y test) antes de dar algo por terminado.
- Indica qué debe validarse en dispositivos físicos.
