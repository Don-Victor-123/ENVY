# Console app grax — instrucciones para Java 21 (Windows)

Este pequeño proyecto Java está configurado para compilarse con Java 21 (LTS). Aquí tienes pasos para instalar JDK 21, compilar y ejecutar.

1) Instalar JDK 21 (Adoptium/Eclipse Temurin) usando PowerShell (ejecutar como Administrador si hace falta):

```powershell
# Descarga e instala (usa winget si está disponible):
winget install --id EclipseAdoptium.Temurin.21.JDK -e --source winget

# Alternativa: descarga manual desde https://adoptium.net y ejecuta el instalador
```

2) Verificar la instalación y la versión:

```powershell
java -version
javac -version
```

3) Compilar con Maven (si instalaste Maven):

```powershell
cd "c:\Users\Android\Documents\ENVY\Console app grax"
mvn -v
mvn clean package

# Ejecutar las clases compiladas:
java -cp target/classes Console
```

4) Compilar y ejecutar sin Maven (usando javac/java):

```powershell
cd "c:\Users\Android\Documents\ENVY\Console app grax"
javac -d out --release 21 src\main\java\*.java
java -cp out Console
```

Notas:
- El proyecto es pequeño y no tiene dependencias externas. El `pom.xml` añadido facilita fijar la versión de compilación a Java 21.
- En esta sesión verifiqué localmente que Temurin JDK 21 (21.0.8) se instaló y la aplicación compila y corre bajo Java 21.
- Si usas un IDE (IntelliJ/Eclipse/VSCode), actualiza el JDK del proyecto a Java 21 en la configuración del IDE.

## Día operativo (20:00–08:00) y reimpresión por fecha

Los tickets y cortes ahora se clasifican por *día operativo* (DO). Cada DO comienza a las 20:00 y termina a las 08:00 del
día siguiente en la zona `America/Mexico_City`. Los tickets vendidos entre las 20:00 y las 07:59:59 se guardan en la carpeta
`data/tickets/YYYY-MM-DD`, donde la fecha corresponde al inicio del turno nocturno. Los cortes se generan dentro de
`data/cortes/YYYY-MM-DD` y siempre incluyen el rango completo del turno (`DO 20:00 → DO+1 08:00`).

Para reimpresión se puede usar el menú de consola “Reimprimir corte por fecha (YYYY-MM-DD)” e ingresar la fecha del DO que se
desea consultar. Si el archivo del corte no existe pero hay tickets para ese DO, el sistema regenerará el corte antes de
imprimirlo.
