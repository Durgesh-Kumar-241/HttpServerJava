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
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Scanner;

public class Server {
    public static void main(String[] args) throws IOException {   int port=2004;
        Scanner scanner =  new Scanner(System.in);
        System.out.println("Waiting on port :"+port+"\n");
        ServerSocket server= new ServerSocket(port);
        while(true) {
            Socket ss = server.accept();
            InputStream is = ss.getInputStream();
            OutputStream os = ss.getOutputStream();

            //OutputStreamWriter outputStreamWriter = new OutputStreamWriter(os);
            // BufferedWriter bw = new BufferedWriter(outputStreamWriter);

            Request r = new Request(is);
            r.decodeHeaders();
           // new TextResponse(os).respondWithText("Hello World");
          // new Response(os).respondWithFile("C:/Users/kdurg/Videos/Bollywood Songs/AASHIQUI 2 MASHUP FULL SONG - KIRAN KAMATH - BEST BOLLYWOOD MASHUPS_v720P.mp4");
           //new Response(os).respondWithFile("C:\\Users\\kdurg\\OneDrive\\Desktop\\WeatherApp");
           // if(r.getUri().equalsIgnoreCase("\\index.html")||r.getUri().equalsIgnoreCase("\\"))
             //  new Response(os).respondWithText(r.getUri());
            new Response(os).respondWithFile(r.getUri());
            System.out.println(r.getUri());
            is.close();
            os.close();
            System.out.println(r.getUri());
        }
            //return new TextResponse("<html>Hello World</html>");
        //ss.close();
    }

    private static String guessContentType(Path filePath) throws IOException {
        return Files.probeContentType(filePath);
    }
    public static class Request{
        HashMap<String,String> headers = new HashMap<>();
        String method="GET";
        String uri="/";
        InputStream inputStream;
        InputStreamReader inputStreamReader;
        BufferedReader br;
        public void addHeader(String title,String value)
        {
            headers.put(title,value);
            System.out.println("Putting:"+title+"="+value);
        }
        public String getHeader(String title)
        {
            return headers.get(title);
        }
        public void setMethod(String Method)
        {
            this.method=Method;
            System.out.println("Method set="+Method);
        }
        public void setUri(String uri)
        {   uri = URLDecoder.decode(uri);
            this.uri=uri;
            System.out.println("set uri="+uri);
        }
        public String getUri()
        {
            return this.uri;
        }
        Request(InputStream in)
        {   this.inputStream=in;
            InputStreamReader inputStreamReader = new InputStreamReader(this.inputStream);
            br = new BufferedReader(inputStreamReader);
        }
        public void decodeHeaders() throws IOException {
            String header = br.readLine();
            this.setMethod(header.substring(0, header.indexOf(' ')));
            this.setUri(header.substring(header.indexOf(' ') + 1, header.lastIndexOf(' ')));
            int temp = 0;
            while ((header = br.readLine()).length() != 0) {
                //System.out.println(header);
                if (header.contains(":")) {
                    temp = header.indexOf(':');
                    this.addHeader(header.substring(0, temp), header.substring(temp + 2));

                }

            }

        }
    }

    public static class Response{
        OutputStream os ;
        PrintWriter printWriter;

        HashMap<String,String> headers = new HashMap<>();
        public void addHeader(String title,String value)
        {
            headers.put(title,value);
            System.out.println("Putting:"+title+"="+value);
        }
        public String getHeader(String title)
        {
            return headers.get(title);
        }
        Response(OutputStream os)
        {   this.os=os;
        this.printWriter = new PrintWriter(os);
        }
        public void respond()
        {
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
            printWriter.print(s+"\r\n");
            printWriter.close();
        }

        public void respondWithFile(String filePath)
        {   try {
            File f = new File(filePath);
            if(f.isFile()) {
                String contentType = guessContentType(Paths.get(filePath));
                FileInputStream fileInputStream = new FileInputStream(f);
                byte[] buffer = new byte[2048];
                DataInputStream dis = new DataInputStream(new BufferedInputStream(fileInputStream));
                DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(os));

                os.write(("HTTP/1.1 200 \r\n").getBytes());
                os.write(("Content-Type: " + contentType + "\r\n").getBytes());
                os.write(("Content-Length: " + f.length() + "\r\n").getBytes());
                os.write("\r\n".getBytes());
                int x;
                while ((x = dis.read(buffer)) > 0)
                    os.write(buffer);
                os.write("\r\n\r\n".getBytes());
                os.flush();
                //client.close();
            }else if(f.isDirectory())
            {   File[] list = f.listFiles();
                os.write(("HTTP/1.1 200 \r\n").getBytes());
                os.write(("Content-Type: text/html\r\n").getBytes());
                os.write("\r\n".getBytes());
                os.write("<html>\r\n".getBytes());
                for (File s:list
                     ) {
                    os.write(("<a href='\\"+s.getAbsolutePath()+"'>"+s.getName()+"</a><br>"+"\r\n").getBytes());

                } os.write("</html>\r\n".getBytes());
                os.write("\r\n\r\n".getBytes());
                os.flush();
            }
        }catch (Exception e){
            respondWithText("File error"+filePath+"Not found");
        }


        }

    }






}
