import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
import java.util.Scanner;


public class Server {

    public static void main(String[] args) throws IOException {
        int port = 2004;
        Scanner scanner = new Scanner(System.in);
        System.out.println("Waiting on port :" + port + "\n");
        ServerSocket server = new ServerSocket(port);
        while (true) {
            new Handler(server.accept()).start();

        }

    }


    public static void mnotify(String message) {
        System.out.println(message);
    }


    public static class Handler extends Thread {
        Socket s;
        public static final String root = "C:\\WWW\\htdocs";
        public static String current;
        InputStream is;
        OutputStream os;

        Handler(Socket s) {
            this.s = s;
        }

        @Override
        public void run() {
            super.run();
            current = root;
            System.out.println("Connection detected");
            try {
                is = s.getInputStream();
                os = s.getOutputStream();
                Request r = new Request(s);
                r.decodeHeaders();
                if (r.method.equals("GET"))
                    new Response(os).respondWithFile(getPath(r.getUri()));
                else if (r.method.equals("POST")) {
                    r.handlePost();
                    new Response(os).respondWithText("Success---post request received");

                }
                is.close();
                os.close();
                System.out.println("Connection closed");
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        public static String getPath(String path) {
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

        public static class Request {
            HashMap<String, String> headers = new HashMap<>();
            String method = "GET";
            String uri = "/";

            Socket s;

            public void addHeader(String title, String value) {
                headers.put(title, value);
                System.out.println("Putting:" + title + "=" + value);
            }

            public String getHeader(String title) {
                return headers.get(title);
            }

            public void setMethod(String Method) {
                this.method = Method;
                //System.out.println("Method set="+Method);
            }

            public void setUri(String uri) {
                uri = URLDecoder.decode(uri);
                this.uri = uri;
                // System.out.println("set uri="+uri);
            }

            public String getUri() {
                return this.uri;
            }


            Request(Socket s) {
                this.s = s;
            }

            public void decodeHeaders() throws IOException {
                try {

                    InputStream inputStream = this.s.getInputStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader br = new BufferedReader(inputStreamReader);
                    String header = br.readLine();
                    this.setMethod(header.substring(0, header.indexOf(' ')));
                    this.setUri(header.substring(header.indexOf(' ') + 1, header.lastIndexOf(' ')));
                    int temp = 0;
                    while ((header = br.readLine()).length() != 0) {
                        System.out.println(header);
                        if (header.contains(":")) {
                            temp = header.indexOf(':');
                            this.addHeader(header.substring(0, temp), header.substring(temp + 2));

                        }

                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void handlePost() throws IOException {

                long length=Long.parseLong(this.getHeader("Content-Length"));
                if(length<0)
                    return;
                if (this.getHeader("Content-Type").equals("application/x-www-form-urlencoded")) {
                    System.out.println("data------------");
                }
            }

        }
            public static class Response {
                OutputStream os;
                PrintWriter printWriter;

                HashMap<String, String> headers = new HashMap<>();

                public void addHeader(String title, String value) {
                    headers.put(title, value);
                    System.out.println("Putting:" + title + "=" + value);
                }

                public String getHeader(String title) {
                    return headers.get(title);
                }

                Response(OutputStream os) {
                    this.os = os;
                    this.printWriter = new PrintWriter(os);
                }

                public void respond() {
                    printWriter.print("HTTP/1.1 200 \r\n");
                    printWriter.print("Content-Type: text/html\r\n");
                    printWriter.print("Connection: close\r\n");
                    printWriter.print("\r\n");
                    printWriter.print("<html>Hello world</html>\r\n");
                    printWriter.close();
                }

                public void respondWithText(String s) {
                    printWriter.print("HTTP/1.1 200 \r\n");
                    printWriter.print("Content-Type: text/plain\r\n");
                    printWriter.print("Connection: close\r\n");
                    printWriter.print("\r\n");
                    printWriter.print(s + "\r\n");
                    printWriter.close();
                }

                public void respondWithText(String s, int status) {
                    printWriter.print("HTTP/1.1 " + status + " \r\n");
                    printWriter.print("Content-Type: text/plain\r\n");
                    printWriter.print("Connection: close\r\n");
                    printWriter.print("\r\n");
                    printWriter.print(s + "\r\n");
                    printWriter.close();
                }

                public void respondWithFile(String filePath) {
                    try {
                        File f = new File(filePath);
                        if (f.isFile()) {
                            String contentType = guessContentType(Paths.get(filePath));
                            FileInputStream fileInputStream = new FileInputStream(f);
                            byte[] buffer = new byte[1024 * 1024 * 6];
                            DataInputStream dis = new DataInputStream(new BufferedInputStream(fileInputStream));

                            os.write(("HTTP/1.1 200 \r\n").getBytes());
                            os.write(("Content-Type: " + contentType + "\r\n").getBytes());
                            os.write(("Content-Length: " + f.length() + "\r\n").getBytes());
                            //os.write(("Content-Disposition: attachment; filename=\""+f.getName()+"\"\r\n").getBytes());
                            os.write("\r\n".getBytes());
                            mnotify("Sending file:" + f.getName());
                            long x, written = 0;
                            long total = f.length();
                            while ((x = dis.read(buffer)) > 0) {
                                os.write(buffer);
                                written += x;
                                mnotify("progress:" + (written * 100) / total);
                            }
                            mnotify("transfer completed");
                            os.write("\r\n\r\n".getBytes());
                            os.flush();
                            fileInputStream.close();
                            //client.close();
                        } else if (f.isDirectory()) {
                            File[] list = f.listFiles();
                            os.write(("HTTP/1.1 200 \r\n").getBytes());
                            os.write(("Content-Type: text/html\r\n").getBytes());
                            os.write("\r\n".getBytes());
                            os.write("<html>\r\n".getBytes());
                            for (File s : list
                            ) {
                                String temp;
                                temp = s.getAbsolutePath().replace(root, "");
                                os.write(("<a href='" + temp + "'>" + s.getName() + "</a><br>" + "\r\n").getBytes());

                            }
                            os.write("</html>\r\n".getBytes());
                            os.write("\r\n\r\n".getBytes());
                            os.flush();

                            current = f.getPath();

                        } else {
                            respondWithText("Error 404:Not Found", 404);
                        }
                    } catch (Exception e) {

                        System.out.println("Error " + e.toString());
                    }


                }


            }

        }
    }





