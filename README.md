# TP1-POD
Repositorio para el trabajo práctico N°1 de la materia "72.42 - Programación de Objetos Distribuidos" - ITBA

## 1. Compilar el proyecto
Siguiendo los pasos provistos por la cátedra en el archivo *gRPC - Communication Patterns*, es necesario primero construir el proyecto corriendo:

```bash
mvn clean install
```
Esto generará un archivo **.tar.gz** con las clases y los jars listos para usar. Luego,
es necesario descomprimir los `-bin.tar.gz` y darle permisos. Esto se puede realizar con los siguientes dos comandos:

```bash
mkdir -p tmp && find . -name '*tar.gz' -exec tar -C tmp -xzf {} \;
find . -path './tmp/tpe1-g7-*/*' -exec chmod u+x {} \;
```
Esto crea una carpeta temporal `tmp`, que es donde estarán nuestros archivos extraidos. Luego,
le otorgamos los permisos de ejecución correspondientes. 

Es importante notar que la compilación de los `.sh` presenta inconvenientes 
cuando se compilan en windows. Al compilarse el proyecto en windows, se genera
al final de las lineas el `\n\r`. Esto es algo que impide la correcta ejecución de los scripts, dado que 
marca un error al no reconocer las lineas desde un sistema *Linux*. Para esto,
se tiene el siguiente comando que elimina el `\r` de todas las lineas, dejando así comandos
ejecutables en un sistema `Linux`:
```
find . -name '*.sh' -exec sed -i -e 's/\r$//' {} \;
```
En la raíz del proyecto encontrarán dos scripts: `./run-scripts.sh` y 
`./run-scripts-no-mvn.sh`. Sus nombres son autoexplicativos, pero ahorran el correr los comandos uno por uno.
Es posible que se necesite dar permisos de ejecución a los mismos o correr una terminal en modo administrador.

- Nota: Los scripts al ser ejectuados por el equipo fueron notoriamente mas lentos que correr los comandos individualmente.

En este punto, se debería poder acceder al directorio `/tmp`, y encontrar dos subcarpetas: 
`tpe1-g7-client-2023.2Q` y `tpe1-g7-server-2023.2Q`. Estas dos carpetas contienen los scripts necesarios para
correr tanto el servidor como los 4 clientes con la sintaxis indicada en la consigna.

## 2. Correr el servidor
```bash
cd ./tmp/tpe1-g7-server-2023.2Q/
./run-server.sh
```
## 3.1 Correr el cliente de administración del parque
```bash
cd ./tmp/tpe1-g7-client-2023.2Q
./admin-cli.sh -DserverAddress=xx.xx.xx.xx:yyyy -Daction=actionName [ -DinPath=filename | -Dride=rideName | -Dday=dayOfYear | -Dcapacity=amount ]
```
Donde
- **xx.xx.xx.xx:yyyy** es la dirección IP y el puerto donde está publicado el servicio de administración del parque.
- **actionName** es el nombre de la acción a realizar.
  - **rides:** Agrega un lote de atracciones.
  - **tickets:** Agrega un lote de pases.
  - **slots:** Carga la capacidad amount de los slots de la atracción con nombre rideName para el día del año dayOfYear.
## Correr el cliente de reserva de atracciones
```bash
cd ./tmp/tpe1-g7-client-2023.2Q
./book-cli.sh -DserverAddress=xx.xx.xx.xx:yyyy -Daction=actionName [ -Dday=dayOfYear -Dride=rideName -Dvisitor=visitorId -Dslot=bookingSlot -DslotTo=bookingSlotTo ]
```
Donde
- **xx.xx.xx.xx:yyyy** es la dirección IP y el puerto donde está publicado el servicio de reserva de atracciones.
- **actionName** es el nombre de la acción a realizar.
  - **attractions:** Deberá imprimir en pantalla el detalle de las atracciones.
  - **availability:** Deberá imprimir en pantalla la disponibilidad de las atracciones para el día del año dayOfYear a partir de uno de los siguientes criterios:
    - Un slot de una atracción a partir del nombre de la atracción rideName y el slot bookingSlot.
    - Un rango de slots de una atracción a partir del nombre de la atracción rideName y el rango de slots [bookingSlot, bookingSlotTo].
    - Un rango de slots de todas las atracciones del parque a partir del rango de slots [bookingSlot, bookingSlotTo].
  - **book:** Deberá realizar una reserva para el visitante visitorId para visitar la atracción rideName en el día del año dayOfYear en el slot bookingSlot.
  - **confirm:** Deberá confirmar una reserva para el visitante visitorId para visitar la atracción rideName en el día del año dayOfYear en el slot bookingSlot.
  - **cancel:** Deberá cancelar una reserva para el visitante visitorId para visitar la atracción rideName en el día del año dayOfYear en el slot bookingSlot.
## 3.2 Correr el cliente de notificaciones de una atracción
```bash
cd ./tmp/tpe1-g7-client-2023.2Q
./notif-cli.sh -DserverAddress=xx.xx.xx.xx:yyyy -Daction=actionName -Dday=dayOfYear -Dride=rideName -Dvisitor=visitorId
```
Donde
- **xx.xx.xx.xx:yyyy** es la dirección IP y el puerto donde está publicado el servicio de notificaciones de una atracción.
- **actionName** es el nombre de la acción a realizar.
  - **follow:** Registrar a un visitante para que sea notificado de los eventos de una atracción.
    - dayOfYear: el día del año.
    - rideName: el nombre de la atracción sobre la cual ser notificado.
    - visitorId: el id del visitante.
  - **unfollow:** Anular el registro, esto es dejar de recibir notificaciones, a partir de los mismos parámetros que la acción follow.
## 3.3 Correr el cliente de consulta
```bash
cd ./tmp/tpe1-g7-client-2023.2Q
./query-cli.sh -DserverAddress=xx.xx.xx.xx:yyyy -Daction=actionName -Dday=dayOfYear -DoutPath=output.txt
```
Donde
- **xx.xx.xx.xx:yyyy** es la dirección IP y el puerto donde está publicado el servicio de consulta.
- **dayOfYear** es el día del año.
- **actionName** es el nombre de la acción a realizar.
  - **capacity:** Resuelve la Consulta 1.
  - **confirmed:** Resuelve la Consulta 2.
- **output.txt** es el path del archivo de salida con los resultados de la 
consulta elegida.
---

### Observaciones
- En la consigna se pedía que los clientes corrieran desde terminal sin incluir la extensión `.sh` (Ejemplo: `./query-cli ...`). Esto depende de la configuración de la terminal, pero en el caso de este desarrollo (y como está aclarado en este README), el proyecto se corrió con la extensión incluida.
- Otra aclaración pertinente es que en la consigna se mezclaba el uso del término _ride_ con _attraction_ para indicar lo mismo (en algunos ejemplos se usaba de una forma, en otros con la otra). Nos tomamos la libertad de unificarlo bajo _ride_, por lo que todos los llamados que involucren pasar el nombre de la atracción usan la _System Property_ con el nombre de _ride_.

## Integrantes:
Nombre | Legajo
-------|--------
[De Simone, Franco](https://github.com/desimonef) | 61100
[Dizenhaus, Manuel](https://github.com/ManuelDizen) | 61101
Anselmo, Sol | 61278
