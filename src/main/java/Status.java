public class Status {
    String application;
    String version;
    double requests_count;
    double success_count;
    long error_count;
    int statusCode;
    String message;

    //Gson needs a no args constructor
    public Status(){}
    //usefull for getOrDefault
    public Status(String name, String version){
        this.application = name;
        this.version = version;
        requests_count = 0;
        success_count = 0;
        error_count = 0;
    }


    @Override
    public String toString() {
        //return a string in the form of "WebApp1, 1.0, 60.00"
        return String.format("%s, %s, %.2f", application, version, (success_count/requests_count)*100);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof Status){
            return ((Status) obj).application.equals(this.application) &&
                    ((Status) obj).version.equals(this.version);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return (application + version).hashCode();
    }
}
