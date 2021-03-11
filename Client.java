import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Client{
   static InputStream inputStream=null;
   static OutputStream outputStream=null;
   static HttpURLConnection httpURLConnection = null;

    public static void main(String[] args)
    {
        downloadFile("http://localhost:2004/y2mate.com%20-%20Lyrical_%20Dil%20Mein%20Ho%20Tum_%20WHY%20CHEAT%20INDIA%20_%20Emraan%20H,%20Shreya%20D_Rochak%20K,%20Armaan%20M,%20Bappi%20L,%20Manoj%20M_360p.mp4","C:\\Users\\kdurg\\OneDrive\\Desktop\\uploads");
        uploadFile("/","C:\\WWW\\htdocs\\WeatherApp\\UI.js");
    }


    public static void downloadFile(String uri,String destDir)
    {
        try{
            URL url=new URL(uri);
            httpURLConnection =(HttpURLConnection) url.openConnection();
            httpURLConnection.connect();
            if(httpURLConnection.getResponseCode()!=HttpURLConnection.HTTP_OK)
                return;
            int TotalLength = httpURLConnection.getContentLength();
            String name=httpURLConnection.getHeaderFields().get("Content-Disposition").get(0);
            String finalNmae =name.substring(name.lastIndexOf('=')+2,name.length()-1);
            System.out.println("Downloading:"+finalNmae+'\t'+TotalLength);
            inputStream=httpURLConnection.getInputStream();
            File dir = new File(destDir,finalNmae);
            outputStream=new FileOutputStream(dir);
            byte[] data =new byte[1024*1024*6];
            long readtotal=0;
            int count;
            //this.statusChangeListener.updateTextview("starting:"+out.getAbsolutePath()+"\n");

            while ((readtotal<TotalLength)&&(count=inputStream.read(data))!=-1)
            {

                readtotal+=count;
                if(TotalLength>0)
                    System.out.println((int)((readtotal*100)/TotalLength));
                System.out.println(readtotal*100/TotalLength);
                outputStream.write(data,0,count);


            }
            System.out.println("Transfer completed");
        } catch (MalformedURLException e) {

            e.printStackTrace();


        } catch (IOException e) {
            //Toast.makeText(this.context, e.toString(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            //Toast.makeText(this.context, e.toString(), Toast.LENGTH_SHORT).show();
            //return e.toString();
            //this.statusChangeListener.updateTextview(e.toString());
        }finally {
            try{
                if(outputStream!=null)
                    outputStream.close();
                if(inputStream!=null)
                    inputStream.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            if(httpURLConnection!=null)
                httpURLConnection.disconnect();
        }

        return;
    }
    private static String guessContentType(Path filePath) throws IOException {
        return Files.probeContentType(filePath);
    }
    public static void uploadFile(String uri,String filePath)
    {
        try{
            Socket s= new Socket("localhost",2004);
            OutputStream os = s.getOutputStream();
            InputStream is = s.getInputStream();
            File f =new File(filePath);
            FileInputStream fileInputStream =new FileInputStream(f);
            if(f.isFile())
            {
                os.write("POST / HTTP/1.1 \r\n".getBytes());
                os.write(("Content-Length: "+f.length()+"\r\n").getBytes());
                os.write(("Content-Type: "+guessContentType(Paths.get(filePath))+"\r\n").getBytes());
                os.write(("Content-Disposition: fileName="+f.getName()+"\r\n").getBytes());
                os.write("\r\n".getBytes());
                System.out.println("Sending file:" + f.getName());
                long x, written = 0;
                long total = f.length();
                DataInputStream dis = new DataInputStream(new BufferedInputStream(fileInputStream));
                byte[] buffer = new byte[1024*1024*6];
                while (written<total&& (x = dis.read(buffer)) > 0) {

                    written += x;
                    os.write(buffer,0, (int) x);
                    System.out.println("progress:" + (written * 100) / total);
                }
                //os.flush();
                os.close();
                dis.close();
                //os.write("\r\n".getBytes());
                System.out.println("transfer completed");

            }
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}