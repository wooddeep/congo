import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;

/**
 * Created by lihan on 2018/3/1.
 */



public class AsyncSQLTest {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle("SqlVerticle", new DeploymentOptions().setInstances(4));
    }

}
