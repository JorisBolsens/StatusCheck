# Status Check

## Usage:
to build and install use `gradlew`, run the command `./gradlew build` to compile everything, and then `./gradlew run` to run the main class

## Details:
The app will read the file "server.txt" in the top directory and loop through each URL in the file and perform a `GET` request

it expects a response in the form of
```json
{
  "requests_count": 96447, 
  "application": "WebApp1", 
  "version": "2.0", 
  "success_count": 96427, 
  "error_count": 20
}
```
These responses then get aggregated and printed out by application name +  version in the form of 
```text
WebApp2, 2.0, 92.36
WebApp2, 1.0, 92.46
WebApp1, 1.0, 90.88
WebApp1, 2.0, 87.57
```

## Implementation
The app streams the list of URLS and creates `Callable`s with a get request. 
These are passed to an executor with a number of thread `n` equal to the number of processors available * 2. 
This means that it can perform `n` GET requests concurrently. The responses are the parsed and aggregated into a hashmap based on application and version.
