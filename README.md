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
javac -d out --release 21 *.java
java -cp out Console
```

Notas:
- El proyecto es pequeño y no tiene dependencias externas. El `pom.xml` añadido facilita fijar la versión de compilación a Java 21.
- En esta sesión verifiqué localmente que Temurin JDK 21 (21.0.8) se instaló y la aplicación compila y corre bajo Java 21.
- Si usas un IDE (IntelliJ/Eclipse/VSCode), actualiza el JDK del proyecto a Java 21 en la configuración del IDE.

## Operación nocturna (20:00–08:00)

La aplicación trabaja con un **día operativo (DO)** que inicia a las 20:00 y termina a las 07:59:59 del día calendario
siguiente. Todas las ventas impresas entre esos horarios se guardan en la carpeta `cortes/YYYY-MM-DD/`, donde
`YYYY-MM-DD` corresponde al DO. Esto permite que, por ejemplo, las ventas del viernes en la madrugada (después de
medianoche) sigan acumulándose en la carpeta del viernes.

### Generación de cortes

Al generar un corte, el sistema imprime el rango real de la jornada (`DO 20:00 → DO+1 08:00`) y utiliza todos los tickets
registrados en esa ventana, incluso si la aplicación se reinicia cruzando medianoche. Si la opción de imprimir al salir está
activa, el programa solicitará la contraseña y generará el corte del DO vigente antes de cerrar.

### Reimpresión por fecha

Desde el menú de la consola se agregó la opción `5) Reimprimir corte por fecha (YYYY-MM-DD)`. Introduce la fecha del DO
que deseas reimprimir (formato ISO). El sistema reconstruye el corte con los tickets del DO, incluso cuando existan
históricos guardados en dos carpetas calendario (legacy). Si ya existe `cortes/DO/corte.txt`, se actualiza con el nuevo
formato y se imprime nuevamente.
