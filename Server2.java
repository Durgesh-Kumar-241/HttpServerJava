package com.dktechhub.mnnit.myapplication;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class Server2 {
    public static void main(String[] args) throws IOException {
        int port = 2004;
        System.out.println("Waiting on port :" + port + "\n");
        ServerSocket server = new ServerSocket(port);
        while (true) {
           new SingleClientHandler(server.accept(), System.out::println).start();
        }
    }

    public static class SingleClientHandler extends Thread {
        Socket socket;
        Notify notify;
        InputStream inputStream;
        OutputStream outputStream;
        InputStreamReader inputStreamReader;
        BufferedReader bufferedReader;
        PrintWriter  printWriter;
        HashMap<String, String> requestheaders = new HashMap<>();
        HashMap<String, String> responceheaders = new HashMap<>();
        String requestMethod,requestUri;
        public static final String root = "C:\\WWW\\htdocs";
        public static String current;
        public SingleClientHandler(Socket socket, Notify notify) throws IOException {
            this.socket = socket;
            this.notify = notify;
            this.inputStream = this.socket.getInputStream();
            this.outputStream = this.socket.getOutputStream();
            this.inputStreamReader = new InputStreamReader(this.inputStream);
            this.bufferedReader = new BufferedReader(inputStreamReader);
            this.printWriter=new PrintWriter(outputStream);
        }

        @Override
        public void run() {
            super.run();
            this.notify.notifyText("New connection arrived");
            current = root;
            try {
                this.decodeHeaders();
                if(this.requestMethod.equals("GET"))
                this.respondWithFile(getPathForRequestedUri(requestUri));
                else {
                    this.receiveFile();
                    this.respond("http ok",200);
                }
                this.finish();
                //System.out.println(this.getString());
                System.out.println("Connection closed");

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        public void decodeHeaders() throws IOException {
            String header;
            int temp;
            header = bufferedReader.readLine();
            this.requestMethod=(header.substring(0, header.indexOf(' ')));
            this.requestUri= URLDecoder.decode(header.substring(header.indexOf(' ') + 1, header.lastIndexOf(' ')));
            while ((header = bufferedReader.readLine()).length() != 0) {
                if (header.contains(":")) {
                    temp = header.indexOf(':');
                    this.requestheaders.put(header.substring(0, temp), header.substring(temp + 2));
                }
            }
        }
        public void receiveFile() throws IOException {
            String uploadPath="C:\\Users\\kdurg\\OneDrive\\Desktop\\uploads";
            String type = this.requestheaders.get("Content-Type");
            long length = Long.parseLong(this.requestheaders.get("Content-Length"));
            if (length < 0)
                return;
            String name = this.requestheaders.get("Content-Disposition");
            name = name.replace("fileName=","");
            System.out.println("receiving file:"+name+'\t'+"Length:"+length+'\t'+"Type:"+type);
            FileOutputStream outputStream=new FileOutputStream(new File(uploadPath,name));
            int count,readtotal=0;
            byte[] buffer = new byte[1024*1024*6];
            while ((count=inputStream.read(buffer))>0)
            {
                readtotal+=count;
                this.notify.notifyText("progress:" + (readtotal * 100) / length);
                //System.out.println(count);
                outputStream.write(buffer,0,count);
            }


            System.out.println("Received File");
            //inputStream.close();
            outputStream.close();
        }
        public void respond(String message,int statusCode)
        {
            printWriter.print("HTTP/1.1 " + statusCode + " \r\n");
            printWriter.print("Content-Type: text/plain\r\n");
            printWriter.print("Connection: close\r\n");
            printWriter.print("\r\n");
            printWriter.print(message + "\r\n");
            printWriter.close();
        }
        public void respondWithFile(String filePath) {
            try {
                File f = new File(filePath);
                if (f.isFile()) {
                    String contentType = guessContentType(Paths.get(filePath));
                    FileInputStream fileInputStream = new FileInputStream(f);
                    byte[] buffer = new byte[1024*1024*6];
                    DataInputStream dis = new DataInputStream(new BufferedInputStream(fileInputStream));

                    outputStream.write(("HTTP/1.1 200 \r\n").getBytes());
                    outputStream.write(("Content-Type: " + contentType + "\r\n").getBytes());
                    outputStream.write(("Content-Length: " + f.length() + "\r\n").getBytes());
                    outputStream.write(("Content-Disposition: inline; filename=\""+f.getName()+"\"\r\n").getBytes());
                    outputStream.write("\r\n".getBytes());
                    this.notify.notifyText("Sending file:" + f.getName());
                    long x, written = 0;
                    long total = f.length();
                    while ((x = dis.read(buffer)) > 0) {
                        outputStream.write(buffer,0, (int) x);
                        written += x;
                        this.notify.notifyText("progress:" + (written * 100) / total);
                    }
                    this.notify.notifyText("Sending file completed");
                    outputStream.write("\r\n\r\n".getBytes());
                    outputStream.flush();
                    outputStream.close();
                    fileInputStream.close();
                } else if (f.isDirectory()) {
                    File[] list = f.listFiles();
                    outputStream.write(("HTTP/1.1 200 \r\n").getBytes());
                    outputStream.write(("Content-Type: text/html\r\n").getBytes());
                    outputStream.write("\r\n".getBytes());
                    outputStream.write("<html>\r\n".getBytes());
                    for (File s : list
                    ) {
                        String temp;
                        temp = s.getAbsolutePath().replace(root, "");
                        outputStream.write(("<a href='" + temp + "'>" + s.getName() + "</a><br>" + "\r\n").getBytes());

                    }
                    outputStream.write("</html>\r\n".getBytes());
                    outputStream.write("\r\n\r\n".getBytes());
                    outputStream.flush();
                    current = f.getPath();

                } else {
                    this.respond("Error 404:Not Found", 404);
                }
            } catch (Exception e) {

                this.notify.notifyText("Error " + e.toString());
            }


        }
        public void finish() throws IOException {
            bufferedReader.close();
            inputStreamReader.close();
            printWriter.close();
            inputStream.close();
            outputStream.close();
            socket.close();
        }
        public static String getPathForRequestedUri(String path) {
            String temp;
            if (path.charAt(0) == '/') {
                temp = root + path.replace('/', '\\');
            } else {
                temp = current + path.replace('/', '\\');
            }
            System.out.println("Requested:" + path + "\t returning" + temp);
            return temp;
        }
        private static String guessContentType(Path filePath) throws IOException {
            return Files.probeContentType(filePath);
        }
        public String getString() {
            //super.toString();
            return "Method:"+requestMethod+'\n'+"Uri:"+requestUri+'\n'+"headers:"+requestheaders.toString();
        }

        public interface Notify {
            void notifyText(String text);
        }
    }
}
