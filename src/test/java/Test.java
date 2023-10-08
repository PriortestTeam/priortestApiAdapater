import io.restassured.RestAssured;
import org.testng.annotations.BeforeMethod;

import static com.priortest.run.api.PTApiRequest.doLogin;
import static com.priortest.run.api.PTApiRequest.doLogout;

public class Test {


    void setupUPI(){
        RestAssured.baseURI ="http://43.139.159.146:8082/api/";
    }

    public void testPTApi() {
       // doLogin();
        doLogout();
    }

}
