import clojure.java.api.Clojure;
import clojure.lang.IFn;
import java.net.URL;
import java.util.ResourceBundle;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class MyUrlStreamHandlerFactory implements URLStreamHandlerFactory {
  public MyUrlStreamHandlerFactory() {}

  public URLStreamHandler createURLStreamHandler(String protocol) {
    IFn require = Clojure.var("clojure.core", "require");
    require.invoke(Clojure.read("ahubu.lib"));

    IFn omnibar = Clojure.var("ahubu.lib", "my-connection-handler");

    return (URLStreamHandler)omnibar.invoke(protocol);
  }
}
