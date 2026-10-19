# CampusGo

## Descripción
CampusGo es una aplicación móvil Android para registrar, consultar y administrar lugares de interés dentro de un campus universitario.

## Funcionalidades
- Crear, consultar, editar y eliminar lugares.
- Buscar y filtrar lugares por categoría.
- Registrar descripciones y coordenadas.
- Seleccionar fotografías de la galería o capturarlas con la cámara.
- Abrir ubicaciones en una aplicación de mapas.
- Guardar registros y fotografías localmente.

## Tecnologías
- Kotlin
- Android SDK
- AndroidX AppCompat
- Material Components
- Gradle con Kotlin DSL
- JUnit

## Arquitectura
- Presentación: MainActivity y PlacePresenter.
- Negocio: PlaceService y PhotoService.
- Repositorios: contratos e implementaciones para acceder a los datos.
- Origen de datos: almacenamiento local de lugares y fotografías.
- Entidades: Place.

## Mockups

Los diseños de referencia se encuentran en `docs/mockups/` y utilizan los estilos compartidos de `docs/styles/mockups.css`.

- [Listado de lugares](docs/mockups/listado-lugares.html)
- [Formulario de lugar](docs/mockups/formulario-lugar.html)
- [Confirmación de eliminación](docs/mockups/confirmar-eliminacion.html)
- [Logotipo conceptual](docs/mockups/campusgo-logo.svg)

Los mockups representan propuestas visuales de referencia. La interfaz funcional de Android se implementa con Kotlin y componentes nativos de Material.

## Ejecución
1. Abrir el proyecto en Android Studio.
2. Esperar la sincronización de Gradle.
3. Seleccionar un dispositivo compatible.
4. Ejecutar la aplicación.

Para verificar el proyecto, ejecutar en PowerShell:
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug

## Estado
Proyecto académico en desarrollo.

## Repositorio
https://github.com/MineProjects-Caguiret/campus.go
