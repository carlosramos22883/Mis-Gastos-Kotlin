# Mis Gastos

## Descripción
Mis Gastos es una aplicación de Android desarrollada en Kotlin que permite a los usuarios controlar sus gastos personales.

El objetivo principal de este proyecto es tener una herramienta móvil que va de la mano con el proyecto Mis Gastos - Laravel.

## Características
- Interfaz moderna y fluida con Jetpack Compose.
- Guardado de datos local con Room Database.
- Consumo de API con Retrofit y Coroutines.

## Tecnologías y Herramientas
Este proyecto está construido utilizando:
## Tecnologías y Herramientas
Este proyecto está construido utilizando:

### 1. Lenguaje y Entorno
- **Kotlin 2.0.20:** Lenguaje principal del proyecto.
- **Android Studio / Gradle (KTS):** Sistema de construcción usando sintaxis Kotlin.
- **JDK 17:** Versión de Java requerida para compilación.

### 2. Interfaz de Usuario (UI)
- **Jetpack Compose:** Framework declarativo moderno para UIs nativas.
- **Material3:** Sistema de diseño y componentes visuales.
- **Compose BOM (2024.09.02):** Gestión centralizada de versiones compatibles de Compose.
- **Material Icons Extended:** Set completo de iconos para la interfaz.
- **Activity Compose:** Integración de Compose con el ciclo de vida de la Activity.

### 3. Arquitectura y Estado
- **Hilt (Dagger):** Inyección de dependencias para desacoplar componentes.
- **ViewModel + Compose:** Gestión de estado y lógica de UI (`lifecycle-viewmodel-compose`).
- **Lifecycle Runtime Compose:** APIs seguras para el ciclo de vida dentro de Compose.
- **Navigation Compose:** Navegación entre pantallas integrada con Hilt (`hilt-navigation-compose`).

### 4. Red y Datos
- **Retrofit + OkHttp:** Cliente HTTP para consumir la API Laravel/MisGastos.
- **Gson Converter:** Serialización/deserialización JSON para Retrofit.
- **Kotlin Serialization:** Librería moderna de serialización de Kotlin.
- **OkHttp Logging Interceptor:** Depuración de peticiones y respuestas en Logcat.
- **DataStore Preferences:** Almacenamiento local asíncrono y seguro para tokens de sesión (Sanctum).

### 5. Autenticación
- **Credential Manager + Google Identity:** API moderna de Android para autenticación con Google (reemplazo de Google Sign-In legacy).

### 6. Asincronía
- **Kotlin Coroutines:** Manejo de operaciones asíncronas sin bloquear el hilo principal.

### 7. Testing
- **JUnit:** Pruebas unitarias locales.
- **Espresso:** Pruebas de UI tradicionales.
- **Compose UI Test JUnit4:** Pruebas instrumentadas específicas para Jetpack Compose.

## Licencia
Este proyecto está bajo la licencia [MIT](LICENSE). Eres libre de usarlo, modificarlo y distribuirlo con fines educativos o de referencia.

## Requisitos Previos
Antes de comenzar, asegúrate de tener instalado:
- [Android Studio Ladybug o superior](https://developer.android.com/studio)
- JDK 17 o superior
- Android SDK (API 24 o superior recomendado)

## Instalación y Ejecución
1. Clona este repositorio:
   ```bash
   git clone git@github.com:carlosramos22883/Mis-Gastos-Kotlin.git


# Mis Gastos

![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-blue)
![Android SDK](https://img.shields.io/badge/SDK-36-green)