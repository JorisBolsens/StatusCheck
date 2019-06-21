import com.google.gson.Gson;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args){
        String fileName = "server.txt";

        if(args.length > 1)
            throw new RuntimeException("Can only pass in 1 file to read");
        else if (args.length == 1)
            fileName = args[0];

        //Construct a new HTTPClient
        //Time out after 5 seconds of trying to connect
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        //Setup a hashmap to store stats on a per server/version basis
        Map<String, Status> stats = new HashMap<>();
        //Use Gson to parse response text into pojo
        Gson gson = new Gson();

        File in = new File(fileName);

        //Use try with resource so we don't have to worry about closing the file
        try(BufferedReader bin = new BufferedReader(new FileReader(in))){
            //Use available processors * 2 for number of available threads to call api
            ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*2);

            //submit all lines to exec service and get a Future for result
            List<Future<Status>> futures = exec.invokeAll(bin.lines()
                    .map(URI::create) //Create a URI based on string
                    .map(HttpRequest::newBuilder)  //create a HttpRequest with URI
                    .map(r -> r.timeout(Duration.ofSeconds(5))) //Set connection timeout to 5 secodns
                    .map(HttpRequest.Builder::build)
                    .map(r -> (Callable<Status>) () -> { //return a callable to be passed to exec service
                        try {
                            //Perform the get request and wait on response
                            HttpResponse<String> response = client.send(r, HttpResponse.BodyHandlers.ofString());
                            //if our response is not within the 500 range then we have an error
                            if(response.statusCode() > 299 || response.statusCode() < 200){
                                //err
                                Status status = new Status();
                                status.statusCode = response.statusCode();
                                status.message = response.body();

                                return status;
                            } else
                                return gson.fromJson(response.body(), Status.class);
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }

                        return null;
                    })
                    .collect(Collectors.toList()));

            //Loop through all futures
            for(Future<Status> fut : futures){
                try {
                    Status status = fut.get();

                    if(status == null)
                        continue;

                    if(status.message != null){
                        System.err.println(status.message);
                    } else {
                        //key for hashmap
                        String name = status.application + "," + status.version;

                        //either get the existing agregate status or create one
                        Status agregate = stats.getOrDefault(name, new Status(status.application, status.version));

                        //increment the aggregate counts for this application/version
                        agregate.requests_count += status.requests_count;
                        agregate.error_count += status.error_count;
                        agregate.success_count += status.success_count;

                        //update the map
                        stats.put(name, agregate);
                    }
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }

            //we are done with everything, all tasks have returned, so shutdown the service
            exec.shutdownNow();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        //print out the results
        for(Status status : stats.values()){
            System.out.println(status);
        }
    }
}
