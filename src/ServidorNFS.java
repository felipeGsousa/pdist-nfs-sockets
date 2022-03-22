import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServidorNFS {

    public static void main(String[] args) throws IOException {

        System.out.println("-------------- Servidor NFS --------------");

        ServerSocket serverSocket = new ServerSocket(7001);
        Socket socket = serverSocket.accept();

        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
        DataInputStream dis = new DataInputStream(socket.getInputStream());

        String OSName = System.getProperty("os.name");
        String sep = "";
        if (OSName.startsWith("Windows")) sep = "\\";
        else sep = "/";
        String missingCMD = "Digite algum dos comandos a seguir: readdir, rename, remove, create";

        while (true) {
            System.out.println("Aguardando ação do cliente...");

            String HOME = System.getProperty("user.home");
            String mensagem = dis.readUTF();
            String comando = mensagem.split(" ")[0];

            if (comando != ""){
                if(comando.equals("readdir")) {
                    if (mensagem.split("-").length > 1){
                        String relativePath = verifyPath(mensagem.split("-")[1], sep);
                        HOME = HOME + sep + relativePath;
                    }


                    Path file = Paths.get(HOME);
                    if (Files.exists(file)) {
                        Set<String> archives = readdir(HOME);

                        String formattedArch = "\nExibindo conteudo do diretorio: "+HOME+" para o cliente\n\n";

                        for (String archive : archives) {
                            formattedArch += archive + "\n";
                        }

                        if (archives.isEmpty()) dos.writeUTF("Pasta Vazia");
                        else{ dos.writeUTF(formattedArch); }
                    } else {
                        dos.writeUTF("Nao encontrado");
                    }

                } else if (comando.equals("rename")){

                    String fileName = mensagem.split("-")[1].trim();
                    String newName = mensagem.split("-")[2].trim();

                    fileName = verifyPath(fileName, sep);
                    newName = verifyPath(newName, sep);

                    rename(HOME, fileName, newName, sep);

                    dos.writeUTF("Aguardando Comando...");

                } else if (comando.equals("remove")) {

                    String fileName = mensagem.split("-")[1].trim();
                    fileName = verifyPath(fileName, sep);
                    remove(HOME, fileName, sep);
                    dos.writeUTF("Aguardando Comando...");

                } else if (comando.equals("create")) {

                    String fileName = mensagem.split("-")[1].trim();
                    fileName = verifyPath(fileName, sep);
                    create(HOME, fileName, sep);
                    dos.writeUTF("Aguardando Comando...");

                } else {
                    dos.writeUTF(missingCMD);
                }
            } else {
                dos.writeUTF(missingCMD);
            }
        }
    }
    public static String verifyPath(String path, String sep){
        String newPath = "";
        if (path.contains("/")){
            for (String relativePath : path.split("/")){
                newPath += relativePath + sep;
            }

            return newPath.substring(0,newPath.length()-1);
        }
        return path;
    }
    public static Set<String> readdir (String dir) {
        return Stream.of(new File(dir).list()).collect(Collectors.toSet());
    }
    public static void rename (String dir, String fileName, String newFileName, String sep) throws IOException {
        Path file = Paths.get(dir+sep+fileName);
        if (Files.exists(file)) {
            Files.move(file, file.resolveSibling(newFileName),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }
    public static void remove (String dir, String fileName, String sep) throws IOException {
        Path file = Paths.get(dir+sep+fileName);
        if (Files.exists(file)) {
            Files.delete(file);
        }
    }
    public static void create (String dir, String fileName, String sep) throws IOException {
        Path file = Paths.get(dir+sep+fileName);
        if (!Files.exists(file)) {
            if (fileName.split("\\.").length > 1){
                Files.createFile(file);
            }else {
                Files.createDirectory(file);
            }
        }
    }
}
