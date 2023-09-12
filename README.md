# TP1-POD
Repositorio para el trabajo práctico N°1 de la materia "72.42 - Programación de Objetos Distribuidos" - ITBA

**Grupo 7:**
 - 61100 - De Simone, Franco
 - 61101 - Dizenhaus, Manuel
 - 61278 - Anselmo, Sol

## Compilar el proyecto
Ejecutar
```bash
mvn clean install
```
Para compilar el proyecto con maven. Luego extraer los .tar.gz
```bash
mkdir -p tmp && find . -name '*tar.gz' -exec tar -C tmp -xzf {} \;
find . -path './tmp/tpe1-g7-*/*' -exec chmod u+x {} \;
find . -name '*.sh' -exec sed -i -e 's/\r$//' {} \;
```
Esto extraerá los .tar.gz generados, guardándolos en un directorio temporal `./tmp` y le dará permisos de ejecución a los `.sh` tanto del cliente como del servidor.

Para evitar todos estos pasos se puede ejecutar el script de bash `./run-scripts.sh` o `./run-scripts-no-mvn.sh` (luego de ejecutar el primer comando).

## Correr el servidor
```bash
cd ./tmp/tpe1-g7-server-2023.2Q/
./run-server.sh
```
## Correr el cliente de administración del parque
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
## Correr el cliente de notificaciones de una atracción
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
## Correr el cliente de consulta
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
- **output.txt** es el path del archivo de salida con los resultados de la consulta elegida.