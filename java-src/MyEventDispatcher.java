import clojure.lang.IFn;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventDispatcher;
import javafx.scene.Node;
import javafx.scene.Scene;

public class MyEventDispatcher implements EventDispatcher {

  private EventDispatcher dispatcher;
  private IFn f;

  public MyEventDispatcher(IFn f, EventDispatcher d) {
    this.dispatcher = d;
    this.f = f;
  }

  @Override
  public Event dispatchEvent(Event arg0, EventDispatchChain arg1) {
    // TODO Auto-generated method stub
    f.invoke(arg0, arg1);
    return dispatcher.dispatchEvent(arg0, arg1);
  }

  public static void replace(IFn f, Node n) {
    EventDispatcher d = n.getEventDispatcher();
    if (d instanceof MyEventDispatcher) {
      MyEventDispatcher d2 = (MyEventDispatcher) d;
      MyEventDispatcher d3 = new MyEventDispatcher(f, d2.dispatcher);
      n.setEventDispatcher(d3);
    } else {
      MyEventDispatcher d2 = new MyEventDispatcher(f, n.getEventDispatcher());
      n.setEventDispatcher(d2);
    }
  }

  public static void replace(IFn f, Scene n) {
    EventDispatcher d = n.getEventDispatcher();
    if (d instanceof MyEventDispatcher) {
      MyEventDispatcher d2 = (MyEventDispatcher) d;
      MyEventDispatcher d3 = new MyEventDispatcher(f, d2.dispatcher);
      n.setEventDispatcher(d3);
    } else {
      MyEventDispatcher d2 = new MyEventDispatcher(f, n.getEventDispatcher());
      n.setEventDispatcher(d2);
    }
  }

}
