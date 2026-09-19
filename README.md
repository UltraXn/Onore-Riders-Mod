<div align="center">

# 🦗 Onore Riders Mod (KRM - REvolution)
### *A Next-Generation Tokusatsu Experience for Minecraft*

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-378805.svg?style=for-the-badge&logo=minecraft&logoColor=white)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.x-orange.svg?style=for-the-badge&logo=neoforged&logoColor=white)](https://neoforged.net/)
[![GeckoLib](https://img.shields.io/badge/GeckoLib-4.x-blueviolet.svg?style=for-the-badge)](https://geckolib.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-DSL_Engine-7F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg?style=for-the-badge)](LICENSE)

**¡Transfórmate, desata el poder del Arcle y ejecuta finishers legendarios con auténticas físicas Tokusatsu!**

[Características](#-características-principales) • [Kamen Rider Kuuga](#-kamen-rider-kuuga) • [Controles](#-controles-y-atajos) • [Instalación](#-instalación) • [Desarrollo](#-desarrollo)

---

</div>

## 🌟 Características Principales

- ⚡ **Motor Modular de Riders (Kotlin DSL)**: Arquitectura desacoplada y escalable basada en Kotlin (`com.neroferno.krm_onore.engine`) diseñada para albergar futuras eras y riders (Heisei, Showa, Reiwa) con formas, stats y finishers independientes.
- 🦹 **Arcle Driver 3D Animado (GeckoLib)**: Modelo 3D de alta definición para el cinturón con rotación dinámica, renderizado adaptativo en inventario/manos y texturas reactivas que cambian según la forma activa (*Off, Growing, Mighty, Dragon, Pegasus, Titan*).
- 🥋 **Físicas de Combate Tokusatsu**:
  - **Apex Hover**: Suspensión aérea cinematográfica al iniciar la patada.
  - **Bézier Dive Kick**: Trayectoria parabólica de impacto hacia el objetivo bloqueado.
  - **Hit-Stop & Slide**: Congelamiento de impacto y deslizamiento inercial tras conectar el golpe.
  - **Explosión Final**: Daño AOE masivo y efectos de partículas tokusatsu.
- 🖥️ **Rider GUI Personalizada**: Menú interactivo estilizado con compartimento de 6 ranuras dedicadas (Armadura, Mano Secundaria y Ranura de Driver).
- 🛠️ **Mobiliario Especializado**: Bloques temáticos de crafteo y lore: *Mesa de Trabajo Rider* y *Escritorio de Investigador*.

---

## 🔴 Kamen Rider Kuuga

El legendario guerrero de la antigüedad adaptado con progresión canónica completa y estadísticas diferenciadas:

| Forma | Aspecto / Color | Rol & Especialización | Finisher Canónico |
| :--- | :--- | :--- | :--- |
| **Growing Form** | Blanco (`#E0E0E0`) | Forma básica inicial sin despertar completo | Ataque cuerpo a cuerpo |
| **Mighty Form** | Rojo (`#CC1111`) | Equilibrio perfecto de daño, defensa y velocidad | **Mighty Kick** (Radio 3.0 AOE) |
| **Dragon Form** | Azul (`#1144CC`) | Máxima agilidad, salto potenciado (`Jump II`) | Splash Kick ágil |
| **Pegasus Form** | Verde (`#11AA44`) | Sentidos aumentados y combate a distancia | Disparo de precisión |
| **Titan Form** | Púrpura (`#771199`) | Tanque acorazado con Resistencia pasiva | Impacto pesado de gran masa |
| **Amazing Mighty** | Negro y Dorado (`#222222`) | Salto de poder devastador | **Amazing Rider Kick** (Radio 4.5 AOE) |
| **Ultimate Form** | Negro Profundo (`#050505`) | El poder definitivo del guerrero oscuro | **Ultimate Kick** (Radio 6.0 AOE, 65.0 DMG) |

---

## 🎮 Controles y Atajos

Por defecto, puedes configurar las siguientes teclas en el menú de **Opciones > Controles > Onore Rider**:

| Tecla Predeterminada | Acción | Descripción |
| :---: | :--- | :--- |
| <kbd>K</kbd> | **Transformarse / Destransformarse** | Activa la transformación si tienes el Arcle Driver equipado en el slot. |
| <kbd>V</kbd> | **Riderkick / Finisher** | Ejecuta la patada tokusatsu hacia el objetivo apuntado (alcance máx: 20 bloques). |
| <kbd>R</kbd> | **Abrir Menú Rider** | Abre la interfaz de equipamiento del cinturón y armadura. |

> [!TIP]
> Puedes equiparte el cinturón haciendo **clic derecho** mientras lo sostienes en la mano principal o colocándolo directamente en la ranura de Driver dentro de la **Rider GUI** (<kbd>R</kbd>).

---

## 📦 Instalación

### Requisitos Previos:
1. **Minecraft**: `1.21.1`
2. **Mod Loader**: [NeoForge](https://neoforged.net/) (`>= 21.1.0`)
3. **Dependencias Obligatorias**:
   - [GeckoLib 4](https://www.curseforge.com/minecraft/mc-mods/geckolib)
   - [Player Animator](https://www.curseforge.com/minecraft/mc-mods/playeranimator)

### Pasos:
1. Descarga el archivo `.jar` más reciente de la sección de Releases.
2. Coloca el `.jar` en tu carpeta `.minecraft/mods`.
3. Inicia Minecraft con el perfil de **NeoForge 1.21.1**.

---

## 💻 Desarrollo y Compilación

Para clonar y compilar el proyecto en tu entorno local:

```bash
# Clonar el repositorio
git clone https://github.com/UltraXn/Onore-Riders-Mod.git
cd Onore-Riders-Mod

# Compilar el código Java y Kotlin
./gradlew build

# Ejecutar cliente de pruebas de Minecraft
./gradlew runClient
```

---

## 🤝 Créditos y Agradecimientos

- **Desarrollado por**: [UltraXn](https://github.com/UltraXn) & Colaboradores.
- **Inspiración**: Franquicia *Kamen Rider* (Toei Company Ltd. & Shotaro Ishinomori).
- **Herramientas de Modelado**: [Blockbench](https://www.blockbench.net/).
- **Agradecimientos especiales**: A la comunidad tokusatsu y a todos los que apoyan el proyecto en GitHub y Discord.

<div align="center">

*¡Cho-Henshin!* ⚡

</div>
