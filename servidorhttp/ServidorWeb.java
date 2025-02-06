import java.io.*;
import java.net.*;
import java.util.StringTokenizer;

public final class ServidorWeb {
    public static void main(String argv[]) throws Exception {
        // Establece el número de puerto.
        int puerto = 6789;

        // Estableciendo el socket de escucha.
        ServerSocket socketEscucha = new ServerSocket(puerto);
        System.out.println("Servidor web iniciado en el puerto " + puerto + "...");

        // Procesando las solicitudes HTTP en un ciclo infinito.
        while (true) {
            // Escuchando las solicitudes de conexión TCP.
            Socket socketConexion = socketEscucha.accept();
            System.out.println("Conexión aceptada desde " + socketConexion.getInetAddress() + ":" + socketConexion.getPort());

            // Construye un objeto para procesar el mensaje de solicitud HTTP.
            SolicitudHttp solicitud = new SolicitudHttp(socketConexion);

            // Crea un nuevo hilo para procesar la solicitud.
            Thread hilo = new Thread(solicitud);

            // Inicia el hilo.
            hilo.start();
        }
    }
}

final class SolicitudHttp implements Runnable {
    final static String CRLF = "\r\n";
    private Socket socket;

    // Constructor
    public SolicitudHttp(Socket socket) throws Exception {
        this.socket = socket;
    }

    // Implementa el método run() de la interface Runnable.
    public void run() {
        try {
            proceseSolicitud();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private String contentType(String nombreArchivo) {
        if (nombreArchivo.endsWith(".html")) {
            return "text/html";
        } else if (nombreArchivo.endsWith(".txt")) {
            return "text/plain";
        } else if (nombreArchivo.endsWith(".jpg") || nombreArchivo.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (nombreArchivo.endsWith(".css")) {
            return "text/css";
        } else if (nombreArchivo.endsWith(".gif")) {
            return "image/gif";
        } else if (nombreArchivo.endsWith(".js")) {
            return "application/javascript";
        }
        return "application/octet-stream";  // Tipo por defecto para archivos binarios.
    }

    private static void enviarBytes(FileInputStream fis, OutputStream os) throws Exception
    {
        // Construye un buffer de 1KB para guardar los bytes cuando van hacia el socket.
        byte[] buffer = new byte[1024];
        int bytes = 0;

        // Copia el archivo solicitado hacia el output stream del socket.
        while((bytes = fis.read(buffer)) != -1 ) {
        os.write(buffer, 0, bytes);
        }
    }

    private void proceseSolicitud() throws Exception {
        // Referencia al stream de salida del socket.
        DataOutputStream os = new DataOutputStream(socket.getOutputStream());

        // Referencia y filtros (InputStreamReader y BufferedReader) para el stream de entrada.
        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Recoge la línea de solicitud HTTP del mensaje.
        String lineaDeSolicitud = br.readLine();

        // Muestra la línea de solicitud en la pantalla.
        System.out.println("\nSolicitud recibida: " + lineaDeSolicitud);

        ////////////////////////////////////////////////////////////////////////
        //Parte 2
        ////////////////////////////////////////////////////////////////////////
        
        // Extrae el nombre del archivo de la línea de solicitud.
        StringTokenizer partesLinea = new StringTokenizer(lineaDeSolicitud);
        partesLinea.nextToken();  // "salta" sobre el método, se supone que debe ser "GET"
        String nombreArchivo = partesLinea.nextToken();

        // Anexa un ".", de tal forma que el archivo solicitado debe estar en el directorio actual.
        nombreArchivo = "." + nombreArchivo;

        // Abre el archivo seleccionado.
        FileInputStream fis = null;
        boolean existeArchivo = true;
        try {
            fis = new FileInputStream(nombreArchivo);
        } catch (FileNotFoundException e) {
            existeArchivo = false;
        }

        // Construye el mensaje de respuesta.
        String lineaDeEstado = null;
        String lineaDeTipoContenido = null;
        String cuerpoMensaje = null;
        if (existeArchivo) {
            lineaDeEstado = "HTTP/1.1 200 OK" + CRLF;
            lineaDeTipoContenido = "Content-type: " + 
            contentType( nombreArchivo ) + CRLF;
        } else {
            lineaDeEstado = "HTTP/1.1 404 Not Found" + CRLF;
            lineaDeTipoContenido = "Content-type: text/html; charset=UTF-8" + CRLF;
            cuerpoMensaje = "<HTML>" + 
                "<HEAD><TITLE>404 Not Found</TITLE></HEAD>" +
                "<BODY><b>404</b> Not Found</BODY></HTML>";
        }

        // Envia la línea de estado.
        os.writeBytes(lineaDeEstado);

        // Envía el contenido de la línea content-type.
        os.writeBytes(lineaDeTipoContenido);

        // Envía una línea en blanco para indicar el final de las líneas de header.
        os.writeBytes(CRLF);

        // Envía el cuerpo del mensaje.
        if (existeArchivo) {
            enviarBytes(fis, os);
            fis.close();
        } else {
            os.writeBytes(cuerpoMensaje);
        }

        System.out.println("!!!!!!!!!!!!!!!!");
        System.out.println("Solicitud de archivo hecha");
        System.out.println("¡¡¡¡¡¡¡¡¡¡¡¡¡¡¡¡");

        ////////////////////////////////////////////////////////////////////////
        //Fin Parte 2
        ////////////////////////////////////////////////////////////////////////

        // Recoge y muestra las líneas de header.
        String lineaDelHeader;
        while ((lineaDelHeader = br.readLine()).length() != 0) {
            System.out.println(lineaDelHeader);
        }

        // Construir una respuesta HTTP básica.
        String respuesta = "HTTP/1.1 200 OK" + CRLF +
                           "Content-Type: text/html; charset=UTF-8" + CRLF + CRLF +
                           "<html style=\"color:blue;\"><body>" +
                           "<h1>Servidor Web Funcionando</h1>" +
                           "<p>Revisa la consola del servidor.</p>" +
                           "</body></html>";

        // Enviar la respuesta al cliente.
        os.writeBytes(respuesta);

        // Cerrar los flujos y el socket.
        os.close();
        br.close();
        socket.close();
    }
}