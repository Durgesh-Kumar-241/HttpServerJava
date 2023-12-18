package com.dktechhub.mnnit.myapplication;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.Map;

public class Server2 {
    private static class Task{
        String name="";
        Long totalSize=0L,currSize=0L;
        String taskId="";

        public void setId(String id) {
            taskId=id;
        }

        public void setSize(long size) {
            totalSize=size;
        }

        public void setFileName(String name) {
            this.name=name;
        }
    }
    private static class TaskGroup{
        String groupId="";
        Long grSize=0L;
        HashMap<String, Task> tasks=new HashMap<String, com.dktechhub.mnnit.myapplication.Server2.Task>();

        public void setGid(String s) {
            groupId=s;
        }
    }

    private static HashMap<String, TaskGroup> taskGroupHolder=new HashMap<String, com.dktechhub.mnnit.myapplication.Server2.TaskGroup>();

    public static void main(String[] args) throws IOException {
        int port = 2004;
        System.out.println("Waiting on port :" + port + "\n");
        ServerSocket server = new ServerSocket(port);
        while (true) {
            new SingleClientHandler(server.accept(), System.out::println).start();
        }

        //server.close();
    }

    public static class SingleClientHandler extends Thread {
        Socket socket;
        Notify notify;
        InputStream inputStream;
        OutputStream outputStream;

        BufferedInputStream socketBfis;
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
            this.socketBfis = new BufferedInputStream(socket.getInputStream());

            this.printWriter=new PrintWriter(outputStream);
        }

        @Override
        public void run() {
            super.run();
            // this.notify.notifyText("New connection arrived");
            current = root;
            try {
                String s= this.readHeaders();
                //out("all:"+s);
                this.parseHeaders(s);

                if(this.requestMethod.equals("GET"))
                    handleGet();
                else{ //post
                    out(requestMethod);
                    out(requestUri);
                    if(requestUri.equals("/list_info"))
                    {
                        //readList();
                        //receiveTaskList();
                    }else {
                        handlePost();

                    }
                }

                respond("Ok",200);
                this.finish();
                //System.out.println(this.getString());
                //     System.out.println("Connection closed");

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        public void handleGet()
        {   if(requestUri.equals("/"))
            requestUri="/index.html";
            else if(requestUri.contains("/common/"))
        {
            requestUri=requestUri.substring(7);
            requestUri=requestUri+".png";
        }

            respondWithFile("resources"+requestUri);
        }

        public void handlePost()
        {
            System.out.println(requestheaders.toString());
            String ct = requestheaders.get("Content-Type");
            if(!ct.isEmpty()&&ct.contains("multipart/form-data; boundary=")) {
                //System.out.println(ct);
                String boundry = ct.substring(ct.indexOf("boundary=")+9);
                String ctln = requestheaders.get("item_size");
                System.out.println(boundry+"\n clen:"+ctln);
                //String header="";
                //String destFile="uploads/fff.xyz";
                try{
                    int bflen=1024*1024*2;
                    byte[] buffer = new byte[bflen];
                    int read = this.socketBfis.read(buffer);
                    if (read == -1 || read == 0) {
                        out("nothing to read");
                        return;
                    }

                    int curr=0,data_start=0;
                    System.out.println("start search for headers of formdata, read bytes:"+read);
                    while(true)
                    {
                        if(curr+3>=8191)
                        {
                            out("header length exceeded");
                            curr=0;
                            break;
                        }
                        else if(buffer[curr]=='\r'&&buffer[curr+1]=='\n'&&buffer[curr+2]=='\r'&&buffer[curr+3]=='\n')
                        {
                            out("found end of headers"+curr);
                            data_start=curr+4;
                            break;
                        }else {
                            //out("searching curr:"+curr);
                            curr++;
                        }
                    }
                    String destFileName="unknown";
                    if(data_start==0||curr==0)
                    {
                        out("formate error");
                        return;
                    }else {
                        out("processing form data");
                        byte[] bArr2 = new byte[curr];
                        System.arraycopy(buffer, 0, bArr2, 0, curr);
                        String formdataProps= new String(bArr2);
                        out(formdataProps);
                        String[] all = formdataProps.split("\r\n");
                        for(String s: all)
                        {
                            if(s.contains("Content-Disposition: form-data;"))
                            {
                                destFileName=s.substring(s.indexOf("filename=")+9);
                                if(destFileName.length()>2)
                                    destFileName=destFileName.substring(1,destFileName.length()-1);
                                out("dest file:"+destFileName);
                            }
                        }

                    }

                    System.out.println("start rec file");
                    Long readtotal=0L;
                    Long maxToRead = Long.parseLong(ctln);
                    FileOutputStream outputStream=new FileOutputStream(new File("uploads/"+destFileName));
                    int usable=read-data_start;
                    int lastChars = ("\r\n--" + boundry + "--\r\n").getBytes().length;
                    if((maxToRead-readtotal)<usable)
                    {

                        usable-=lastChars;
                    }
                    outputStream.write(buffer,data_start,usable);
                    readtotal+=usable;
                    out("info:read="+read+" filestartfrom="+data_start+" offset in arr1:"+(data_start-1)+"written:"+(usable));

                    if(readtotal<maxToRead)
                    {   out("rec not completed yet");
                        //Instant i1 = Instant.now();
                        while ((readtotal<maxToRead)&&((read=socketBfis.read(buffer))>0))
                        {   out("read:"+read+" rem:"+(maxToRead-readtotal));
                            if((maxToRead-readtotal)<read)
                            {
                                //
                                out("received last buffer, usable data length="+(read-lastChars));
                                read-=lastChars;

                            }

                            readtotal+=read;
                            outputStream.write(buffer,0,read);
                            //Instant i2 = Instant.now();
                            //int t = Duration.between(i1,i2);

                            //int sp = buffer.length/(t*1000);
                            System.out.println("progress:" + (readtotal)+"/"+ maxToRead);
                            //i1=i2;
                            //System.out.println(count);

                        }

                    }
                    System.out.println("progress:" + (readtotal)+"/"+ maxToRead);
                    outputStream.close();
                    System.out.println("complete");

                }catch (Exception e)
                {
                    e.printStackTrace();
                }


            }
            respond("OK",200);
        }

//        void receiveTaskList()
//        {
//            byte[] bArr = new byte[1024];
//            StringBuffer stringBuffer = new StringBuffer();
//            try {
//                do {
//                    int read = this.socketBfis.read(bArr);
//                    if (read != -1) {
//                        stringBuffer.append(new String(bArr, 0, read));
//                        //  Log.i("jfowjfoefsf", stringBuffer.toString());
//                    }
//                    else break;
//                } while (!stringBuffer.toString().endsWith("_end_"));
//
//                String jsonStr = stringBuffer.toString();
//                JSONArray jSONArray = new JSONArray(jsonStr.substring(0, jsonStr.length() - 5));
//                out("tasks received "+jSONArray.toString());
//                TaskGroup taskGroup = new TaskGroup();
//                taskGroup.setGid("group_" + System.currentTimeMillis());
//                ///C12460b.m5625e().m5629a(taskGroup);
//                //TaskGroupHolder.addTaskGroup(taskGroup);
//
//
//                for (int i = 0; i < jSONArray.length(); i++) {
//                    JSONObject jSONObject = new JSONObject(jSONArray.get(i).toString());
//                    Task task = new Task();
//                    //task.setType(2);
//                    //task.m5665Q(taskGroup.m5615d());
//                    task.setId(jSONObject.getString("id"));
//                    task.setFileName(jSONObject.getString("name"));
//                    task.setSize(jSONObject.getLong("size"));
//                    taskGroup.grSize+=task.totalSize;
//                    taskGroup.tasks.put(task.taskId,task);
//                }
//                // C12465g.m5587c(taskGroup.m5615d());
//                if(taskGroup.tasks.size()>0) {
//                    taskGroupHolder.put(taskGroup.groupId, taskGroup);
//                    out("received list:"+taskGroup);
//                }
//                return;
//            } catch (JSONException | IOException e) {
//                e.printStackTrace();
//                return;
//            }
//
//        }

        public Task findTask(String id)
        {

//            for(Map<String,TaskGroup>.Entry tgEntry: taskGroupHolder.entrySet()){
//                if(tgEntry.getValue().tasks.containsKey(id))
//                {
//                    return tgEntry.getValue().tasks.get(id);
//                }
//            }
            return null;
        }


        public void parseHeaders(String str) throws IOException {
            if(str==null||str.isEmpty())
            {
                out("empty");
                return;
            }
            String[] h=str.split("\r\n");

            this.requestMethod=(h[0].substring(0, h[0].indexOf(' ')));
            this.requestUri= URLDecoder.decode(h[0].substring(h[0].indexOf(' ') + 1, h[0].lastIndexOf(' ')),"utf-8");
            int temp;
            for(int i=1;i<h.length;i++)
            {
                if (h[i].contains(":")) {
                    temp = h[i].indexOf(':');
                    this.requestheaders.put(h[i].substring(0, temp), h[i].substring(temp + 2));
                }
            }

        }
        public String readHeaders()throws IOException
        {
            byte[] bArr = new byte[8192];
            this.socketBfis.mark(8192);
            int read = this.socketBfis.read(bArr);
            if (read == -1 || read == 0) {
                return null;
            }
            int curr=0,headerEnd=0;
            //System.out.println("start search, read bytes:"+read);
            while(true)
            {
                if(curr+3>8191)
                {
                    //out("reached end of buffer");
                    curr=0;
                    break;
                }
                else if(bArr[curr]=='\r'&&bArr[curr+1]=='\n'&&bArr[curr+2]=='\r'&&bArr[curr+3]=='\n')
                {
                    //out("found end of headers"+curr);
                    headerEnd=curr+4;
                    break;
                }else {
                    curr++;
                }
            }

            if(headerEnd<=read)
            {
                this.socketBfis.reset();
                this.socketBfis.skip(headerEnd);
                //out("skipped till:"+headerEnd);
            }

            byte[] bArr2 = new byte[curr];
            System.arraycopy(bArr, 0, bArr2, 0, curr);
            return new String(bArr2);


        }

        void out(String s)
        {
            System.out.println(s);
        }


        // public void receiveFile() throws IOException {
        //     String uploadPath="C:\\Users\\kdurg\\OneDrive\\Desktop\\uploads";
        //     String type = this.requestheaders.get("Content-Type");
        //     long length = Long.parseLong(this.requestheaders.get("Content-Length"));
        //     if (length < 0)
        //         return;
        //     String name = this.requestheaders.get("Content-Disposition");
        //     name = name.replace("fileName=","");
        //     System.out.println("receiving file:"+name+'\t'+"Length:"+length+'\t'+"Type:"+type);
        //     FileOutputStream outputStream=new FileOutputStream(new File(uploadPath,name));
        //     int count,readtotal=0;
        //     byte[] buffer = new byte[1024*1024*6];
        //     while ((count=inputStream.read(buffer))>0)
        //     {
        //         readtotal+=count;
        //         this.notify.notifyText("progress:" + (readtotal * 100) / length);
        //         //System.out.println(count);
        //         outputStream.write(buffer,0,count);
        //     }


        //     System.out.println("Received File");
        //     //inputStream.close();
        //     outputStream.close();
        // }

        public void respond(String message,int statusCode)
        {
            printWriter.print("HTTP/1.1 " + statusCode + " \r\n");
            printWriter.print("Content-Type: text/plain\r\n");
            printWriter.print("Connection: close\r\n");
            printWriter.print("\r\n");

            printWriter.close();
        }
        public void respondWithFile(String filePath) {
            try {
                //System.out.println("file: "+filePath);
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

            printWriter.close();
            inputStream.close();
            outputStream.close();
            socket.close();
        }
        public static String getPathForRequestedUri(String path) {

            return "resources/index.html";
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
