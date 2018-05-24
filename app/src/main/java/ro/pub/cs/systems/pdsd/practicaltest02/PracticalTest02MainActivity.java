package ro.pub.cs.systems.pdsd.practicaltest02;

import android.app.Activity;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;

public class PracticalTest02MainActivity extends Activity {

    EditText query, ip, port, portServer;
    Button search, start;
    TextView resultTV;

    private ServerThread singleThreadedServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practical_test02_main);

        query = (EditText) findViewById(R.id.word);
        ip = (EditText) findViewById(R.id.ip);
        port = (EditText) findViewById(R.id.port);
        portServer = (EditText) findViewById(R.id.portServer);
        search = (Button) findViewById(R.id.searchButton);
        start = (Button) findViewById(R.id.start);
        resultTV = (TextView) findViewById(R.id.result);

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    new AsyncTask() {

                        @Override
                        protected Object doInBackground(Object[] objects) {

                            try {
                            String hostname = ip.getText().toString();
                            int portNo = Integer.parseInt(port.getText().toString());
                            Socket socket = new Socket(hostname, portNo);

                            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(socket.getOutputStream());
                            PrintWriter printWriter = new PrintWriter(bufferedOutputStream, true);
                            printWriter.println(query.getText().toString());

                            InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
                            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                            final String response = bufferedReader.readLine();

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    resultTV.setText(response);
                                }
                            });

                            socket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Object o) {
                            super.onPostExecute(o);
                        }
                    }.execute(null, null, null);


            }
        });

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                singleThreadedServer = new ServerThread();
                singleThreadedServer.startServer();
            }
        });
    }




    public static BufferedReader getReader(Socket socket) throws IOException {
        return new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    public static PrintWriter getWriter(Socket socket) throws IOException {
        return new PrintWriter(socket.getOutputStream(), true);
    }


    private class ServerThread extends Thread {

        private boolean isRunning;

        private ServerSocket serverSocket;

        public void startServer() {
            isRunning = true;
            start();
        }

        public void stopServer() {
            isRunning = false;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (serverSocket != null) {
                            serverSocket.close();
                        }
                        Log.v("", "stopServer() method invoked "+serverSocket);
                    } catch(IOException ioException) {
                        Log.e("", "An exception has occurred: "+ioException.getMessage());

                    }
                }
            }).start();
        }


        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(Integer.parseInt(portServer.getText().toString()));
                while (isRunning) {
                    Socket socket = serverSocket.accept();
                    Log.v("", "Connection opened with "+socket.getInetAddress()+":"+socket.getLocalPort());

                    BufferedReader bufferedReader =  getReader(socket);
                    String response = bufferedReader.readLine();

                    //make server call
                    HttpURLConnection httpURLConnection = null;
                    StringBuilder result = new StringBuilder();
                    String error = null;
                    try {
                        String webPageAddress = "http://services.aonaware.com/DictService/DictService.asmx/Define?word=";
                        String keyword  = response;

                        URL url = new URL(webPageAddress+keyword);
                        URLConnection urlConnection = url.openConnection();
                        if (urlConnection instanceof HttpURLConnection) {
                            httpURLConnection = (HttpURLConnection)urlConnection;
                            BufferedReader bufferedReader2 = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                            int currentLineNumber = 0, numberOfOccurrencies = 0;
                            String currentLineContent;
                            while ((currentLineContent = bufferedReader2.readLine()) != null) {
                                currentLineNumber++;
                                result.append(currentLineContent);
                            }
                            result.append("Number of occurrencies: " + numberOfOccurrencies+"\n");
                            String finalRes = result.toString().split("<WordDefinition>")[1];
                            Log.e("RESULT", finalRes);

                            //return response
                            PrintWriter printWriter = getWriter(socket);
                            printWriter.println(finalRes);
                            socket.close();
                        }
                    } catch (MalformedURLException malformedURLException) {

                    } catch (IOException ioException) {

                    } finally {
                        if (httpURLConnection != null) {
                            httpURLConnection.disconnect();
                        }
                    }

                    Log.v("", "Connection closed");
                }
            } catch (IOException ioException) {
                Log.e("", "An exception has occurred: "+ioException.getMessage());

            }
        }
    }
}
